package org.duckdns.dcnick3.learnenglish.wordsets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.duckdns.dcnick3.learnenglish.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by nikit_000 on 5/5/2018.
 */

public class IconManager {
    public IconManager(Context context, File path) {
        if (!path.exists())
            if (!path.mkdir())
                throw new RuntimeException();

        this.path = path;
        cacheDir = context.getCacheDir();
        lock = new ReentrantLock();
    }

    private Lock lock;
    private File cacheDir;
    private File path;

    private final static String iconExcension = ".png";

    public Bitmap loadIcon(String hash) {
        lock.lock();
        try {
            return BitmapFactory.decodeFile(new File(path, hash + iconExcension).getPath());
        } finally {
            lock.unlock();
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public String addIcon(InputStream iconStream) throws IOException {
        File tmpFile = null;
        OutputStream os = null;
        FileInputStream inp = null;
        lock.lock();
        try {

            tmpFile = File.createTempFile("icon", null, cacheDir);
            os = new FileOutputStream(tmpFile);

            copyStream(iconStream, os);
            os.close();
            os = null;

            inp = new FileInputStream(tmpFile);

            String hash = Util.sha1(inp);

            inp.close();
            inp = null;

            File fname = new File(path, hash + iconExcension);
            tmpFile.renameTo(fname);
            return hash;
        }
        finally {
            lock.unlock();
            if (os != null) os.close();
            if (tmpFile != null) tmpFile.delete();
            if (inp != null) inp.close();
        }
    }
}
