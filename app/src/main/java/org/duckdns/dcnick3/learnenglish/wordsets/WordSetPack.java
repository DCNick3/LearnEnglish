package org.duckdns.dcnick3.learnenglish.wordsets;

import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.duckdns.dcnick3.learnenglish.CancellationToken;
import org.duckdns.dcnick3.learnenglish.ProgressListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by nikit_000 on 5/5/2018.
 */

public class WordSetPack {
    public WordSetPack(File file) throws IOException {
        zip = new ZipFile(file);
        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }

    private static final String WORDS_FILE_NAME = "words.txt";
    private static final String INDEX_FILE_NAME = "index.json";
    private static final String ICON_FILE_NAME = "icon.png";
    private static final String TAG = "WordSetPack";

    private static final int PROGRESS_REGULARITY = 50;

    private Gson gson;
    private ZipFile zip;
    private WordSetIndex index;
    private int wordCount;

    private boolean checkIfBitmap(InputStream stream) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, opt);
        return opt.outWidth != -1 && opt.outHeight != -1;
    }

    public boolean readInto(ProgressListener progress, WordSetDatabase database, IconManager icons,
                            CancellationToken cancellationToken, String domain) throws IOException {
        database.lock();
        database.beginTransaction();
        try {
            Log.i(TAG, "Starting installation of " + zip.getName() + " wordpack");
            long startTime = System.currentTimeMillis();


            String iconHash = null;

            if (zip.getEntry(WORDS_FILE_NAME) == null || zip.getEntry(INDEX_FILE_NAME) == null
                    || zip.getEntry(ICON_FILE_NAME) == null ) {
                Log.e(TAG, "Error installing " + zip.getName() + "; Not all files are present");
                return false;
            }
            try (InputStream is = zip.getInputStream(zip.getEntry(INDEX_FILE_NAME))) {

                index = gson.fromJson(new InputStreamReader(is), WordSetIndex.class);
                if (!index.validate()) {
                    Log.e(TAG, "Error installing " + zip.getName() + "; Index is missing some fields");
                    return false;
                }
            } catch (JsonParseException ex) {
                return false;
            }
            try (InputStream str = zip.getInputStream(zip.getEntry(ICON_FILE_NAME))) {
                if (!checkIfBitmap(str)) {
                    Log.e(TAG, "Error installing " + zip.getName() + "; Icon is not a Bitmap");
                    return false;
                }
            }

            try (InputStream str = zip.getInputStream(zip.getEntry(ICON_FILE_NAME))) {
                iconHash = icons.addIcon(str);
            }

            WordSet ws = new WordSet(null, index.id + "@" + domain, index.localizedName, index.baseLanguage,
                    index.targetLanguage, index.description, index.tags, iconHash, false, index.repeatCount);
            int wordsetId = database.insertWordSet(ws);

            int counter = 0;
            String line;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(zip.getEntry(WORDS_FILE_NAME))))) {

                while (br.readLine() != null) counter++;

            }
            wordCount = counter;
            counter = 0;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(zip.getEntry(WORDS_FILE_NAME))))) {

                while ((line = br.readLine()) != null) {
                    WordEntry entry = gson.fromJson(line, WordEntry.class);
                    if (!entry.validateDeserialized()) {
                        Log.e(TAG, "Error installing " + zip.getName() + " (word #" + counter + ");" +
                                " Not all word fields are present");
                        return false;
                    }
                    entry.wordset = wordsetId;
                    entry.hits = 0;
                    entry.id = null;
                    database.insertWord(entry);
                    if (counter % PROGRESS_REGULARITY == 0)
                        progress.onProgress(counter, wordCount);
                    if (cancellationToken.isCancelled())
                        return false;
                    counter++;
                }

            } catch (JsonParseException ex) {
                Log.e(TAG, "Error installing " + zip.getName() + " (word #" + counter + ")", ex);
                return false;
            }

            database.setTransactionSuccessful();
            long time = (System.currentTimeMillis() - startTime);
            Log.i(TAG, "Successfully installed " + zip.getName() + " in " + time + " ms");

            progress.finished(wordCount, time);

            return  true;
        } finally {
            database.endTransaction();
            database.unlock();
        }
    }

    public void close() throws IOException {
        zip.close();
    }
}
