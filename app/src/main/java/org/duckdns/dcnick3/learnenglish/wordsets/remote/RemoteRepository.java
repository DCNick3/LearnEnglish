package org.duckdns.dcnick3.learnenglish.wordsets.remote;

import android.view.ViewDebug;

import com.google.gson.annotations.Expose;

import org.duckdns.dcnick3.learnenglish.Util;

import java.net.URL;
import java.util.Collections;

public class RemoteRepository {
    public Integer id;
    public URL url;
    @Expose public String localizedName;
    @Expose public RemoteWordpack[] wordpacks;

    public String getGlobalId() {
        return Util.sha1(url.toString());
    }

    public boolean validate() {
        if (localizedName == null || wordpacks == null)
            return false;
        for (RemoteWordpack pack : wordpacks) {
            if (!pack.validate()) {
                return false;
            }
        }
        return true;
    }
}
