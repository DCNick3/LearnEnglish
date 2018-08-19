package org.duckdns.dcnick3.learnenglish;

/**
 * Created by nikit_000 on 5/6/2018.
 */

public class CancellationTokenSource {

    public  CancellationTokenSource() {
        cancelled = false;
        token = new CancellationToken(this);
    }

    private boolean cancelled;
    private CancellationToken token;

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
    }

    public CancellationToken getToken() {
        return token;
    }
}
