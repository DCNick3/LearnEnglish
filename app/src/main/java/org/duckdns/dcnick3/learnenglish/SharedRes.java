package org.duckdns.dcnick3.learnenglish;

import android.content.Context;

import org.duckdns.dcnick3.learnenglish.wordsets.IconManager;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSetDatabase;
import org.duckdns.dcnick3.learnenglish.wordsets.remote.RemoteWordpackManager;

import java.io.File;

/**
 * Created by nikit_000 on 5/5/2018.
 */

public final class SharedRes {
    private SharedRes() {}

    private static WordSetDatabase db;
    private static IconManager iman;
    private static RemoteWordpackManager rwman;

    public static synchronized void initialize(Context context) {
        db = new WordSetDatabase(context, new File(context.getFilesDir(), "database"));
        iman = new IconManager(context, new File(context.getFilesDir(), "database/icons"));
        rwman = new RemoteWordpackManager(new File(context.getCacheDir(), "repos"), db, iman);
    }

    public static synchronized WordSetDatabase openDatabase() {
        return db;
    }
    public static synchronized IconManager openIconManager() {
        return iman;
    }
    public static synchronized RemoteWordpackManager openWordpackManager() {
        return rwman;
    }
}
