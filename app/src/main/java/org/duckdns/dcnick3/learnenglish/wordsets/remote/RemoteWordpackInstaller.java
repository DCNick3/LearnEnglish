package org.duckdns.dcnick3.learnenglish.wordsets.remote;

import android.graphics.Paint;
import android.util.Pair;

import org.duckdns.dcnick3.learnenglish.CancellationToken;
import org.duckdns.dcnick3.learnenglish.CancellationTokenSource;
import org.duckdns.dcnick3.learnenglish.ProgressListener;
import org.duckdns.dcnick3.learnenglish.wordsets.FaultyProgressListener;

public class RemoteWordpackInstaller implements ProgressListener {
    RemoteWordpackInstaller(RemoteWordpack wordpack) {
        this.wordpack = wordpack;
    }

    private static final long NOTIFY_INTERVAL = 500;

    private RemoteWordpack wordpack;
    private FaultyProgressListener progressListener = null;
    private long startTime = System.currentTimeMillis();
    private int total = 0;
    private int current = 0;
    private CancellationTokenSource tok = new CancellationTokenSource();
    private boolean finished = false;
    private long lastNotify = 0;


    /* Public API */

    public void setProgressListener(FaultyProgressListener listener) {
        progressListener = listener;
    }

    public void cancel() {
        tok.cancel();
    }

    public RemoteWordpack getWordpack() {
        return wordpack;
    }

    public Pair<Integer, Integer> getProgress() {
        return new Pair<>(current, total);
    }

    /* this is not intended to be public
     * thank Java for not having protected interface implementations
     * */

    @Override
    public void onProgress(int done, int total) {
        this.total = total;
        current = done;
        if (progressListener != null) {
            long time = System.currentTimeMillis();
            if (time - lastNotify >= NOTIFY_INTERVAL) {
                lastNotify = time;
                progressListener.onProgress(done, total);
            }
        }
    }

    @Override
    public void finished(int total, long time) {
        this.total = total;
        current = total;
        progressListener.onProgress(total, total);
    }

    void reallyFinished() {
        finished = true;
        if (progressListener != null)
            progressListener.finished(total, System.currentTimeMillis() - startTime);
    }

    void fault() {
        finished = true;
        if (progressListener != null)
            progressListener.faulted(this);
    }

    CancellationToken getCancellationToken() {
        return tok.getToken();
    }

    boolean isFinished() {
        return finished;
    }
}
