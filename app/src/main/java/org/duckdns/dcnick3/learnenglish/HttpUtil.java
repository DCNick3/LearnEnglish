package org.duckdns.dcnick3.learnenglish;

import android.support.annotation.NonNull;
import android.text.InputType;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class HttpUtil {
    public static Task<String> get(final URL url) {
        Log.i("HttpUtil", "Reading " + url + " as String");
        return Task.callInBackground(new Callable<String>() {
            @Override
            public String call() throws Exception {
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                try {
                    con.setRequestProperty("Accept-Encoding", "identity");
                    final int bufferSize = 1024;
                    final char[] buffer = new char[bufferSize];
                    final StringBuilder out = new StringBuilder();
                    try (Reader in = new InputStreamReader(con.getInputStream(), "UTF-8")) {
                        for (; ; ) {
                            int rsz = in.read(buffer, 0, buffer.length);
                            if (rsz < 0)
                                break;
                            out.append(buffer, 0, rsz);
                        }
                        return out.toString();
                    }
                } finally {
                    con.disconnect();
                }
            }
        });
    }

    public static Task<InputStream> getStream(final URL url) {
        Log.i("HttpUtil", "Reading " + url + " as stream");
        return Task.callInBackground(new Callable<InputStream>() {
            @Override
            public InputStream call() throws Exception {
                final HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestProperty("Accept-Encoding", "identity");
                final InputStream is = con.getInputStream();
                /* return wrapped stream to handle disconnection on stream close */
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return is.read();
                    }

                    @Override
                    public int read(@NonNull byte[] b) throws IOException {
                        return is.read(b);
                    }

                    @Override
                    public int read(@NonNull byte[] b, int off, int len) throws IOException {
                        return is.read(b, off, len);
                    }

                    @Override
                    public long skip(long n) throws IOException {
                        return is.skip(n);
                    }

                    @Override
                    public int available() throws IOException {
                        return is.available();
                    }

                    @Override
                    public synchronized void mark(int readlimit) {
                        is.mark(readlimit);
                    }

                    @Override
                    public synchronized void reset() throws IOException {
                        is.reset();
                    }

                    @Override
                    public boolean markSupported() {
                        return is.markSupported();
                    }

                    @Override
                    public void close() throws IOException {
                        super.close();
                        con.disconnect();
                    }
                };
            }
        });
    }

    public static Task<URL> resolveRedirect(final URL url) {
        Log.i("HttpUtil", "Resolving redirects of " + url);
        return Task.callInBackground(new Callable<URL>() {
            @Override
            public URL call() throws Exception {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                try {
                    con.setRequestProperty("Accept-Encoding", "identity");
                    con.setInstanceFollowRedirects(false);
                    int res = con.getResponseCode();
                    if (res == 301 || res == 302 || res == 307 || res == 308) {
                        Task<URL> u = resolveRedirect(new URL(con.getHeaderField("Location")));
                        u.waitForCompletion();
                        return u.getResult();
                    } else {
                        return url;
                    }
                } finally {
                    con.disconnect();
                }
            }
        });
    }

    public static Task<Boolean> getFile(final URL url, final File path) {
        return getFile(url, path, CancellationToken.DUMMY, new ProgressListener() {
            @Override
            public void onProgress(int done, int total) {
            }

            @Override
            public void finished(int total, long time) {
            }
        });
    }

    public static Task<Boolean> getFile(final URL url, final File path,
                                     final CancellationToken cancellationToken, final ProgressListener progressListener) {
        Log.i("HttpUtil", "Downloading " + url + " to " + path);
        return Task.callInBackground(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Boolean downloadFinished = false;
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                try {
                    long startTime = System.currentTimeMillis();
                    con.setRequestProperty("Accept-Encoding", "identity");

                    int total = con.getContentLength();
                    final int bufferSize = 512;
                    final byte[] buffer = new byte[bufferSize];
                    try (final OutputStream out = new FileOutputStream(path)) {
                        int counter = 0;
                        try (InputStream in = con.getInputStream()) {
                            for (; ; ) {
                                if (cancellationToken.isCancelled())
                                    return false;

                                progressListener.onProgress(counter, total);
                                int rsz = in.read(buffer, 0, buffer.length);
                                if (rsz < 0)
                                    break;
                                out.write(buffer);
                                counter += bufferSize;
                            }
                            downloadFinished = true;
                            progressListener.finished(total, System.currentTimeMillis() - startTime);
                        }
                    }
                } finally {
                    con.disconnect();
                    if (!downloadFinished && path.exists())
                        path.delete();
                }
                return true;
            }
        });
    }
}
