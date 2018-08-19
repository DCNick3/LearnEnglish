package org.duckdns.dcnick3.learnenglish.wordsets;

import android.support.annotation.Nullable;

import com.google.gson.annotations.Expose;

import java.util.Date;

/**
 * Created by nikit_000 on 4/28/2018.
 */

public class WordEntry {

    public WordEntry() {
        lastHit = -1;
    }

    public Integer id;
    public Integer wordset;
    public Integer hits;
    public Integer lastHit;
    public boolean learned;
    public Date lastHitDate;
    @Expose public String word;
    @Expose public String translation;
    @Expose public String samples;

    public boolean validateDeserialized() {
        return word != null && translation != null && samples != null;
    }
}
