package org.duckdns.dcnick3.learnenglish.wordsets;

import org.duckdns.dcnick3.learnenglish.ProgressListener;

public interface FaultyProgressListener extends ProgressListener {
    void faulted(Object sender);
}
