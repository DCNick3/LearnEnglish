package org.duckdns.dcnick3.learnenglish.wordsets.remote;

import android.support.annotation.Nullable;

import com.google.gson.annotations.Expose;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class RemoteWordpack {
    public @Expose String name;
    public @Expose String localizedName;
    public @Expose String iconHash;
    public @Nullable @Expose Integer wordCount;

    public RemoteRepository repository;
    public boolean installed;
    public RemoteWordpackInstaller installer;

    public boolean validate() {
        return name != null && !name.contains("@") && localizedName != null
                && iconHash != null;
    }

    public String getGlobalId() {
        return name + "@" + repository.getGlobalId();
    }

    public URL getUrl() {
        try {
            return repository.url.toURI().resolve(RemoteWordpackManager.REPO_WORDPACKS_PATH).resolve(name + ".zip").toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
