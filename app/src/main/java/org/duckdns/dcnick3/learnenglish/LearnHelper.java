package org.duckdns.dcnick3.learnenglish;

import android.content.SharedPreferences;
import android.support.v7.util.SortedList;
import android.util.SparseArray;

import org.duckdns.dcnick3.learnenglish.wordsets.WordEntry;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSet;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSetDatabase;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

public class LearnHelper {
    public LearnHelper(WordSetDatabase database, SharedPreferences prefs) {
        this.database = database;
        this.prefs = prefs;

        hitSeq = prefs.getInt(HIT_SEQ_KEY, 0);

        workSet.addAll(Arrays.asList(database.getLearningWords()));
    }

    private static final String HIT_SEQ_KEY = "hit_seq";
    private static final int REPEAT_WORD_THRESH = 20;
    private static final int PREFFERED_WORD_SET_SIZE = 30;

    private SparseArray<WordSet> wordsetCache = new SparseArray<>();

    private WordSetDatabase database;
    private SharedPreferences prefs;
    private int hitSeq;
    private SortedSet<WordEntry> workSet = new TreeSet<>(new Comparator<WordEntry>() {
        @Override
        public int compare(WordEntry o1, WordEntry o2) {
            return o1.lastHit - o2.lastHit;
        }
    });
    private Random rnd = new Random();

    private void nextSeq() {
        hitSeq++;
        prefs.edit().putInt(HIT_SEQ_KEY, hitSeq).apply();
    }

    private WordEntry selectWord() {
        int mx = Math.max(2, workSet.size() - REPEAT_WORD_THRESH);
        if (workSet.size() == 1) mx = 1;
        int selcd = rnd.nextInt(mx) + 1;
        Iterator<WordEntry> it = workSet.iterator();
        WordEntry we = null;
        for (int i = 0; i < selcd; i++) we = it.next();
        return we;
    }

    public WordEntry nextWord() {
        if (workSet.size() == 0) {
            return addWord();
        } else {
            if (hitSeq - workSet.first().lastHit < PREFFERED_WORD_SET_SIZE) {
                WordEntry we = addWord();
                if (we == null)
                    return selectWord();
                else
                    return we;
            } else {
                return selectWord();
            }
        }
    }

    private WordEntry addWord() {
        WordEntry we = database.getNextWordForLearning();
        if (we != null) workSet.add(we);
        return we;
    }

    private WordSet getWordset(int id) {
        WordSet ws = wordsetCache.get(id);
        if (ws != null)
            return ws;
        else {
            ws = database.getWordset(id);
            wordsetCache.append(ws.id, ws);
            return ws;
        }
    }

    public void submitWord(WordEntry word, boolean reminded) {
        workSet.remove(word);
        word.lastHit = hitSeq;
        word.lastHitDate = new Date();
        WordSet ws = getWordset(word.wordset);
        if (word.hits == 0 && reminded) {
            word.hits = ws.repeatCount;
        } else {
            if (reminded)
                word.hits++;
            else {
                word.hits--;
                if (word.hits < 1)
                    word.hits = 1;
            }
        }
        if (word.hits < ws.repeatCount) {
            workSet.add(word);
        }
        database.updateWord(word);
        nextSeq();
    }
}
