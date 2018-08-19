package org.duckdns.dcnick3.learnenglish;

import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {
    public static float dpFromPx(final Context context, final float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public static float pxFromDp(final Context context, final float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    private final static char[] hexArray = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String sha1(String message) {
        try {
            return sha1(new ByteArrayInputStream(message.getBytes("UTF-8")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String sha1(InputStream stream) throws IOException {

        MessageDigest dig;
        try {
            dig = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException();
        }

        byte[] buf = new byte[4096];
        int read;
        while ((read = stream.read(buf)) != -1) {
            dig.update(buf, 0, read);
        }

        return bytesToHex(dig.digest());
    }
}
