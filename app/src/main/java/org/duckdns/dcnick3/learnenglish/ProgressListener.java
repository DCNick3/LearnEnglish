package org.duckdns.dcnick3.learnenglish;

/**
 * Created by nikit_000 on 5/5/2018.
 */

public interface ProgressListener {
    void onProgress(int done, int total);
    void finished(int total, long time);
}
