package org.duckdns.dcnick3.learnenglish.wordsets;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.Pair;
import android.util.SparseArray;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.duckdns.dcnick3.learnenglish.wordsets.remote.RemoteRepository;
import org.duckdns.dcnick3.learnenglish.wordsets.remote.RemoteWordpack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by nikit_000 on 4/28/2018.
 */

public class WordSetDatabase {
    private static final String WORDS_INDEX_NAME = "words_index";

    /* TODO: finish documenting this class (and others) */

    public WordSetDatabase(Context context, File path) {
        this.path = path;
        if (!this.path.exists())
            if (!this.path.mkdir())
                throw new IllegalStateException();

        database = new WordSetDatabaseOpenHelper(context,
                new File(this.path, DATABASE_FILE_NAME), 1)
                .getWritableDatabase();


        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }

    private static final String DATABASE_FILE_NAME = "wordsets.db";

    private static final String WORDS_TABLE_NAME = "words";
    private static final String WORDSETS_TABLE_NAME = "wordsets";
    private static final String REPOSITORIES_TABLE_NAME = "repositories";

    private File path;
    private Gson gson;
    Lock lock = new ReentrantLock();

    private SQLiteDatabase database;

    /* DB stuff */

    public void close() {
        database.close();
    }
    public void beginTransaction() {
        database.beginTransaction();
    }
    public void endTransaction() {
        database.endTransaction();
    }
    public void setTransactionSuccessful() {
        database.setTransactionSuccessful();
    }
    public void lock() { lock.lock(); }
    public void unlock() { lock.unlock(); }


    /* Learning stuff */

    public boolean checkCanLearn() {
        lock();
        try {
            long num = DatabaseUtils.queryNumEntries(database, WORDS_TABLE_NAME,
                    "wordset IN (SELECT id FROM " + WORDSETS_TABLE_NAME + " WHERE is_active != 0)" +
                            " AND hits != (SELECT repeat_count FROM " + WORDSETS_TABLE_NAME + " WHERE id = wordset)");
            return num != 0;
        } finally {
            unlock();
        }
    }
    public WordEntry getNextWordForLearning() {
        lock();
        try (Cursor cur = database.query(WORDS_TABLE_NAME,
                new String[] {"id", "wordset", "word", "translation", "samples" },
                "wordset IN (SELECT id FROM " + WORDSETS_TABLE_NAME + " WHERE is_active != 0) AND hits = 0",
                new String[] {  }, null, null, "RANDOM()", "1")) {
            if (cur.getCount() == 0)
                return null;
            cur.moveToFirst();

            WordEntry word = new WordEntry();
            word.hits = 0;
            word.id = cur.getInt(0);
            word.wordset = cur.getInt(1);
            word.word = cur.getString(2);
            word.translation = cur.getString(3);
            word.samples = cur.getString(4);
            word.lastHit = -1;
            word.lastHitDate = new Date(0);

            WordSet ws = getWordset(word.wordset);
            word.learned = word.hits >= ws.repeatCount;

            return word;
        } finally {
            unlock();
        }
    }
    public WordEntry[] getLearningWords() {
        lock();
        try (Cursor cur = database.query(WORDS_TABLE_NAME,
                new String[] { "id", "wordset", "word", "translation", "samples", "hits", "last_hit", "last_hit_date" },
                "hits > 0 AND (SELECT is_active FROM " + WORDSETS_TABLE_NAME + " " +
                        "WHERE id = wordset) != 0 AND hits < (SELECT repeat_count FROM " + WORDSETS_TABLE_NAME + " " +
                        "WHERE id = wordset)", new String[] {}, null, null, null)) {

            SparseArray<WordSet> wsm = new SparseArray<>();

            WordEntry[] res = new WordEntry[cur.getCount()];
            for (int i = 0; i < res.length; i++) {
                cur.moveToNext();
                WordEntry w = new WordEntry();

                w.id = cur.getInt(0);
                w.wordset = cur.getInt(1);
                w.word = cur.getString(2);
                w.translation = cur.getString(3);
                w.samples = cur.getString(4);
                w.hits = cur.getInt(5);
                w.lastHit = cur.getInt(6);
                w.lastHitDate = new Date(cur.getLong(7));

                WordSet ws = wsm.get(w.wordset, null);
                if (ws == null) {
                    ws = getWordset(w.wordset);
                    wsm.put(w.wordset, ws);
                }
                w.learned = w.hits >= ws.repeatCount;

                res[i] = w;
            }
            return  res;
        } finally {
            unlock();
        }
    }


    /* WordEntry stuff */

    private ContentValues getWordValues(WordEntry word) {
        ContentValues val = new ContentValues();
        if (word.id != null)
            val.put("id", word.id);
        val.put("hits", word.hits == null ? 0 : word.hits);
        val.put("wordset", word.wordset);
        val.put("word", word.word);
        val.put("translation", word.translation);
        val.put("samples", word.samples);
        val.put("last_hit", word.lastHit);
        if (word.lastHitDate != null)
            val.put("last_hit_date", word.lastHitDate.getTime());
        else
            val.put("last_hit_date", 0);

        return val;
    }
    /**
     * Inserts {@code WordEntry} object into database, updating it's {@code id} field
     * @param word object to insert to database
     */
    public void insertWord(WordEntry word) {
        lock();
        try {
            word.id = (int) database.insert(WORDS_TABLE_NAME, null, getWordValues(word));
        } finally {
            unlock();
        }
    }
    /**
     * Updates database record for {@code WordEntry} object (by {@code id} field)
     * @param word Object to update information about
     */
    public void updateWord(WordEntry word) {
        lock();
        try {
            database.update(WORDS_TABLE_NAME, getWordValues(word),
                    "id=?", new String[]{Integer.toString(word.id)});
        } finally {
            unlock();
        }
    }
    /**
     * Fetches all words from wordset, tolerating limit
     * @param wordset wordset to fetch words from
     * @param limit maximum size of returned array
     * @return array of words fetched, truncated so size <= limit
     */
    public WordEntry[] getWords(int wordset, Integer limit) {

        if (limit == null) {
            limit = Integer.MAX_VALUE;
        }

        lock();
        try (Cursor cur = database.query(WORDS_TABLE_NAME,
                new String[] { "id", "word", "translation", "samples", "hits", "last_hit", "last_hit_date" },
                "wordset = ?", new String[] { Integer.toString(wordset) },
                null, null, null, Integer.toString(limit))) {

            WordSet ws = getWordset(wordset);

            WordEntry[] res = new WordEntry[cur.getCount()];
            for (int i = 0; i < res.length; i++) {
                cur.moveToNext();
                WordEntry w = new WordEntry();

                w.id = cur.getInt(0);
                w.wordset = wordset;
                w.word = cur.getString(1);
                w.translation = cur.getString(2);
                w.samples = cur.getString(3);
                w.hits = cur.getInt(4);
                w.lastHit = cur.getInt(5);
                w.lastHitDate = new Date(cur.getLong(7));

                w.learned = w.hits >= ws.repeatCount;

                res[i] = w;
            }
            return res;
        } finally {
            unlock();
        }
    }
    /**
     * Searches words in wordset, that have {@code word} substring in it, tolerating limit
     * @param wordset wordset to search words in
     * @param word a substring to search for
     * @param limit limit
     * @return found words, ordered by
     */
    public WordEntry[] searchWords(Integer wordset, String word, Integer limit) {
        word = word.toLowerCase().replace("%", "").replace("_", "");

        if (limit == null) limit = Integer.MAX_VALUE;

        String wh = wordset != null ? "wordset = ? AND word LIKE ?" : "word LIKE ?";
        String[] pr = wordset != null ? new String[] { wordset.toString(), "%" + word + "%" } : new String[] { "%" + word + "%" };
        String esc1 = DatabaseUtils.sqlEscapeString(word);
        String esc2 = DatabaseUtils.sqlEscapeString(word + "%");


        lock();
        try (Cursor cur = database.query(WORDS_TABLE_NAME,
                new String[] { "id", "wordset", "word", "translation", "samples", "hits", "last_hit", "last_hit_date" },
                wh, pr, null, null, "(CASE WHEN LOWER(word) = " + esc1 + " THEN 1 WHEN word LIKE " + esc2
                        + " THEN 2 ELSE 3 END), word", Integer.toString(limit))) {

            SparseArray<WordSet> wsm = new SparseArray<>();

            WordEntry[] res = new WordEntry[cur.getCount()];
            for (int i = 0; i < res.length; i++) {
                cur.moveToNext();
                WordEntry w = new WordEntry();

                w.id = cur.getInt(0);
                w.wordset = cur.getInt(1);
                w.word = cur.getString(2);
                w.translation = cur.getString(3);
                w.samples = cur.getString(4);
                w.hits = cur.getInt(5);
                w.lastHit = cur.getInt(6);
                w.lastHitDate = new Date(cur.getLong(7));

                WordSet ws = wsm.get(w.wordset, null);
                if (ws == null) {
                    ws = getWordset(w.wordset);
                    wsm.put(w.wordset, ws);
                }
                w.learned = w.hits >= ws.repeatCount;

                res[i] = w;
            }
            return  res;
        } finally {
            unlock();
        }
    }


    /* WordSet stuff */

    private ContentValues getWordsetValues(WordSet ws) {
        ContentValues val = new ContentValues();
        val.put("name", ws.name);
        val.put("localized_name", ws.localizedName);
        val.put("base_language", ws.baseLanguage);
        val.put("target_language", ws.targetLanguage);
        val.put("icon_hash", ws.iconHash);
        val.put("description", ws.description);
        val.put("tags", gson.toJson(ws.tags));
        val.put("is_active", ws.isActive ? 1 : 0);
        val.put("repeat_count", ws.repeatCount);

        return  val;
    }
    /**
     * Inserts wordset object into database, updating id field to corresponding Wordset id
     * @param ws Wordset object to insert
     * @return id of inserted wordset
     */
    public int insertWordSet(WordSet ws) {
        lock();
        try {
            return ws.id = (int) database.insert(WORDSETS_TABLE_NAME, null, getWordsetValues(ws));
        }finally {
            unlock();
        }
    }
    /**
     * Updates values for specified wordset (by id)
     * @param ws
     */
    public void updateWordSet(WordSet ws) {
        lock();
        try {
            database.update(WORDSETS_TABLE_NAME, getWordsetValues(ws),
                    "id=?", new String[]{Integer.toString(ws.id)});
        }finally {
            unlock();
        }
    }
    /**
     * Fetch all installed wordsets
     * @return array of installed Wordsets
     */
    public WordSet[] getWordSets() {
        lock();
        try (Cursor cur = database.query(WORDSETS_TABLE_NAME,
                new String[] { "id", "name", "localized_name", "base_language", "target_language",
                        "description", "tags", "icon_hash", "is_active", "repeat_count" }, null, null, null, null, null)) {

            WordSet[] wsts = new WordSet[cur.getCount()];
            for (int i = 0; cur.moveToNext(); i++) {
                wsts[i] = new WordSet(cur.getInt(0), cur.getString(1), cur.getString(2), cur.getString(3),
                        cur.getString(4), cur.getString(5),
                        gson.fromJson(cur.getString(6), String[].class), cur.getString(7),
                        cur.getInt(8) != 0, cur.getInt(9));
            }

            return wsts;
        } finally {
            unlock();
        }
    }
    /**
     * Finds wordset object by id
     * @param id id to search for
     * @return Wordset object if found or null otherwise
     */
    public WordSet getWordset(int id) {
        lock();
        try (Cursor cur = database.query(WORDSETS_TABLE_NAME,
                new String[]{"name", "localized_name", "base_language", "target_language",
                        "description", "tags", "icon_hash", "is_active", "repeat_count" },
                "id = ?", new String[]{Integer.toString(id)}, null, null, null)) {
            if (cur.getCount() == 0)
                return null;
            else {
                cur.moveToFirst();
                WordSet ws = new WordSet();
                ws.id = id;
                ws.name = cur.getString(0);
                ws.localizedName = cur.getString(1);
                ws.baseLanguage = cur.getString(2);
                ws.targetLanguage = cur.getString(3);
                ws.description = cur.getString(4);
                ws.tags = gson.fromJson(cur.getString(5), String[].class);
                ws.iconHash = cur.getString(6);
                ws.isActive = cur.getInt(7) != 0;
                ws.repeatCount = cur.getInt(8);
                return ws;
            }
        } finally {
            unlock();
        }
    }
    /**
     * Gets progress information about wordset
     * @param wordset wordset to get info about
     * @return tuple of (learned, total) values
     */
    public Pair<Integer, Integer> getWordsetProgress(int wordset) {
        lock();
        try {
            int total = (int) DatabaseUtils.queryNumEntries(database, WORDS_TABLE_NAME,
                    "wordset = ?", new String[]{Integer.toString(wordset)});
            int learned = (int) DatabaseUtils.queryNumEntries(database, WORDS_TABLE_NAME, "" +
                            "wordset = ? AND hits = (SELECT repeat_count FROM " + WORDSETS_TABLE_NAME + " WHERE id = ?)",
                    new String[]{Integer.toString(wordset), Integer.toString(wordset)});
            return new Pair<Integer, Integer>(learned, total);
        } finally {
            unlock();
        }
    }
    /**
     * Checks whether wordset with specified name is installed
     * @param name wordset name to search
     * @return whether wordset with specified name is installed
     */
    public boolean isWordsetInstalled(String name) {
        lock();
        try {
            return DatabaseUtils.queryNumEntries(database, WORDSETS_TABLE_NAME, "name = ?",
                new String[] { name }) > 0;
        } finally {
            unlock();
        }
    }
    /**
     * Searches for wordset with specified name
     * @param name Wordset name to search for
     * @return WordSet object if found or null otherwise
     */
    public WordSet getWordsetByName(String name) {
        lock();
        try (Cursor cur = database.query(WORDSETS_TABLE_NAME, new String[]
                        {"id", "localized_name", "base_language", "target_language",
                        "description", "tags", "icon_hash", "is_active", "repeat_count"},
                "name = ?", new String[] { name },
                null, null, null)) {

            if (cur.getCount() == 0)
                return null;

            cur.moveToFirst();
            WordSet ws = new WordSet();
            ws.id = cur.getInt(0);
            ws.name = name;
            ws.localizedName = cur.getString(1);
            ws.baseLanguage = cur.getString(2);
            ws.targetLanguage = cur.getString(3);
            ws.description = cur.getString(4);
            ws.tags = gson.fromJson(cur.getString(5), String[].class);
            ws.iconHash = cur.getString(6);
            ws.isActive = cur.getInt(7) != 0;
            ws.repeatCount = cur.getInt(8);

            return ws;
        } finally {
            unlock();
        }
    }
    /**
     * Deletes WordSet and all associated words from database
     * @param id id of wordset to delete
     */
    public void deleteWordset(int id) {
        lock();
        beginTransaction();
        try {
            database.delete(WORDSETS_TABLE_NAME, "id = ?", new String[] { Integer.toString(id) });
            database.delete(WORDS_TABLE_NAME, "wordset = ?", new String[] { Integer.toString(id) });
            setTransactionSuccessful();
        } finally {
            endTransaction();
            unlock();
        }

    }
    /**
     * Deactivates all wordsets
     */
    public void deactivateAllWordsets() {
        lock();
        try {
            ContentValues val = new ContentValues();
            val.put("is_active", false);
            database.update(WORDSETS_TABLE_NAME, val, null, null);
        } finally {
            unlock();
        }
    }


    /* RemoteRepository stuff */

    private ContentValues getRemoteRepositoryValues(RemoteRepository repo) {
        ContentValues v = new ContentValues();
        if (repo.id != null)
            v.put("id", repo.id);
        v.put("url", repo.url.toString());
        v.put("localized_name", repo.localizedName);
        v.put("wordsets", gson.toJson(repo.wordpacks));
        return v;
    }
    public void insertRemoteRepository(RemoteRepository repo) {
        lock();
        try {
            repo.id = (int)database.insert(REPOSITORIES_TABLE_NAME, null, getRemoteRepositoryValues(repo));
        } finally {
            unlock();
        }
    }
    public void updateRemoteRepository(RemoteRepository repo) {
        lock();
        try {
            database.update(REPOSITORIES_TABLE_NAME, getRemoteRepositoryValues(repo),
                    "id = ?", new String[] { repo.id.toString() });
        } finally {
            unlock();
        }
    }
    public RemoteRepository[] getRemoteRepositories() {
        lock();
        try (Cursor cur = database.query(REPOSITORIES_TABLE_NAME, new String[] { "id", "url", "localized_name", "wordsets" },
                null, null, null, null, "id");) {
            RemoteRepository[] repos = new RemoteRepository[cur.getCount()];
            for (int i = 0; cur.moveToNext(); i++) {
                repos[i] = new RemoteRepository();
                repos[i].id = cur.getInt(0);
                repos[i].url = new URL(cur.getString(1));
                repos[i].localizedName = cur.getString(2);
                repos[i].wordpacks = gson.fromJson(cur.getString(3), RemoteWordpack[].class);
            }
            return repos;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } finally {
            unlock();
        }
    }
    public void deleteRemoteRepository(int id) {
        lock();
        try {
            database.delete(REPOSITORIES_TABLE_NAME, "id = ?", new String[]{ Integer.toString(id) });
        } finally {
            unlock();
        }
    }
    public boolean isRepositoryInstalled(String url) {
        lock();
        try {
            return DatabaseUtils.queryNumEntries(database, REPOSITORIES_TABLE_NAME,
                    "url = ?", new String[] { url }) > 0;
        } finally {
            unlock();
        }
    }


    private class WordSetDatabaseOpenHelper extends SQLiteOpenHelper {
        private final String CREATE_WORDS =
                "CREATE TABLE " + WORDS_TABLE_NAME + "(" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "wordset INTEGER NOT NULL," +
                        "word TEXT NOT NULL," +
                        "translation TEXT NOT NULL," +
                        "samples TEXT NOT NULL," +
                        "hits INTEGER NOT NULL," +
                        "last_hit INTEGER NOT NULL," +
                        "last_hit_date INTEGER NOT NULL" +
                        ")";
        private final String CREATE_WORDS_INDEX =
                "CREATE INDEX " + WORDS_INDEX_NAME + " ON " + WORDS_TABLE_NAME + "(wordset);";
        private final String CREATE_WORDSETS =
                "CREATE TABLE " + WORDSETS_TABLE_NAME + "(" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL UNIQUE," +
                        "localized_name TEXT NOT NULL," +
                        "base_language TEXT NOT NULL," +
                        "target_language TEXT NOT NULL," +
                        "description TEXT NOT NULL," +
                        "tags TEXT NOT NULL," +
                        "icon_hash TEXT NOT NULL," +
                        "is_active INTEGER NOT NULL," +
                        "repeat_count INTEGER NOT NULL" +
                        ");";
        private final String CREATE_REPOSITORIES =
                "CREATE TABLE " + REPOSITORIES_TABLE_NAME + "(" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "url TEXT NOT NULL UNIQUE," +
                        "localized_name TEXT NOT NULL," +
                        "wordsets TEXT NOT NULL" +
                        ");";

        WordSetDatabaseOpenHelper(Context context, File file, int version) {
            super(context, file.getPath(), null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase sqliteDatabase) {
            sqliteDatabase.execSQL(CREATE_WORDS);
            sqliteDatabase.execSQL(CREATE_WORDS_INDEX);
            sqliteDatabase.execSQL(CREATE_WORDSETS);
            sqliteDatabase.execSQL(CREATE_REPOSITORIES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        }
    }
}

