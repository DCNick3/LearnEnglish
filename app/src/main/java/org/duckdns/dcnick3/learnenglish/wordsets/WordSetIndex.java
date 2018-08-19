package org.duckdns.dcnick3.learnenglish.wordsets;

import com.google.gson.annotations.Expose;

/**
 * Created by nikit_000 on 5/5/2018.
 */

public class WordSetIndex
{
    @Expose public String id;
    @Expose public String localizedName;
    @Expose public String baseLanguage;
    @Expose public String targetLanguage;
    @Expose public String description;
    @Expose public String[] tags;
    @Expose public int repeatCount;

    public boolean validate() {
        return baseLanguage != null && description != null && id != null
                && localizedName != null && tags != null && targetLanguage != null
                && repeatCount != 0;
    }
}
