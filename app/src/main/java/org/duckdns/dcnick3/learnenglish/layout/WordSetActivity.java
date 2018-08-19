package org.duckdns.dcnick3.learnenglish.layout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.SharedRes;
import org.duckdns.dcnick3.learnenglish.wordsets.IconManager;
import org.duckdns.dcnick3.learnenglish.wordsets.WordEntry;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSet;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSetDatabase;

import java.util.Random;

/**
 * Created by nikit_000 on 4/28/2018.
 */

public class WordSetActivity extends Activity {

    WordSet wordset;
    WordSetDatabase database;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wordset);

        database = SharedRes.openDatabase();
        IconManager iman = SharedRes.openIconManager();

        wordset =  database.getWordset(getIntent().getIntExtra("wordset", -1));
        ImageView icon = findViewById(R.id.wordset_icon);
        TextView name = findViewById(R.id.wordset_name);
        TextView desc = findViewById(R.id.wordset_description);

        icon.setImageBitmap(iman.loadIcon(wordset.iconHash));
        name.setText(wordset.localizedName);
        desc.setText(wordset.description);
    }

    public void onBrowseButtonClick(View v) {
        Intent i = new Intent(WordSetActivity.this, WordSetBrowserActivity.class);
        i.putExtra("wordset", wordset.id);
        startActivity(i);
    }

    public void onSelectButtonClick(View view) {
        database.deactivateAllWordsets();
        wordset.isActive = true;
        database.updateWordSet(wordset);

        Intent i = new Intent(this, LearningActivity.class);
        startActivity(i);

        // Not actually needed, just want to save it for a while...
        /*
        final int count = new Random().nextInt(40) + 5;
        if (!wordset.isActive) {
            wordset.isActive = true;
            database.updateWordSet(wordset);
        }

        if (!database.checkCanLearn()) {
            Toast.makeText(this, "You are too smart, baka!", Toast.LENGTH_SHORT).show();
        }
        else {
            int i;
            for (i = 0; i < count && database.checkCanLearn(); i++) {
                WordEntry word = database.getNextWordForLearning();
                if (word == null) {
                    break;
                }
                word.hits = wordset.repeatCount;
                database.updateWord(word);
            }
            if (i == 0)
                Toast.makeText(this, "You are too smart, baka!", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Using some mi" +
                    "ght and magic you learned " + Integer.toString(i)
                    + " words!", Toast.LENGTH_SHORT).show();
        }*/
    }

    public void onUninstallButtonClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.are_you_sure);
        builder.setMessage(R.string.uninstall_warning);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                database.deleteWordset(wordset.id);
                finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
}
