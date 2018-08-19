package org.duckdns.dcnick3.learnenglish.wordsets;

/**
 * Created by nikit_000 on 2/5/2018.
 */

public class WordSet {
    public WordSet() {}

    public WordSet(Integer id, String name, String localizedName, String baseLanguage,
                   String targetLanguage, String description, String[] tags, String iconHash,
                   boolean isActive, int repeatCount) {
        this.id = id;
        this.name = name;
        this.localizedName = localizedName;
        this.baseLanguage = baseLanguage;
        this.targetLanguage = targetLanguage;
        this.description = description;
        this.tags = tags.clone();
        this.iconHash = iconHash;
        this.isActive = isActive;
        this.repeatCount = repeatCount;
    }

    public Integer id;
    public String name;
    public String localizedName;
    public String baseLanguage;
    public String targetLanguage;
    public String description;
    public String[] tags;
    public String iconHash;
    public boolean isActive;
    public int repeatCount;
}
