package org.duckdns.dcnick3.learnenglish.layout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.SharedRes;
import org.duckdns.dcnick3.learnenglish.wordsets.FaultyProgressListener;
import org.duckdns.dcnick3.learnenglish.wordsets.remote.RemoteWordpack;
import org.duckdns.dcnick3.learnenglish.wordsets.remote.RemoteWordpackInstaller;
import org.duckdns.dcnick3.learnenglish.wordsets.remote.RemoteWordpackManager;

import java.util.ArrayList;

import bolts.Continuation;
import bolts.Task;

public class RemoteWordsetsActivity extends Activity {

    RemoteWordpackManager rwman;
    ArrayList<RemoteWordpackInstaller> activeInstallations = new ArrayList<>();
    RemoteWordsetListAdapter adapter;
    RemoteWordpack[] wordpacks;
    ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_wordsets);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);

        rwman = SharedRes.openWordpackManager();

        list = findViewById(R.id.wordset_list);
        registerForContextMenu(list);
        updateWordpacks();



        if (wordpacks.length == 0) {
            rwman.tryAddRepositoryAsync("https://dcnick3.duckdns.org/learnenglish/repo").continueWith(new Continuation<RemoteWordpackManager.InstallResult, Void>() {
                @Override
                public Void then(Task<RemoteWordpackManager.InstallResult> task) throws Exception {
                    if (task.isFaulted() || task.getResult() != RemoteWordpackManager.InstallResult.OK) {
                        if (task.isFaulted()) {
                            Log.e("RemotwWordsetsActivity", "Initial repository installation task faulted: ", task.getError());
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RemoteWordsetsActivity.this, R.string.initial_repo_install_failed, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recreate();
                            }
                        });
                    }
                    return null;
                }
            });
        }

        for (RemoteWordpackInstaller installer : rwman.getRunningInstallations()) {
            activeInstallations.add(wrapInstallation(installer));
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();

        RemoteWordpack pack = (RemoteWordpack) adapter.getItem(
                ((AdapterView.AdapterContextMenuInfo)menuInfo).position);

        if (pack.installed)
            inflater.inflate(R.menu.installed_remote_wordset, menu);
        else
            inflater.inflate(R.menu.not_installed_remote_wordset, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final RemoteWordpack pack = (RemoteWordpack) adapter.getItem(
                ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.menu_remove_wordset:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.are_you_sure);
                builder.setMessage(R.string.uninstall_warning);
                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        rwman.removeWordpack(pack);
                        updateWordpacks();
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
                return true;

            case R.id.menu_install_wordset:
                installWordpack(pack);
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return true;
    }

    private void updateWordpacks() {
        wordpacks = rwman.getWordpacks();
        adapter = new RemoteWordsetListAdapter(this, SharedRes.openIconManager(),
                wordpacks);
        list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.remote_wordset_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_browse_repos:
                Intent i = new Intent(this, RemoteRepositoriesActivity.class);
                startActivity(i);
                return true;
            default:
                return false;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWordpacks();
    }

    private RemoteWordpackInstaller wrapInstallation(final RemoteWordpackInstaller installer) {
        installer.setProgressListener(new FaultyProgressListener() {
            private void notifyDataSetChanged() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void faulted(Object sender) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RemoteWordsetsActivity.this, "Wordset install failed",
                                Toast.LENGTH_LONG).show();
                    }
                });
                activeInstallations.remove(installer);
                notifyDataSetChanged();
            }

            @Override
            public void onProgress(int done, int total) {
                activeInstallations.remove(installer);
                notifyDataSetChanged();
            }

            @Override
            public void finished(int total, long time) {
                notifyDataSetChanged();
            }
        });
        return installer;
    }

    public void installWordpack(RemoteWordpack item) {
        activeInstallations.add(wrapInstallation(rwman.installWordpackAsync(item)));
    }

    public void cancelInstallation(RemoteWordpack item) {
        RemoteWordpackInstaller installer = item.installer;
        if (installer != null)
            installer.cancel();
    }
}
