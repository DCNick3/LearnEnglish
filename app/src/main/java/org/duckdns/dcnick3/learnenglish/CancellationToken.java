package org.duckdns.dcnick3.learnenglish;

/**
 * Created by nikit_000 on 5/6/2018.
 */

public class CancellationToken {
    CancellationToken(CancellationTokenSource source) {
        this.source = source;
    }

    private CancellationTokenSource source;

    public boolean isCancelled() {
        return source.isCancelled();
    }

    public static final CancellationToken DUMMY = new CancellationToken(new CancellationTokenSource());
}
