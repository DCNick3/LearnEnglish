package org.duckdns.dcnick3.learnenglish.wordsets.remote;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.duckdns.dcnick3.learnenglish.HttpUtil;
import org.duckdns.dcnick3.learnenglish.ProgressListener;
import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.wordsets.IconManager;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSet;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSetDatabase;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSetPack;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import bolts.Continuation;
import bolts.Task;

public class RemoteWordpackManager {
    public RemoteWordpackManager(File tempPath, WordSetDatabase database, IconManager iman) {
        if (!tempPath.exists())
            if (!tempPath.mkdir())
                throw new RuntimeException();

        this.tempPath = tempPath;
        this.database = database;
        this.iman = iman;

        repos = new ArrayList<>();
        for (RemoteRepository p : database.getRemoteRepositories())
            repos.add(updateRepositoryInfo(p));



        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        lock = new ReentrantLock();
        runningInstallations = new HashSet<>();
    }

    private static final String REPO_INDEX_PATH = "index.json";
    private static final String REPO_ICONS_PATH = "icons/";
    static final String REPO_WORDPACKS_PATH = "wordpacks/";

    private File tempPath;
    private ArrayList<RemoteRepository> repos;
    private WordSetDatabase database;
    private IconManager iman;
    private Gson gson;
    private ReentrantLock lock;
    private Set<RemoteWordpackInstaller> runningInstallations;

    public RemoteRepository[] getRepos() {
        lock.lock();
        try {
            return repos.toArray(new RemoteRepository[repos.size()]);
        } finally {
            lock.unlock();
        }
    }

    public RemoteWordpack[] getWordpacks() {
        ArrayList<RemoteWordpack> packs = new ArrayList<>();
        for (RemoteRepository repo : repos) {
            updateRepositoryInfo(repo);
            packs.addAll(Arrays.asList(repo.wordpacks));
        }
        return packs.toArray(new RemoteWordpack[packs.size()]);
    }

    public RemoteWordpackInstaller[] getRunningInstallations() {
        return runningInstallations.toArray(new RemoteWordpackInstaller[0]);
    }

    private RemoteRepository updateRepositoryInfo(RemoteRepository repo) {
        for (RemoteWordpack p : repo.wordpacks) {
            p.repository = repo;
            p.installed = database.isWordsetInstalled(p.getGlobalId());
        }
        return repo;
    }

    private Task<Void> fetchIconAsync(RemoteRepository repo, String hash) {
        try {
            return HttpUtil.getStream(repo.url.toURI().resolve(REPO_ICONS_PATH).resolve(hash + ".png").toURL())
                .continueWith(new Continuation<InputStream, Void>() {
                @Override
                public Void then(Task<InputStream> task) throws Exception {
                    InputStream inp = task.getResult();
                    iman.addIcon(inp);
                    inp.close();
                    return null;
                }
            });
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Task<File> downloadWordpackAsync(RemoteWordpack remoteWordpack,  RemoteWordpackInstaller installer) {
        final File tmpFile = new File(tempPath, remoteWordpack.getGlobalId() + ".zip");
        return HttpUtil.getFile(remoteWordpack.getUrl(), tmpFile, installer.getCancellationToken(), installer).continueWithTask(new Continuation<Boolean, Task<File>>() {
            @Override
            public Task<File> then(Task<Boolean> task) throws Exception {
                if (task.isFaulted())
                    return Task.forError(task.getError());
                else if (!task.getResult())
                    return Task.forResult(null);
                else
                    return Task.forResult(tmpFile);
            }
        });
    }

    public RemoteWordpackInstaller installWordpackAsync(final RemoteWordpack wordpack) {
        final RemoteWordpackInstaller inst = new RemoteWordpackInstaller(wordpack);
        runningInstallations.add(inst);
        wordpack.installer = inst;
        downloadWordpackAsync(wordpack, inst).continueWith(new Continuation<File, Void>() {
            @Override
            public Void then(Task<File> task) throws Exception {
                File path = task.getResult();
                boolean res = false;
                if (task.isFaulted()) {
                    Log.e("rwman", "Install problem: ", task.getError());
                    res = false;
                }
                else if (path == null) {
                    res = true;
                } else {
                        new WordSetPack(path).readInto(new ProgressListener() {
                                                           @Override
                                                           public void onProgress(int done, int total) {

                                                           }

                                                           @Override
                                                           public void finished(int total, long time) {

                                                           }
                                                       }, database, iman,
                                inst.getCancellationToken(), wordpack.repository.getGlobalId());
                        path.delete();
                        res = true;
                }
                wordpack.installer = null;
                runningInstallations.remove(inst);
                updateRepositoryInfo(wordpack.repository);

                if (res)
                    inst.reallyFinished();
                else
                    inst.fault();

                return null;
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                if (task.isFaulted()) {
                    Log.e("rwman", "Install problem: ", task.getError());
                    wordpack.installer = null;
                    runningInstallations.remove(inst);
                    updateRepositoryInfo(wordpack.repository);
                    if (!inst.isFinished())
                        inst.fault();
                }
                return null;
            }
        });
        return inst;
    }

    public Task<InstallResult> tryAddRepositoryAsync(final String url) {
        Log.i("rwman", "Trying to add " + url + " to repos");

        final AtomicReference<URL> urlReference = new AtomicReference<>();
        URL repoUrl;
        try {
            repoUrl = new URL(url);
        } catch (MalformedURLException e) {
            return Task.forResult(InstallResult.INVALID_URL);
        }

        return HttpUtil.resolveRedirect(repoUrl
        ).continueWithTask(new Continuation<URL, Task<String>>() {
            @Override
            public Task<String> then(Task<URL> task) throws Exception {
                urlReference.set(task.getResult());
                Log.i("rwman", "Resolved redirects; final url is " + urlReference.get());
                return HttpUtil.get(urlReference.get().toURI().resolve(REPO_INDEX_PATH).toURL());
            }
        }).continueWithTask(new Continuation<String, Task<InstallResult>>() {
            @Override
            public Task<InstallResult> then(Task<String> task) throws Exception {
                Log.i("rwman", "Read index");
                final RemoteRepository grepo;
                try {
                    URL url = urlReference.get();
                    RemoteRepository repo = gson.fromJson(task.getResult(), RemoteRepository.class);
                    if (!repo.validate()) {
                        Log.i("rwman", "invalid index");
                        return Task.forResult(InstallResult.INVALID_REPO);
                    }
                    repo.url = url;
                    grepo = updateRepositoryInfo(repo);
                } catch (JsonParseException e) {
                    Log.i("rwman", "invalid index");
                    return Task.forResult(InstallResult.INVALID_REPO);
                }
                if (database.isRepositoryInstalled(urlReference.get().toString())) {
                    return Task.forResult(InstallResult.ALREADY_INSTALLED);
                }
                lock.lock();
                try {
                    database.insertRemoteRepository(grepo);
                    repos.add(grepo);
                } finally {
                    lock.unlock();
                }
                Log.i("rwman", "inserted to DB, downloading icons");

                ArrayList<Task<Void>> tasks = new ArrayList<>();
                for (RemoteWordpack pack : grepo.wordpacks) {
                    tasks.add(fetchIconAsync(grepo, pack.iconHash));
                }
                Task.whenAll(tasks).waitForCompletion(5, TimeUnit.SECONDS);
                return Task.forResult(InstallResult.OK);
            }
        });
    }

    public void removeRepository(RemoteRepository repo) {
        lock.lock();
        try {
            repos.remove(repo);
            database.deleteRemoteRepository(repo.id);
        } finally {
            lock.unlock();
        }
    }

    public void removeWordpack(RemoteWordpack pack) {
        lock.lock();
        try {
            WordSet ws = database.getWordsetByName(pack.getGlobalId());
            database.deleteWordset(ws.id);
            updateRepositoryInfo(pack.repository);
        } finally {
            lock.unlock();
        }
    }

    public enum InstallResult {
        OK,
        INVALID_REPO,
        INVALID_URL,
        ALREADY_INSTALLED;

        public String toString(Context context) {
            switch (this) {
                case OK:
                    return context.getString(R.string.OK);
                case INVALID_REPO:
                    return context.getString(R.string.invalid_repository);
                case ALREADY_INSTALLED:
                    return context.getString(R.string.repository_already_installed);
                case INVALID_URL:
                    return context.getString(R.string.invalid_url);
                default:
                    return context.getString(R.string.unknown_error);
            }
        }
    }
}
