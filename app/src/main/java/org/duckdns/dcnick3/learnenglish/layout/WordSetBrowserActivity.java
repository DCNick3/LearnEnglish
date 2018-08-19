package org.duckdns.dcnick3.learnenglish.layout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ListView;
import android.widget.SearchView;

import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.SharedRes;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSetDatabase;

/**
 * Created by nikit_000 on 4/28/2018.
 */

public class WordSetBrowserActivity extends Activity implements SearchView.OnQueryTextListener {

    //WordSet wordset;
    SearchView searchView;
    ListView listView;
    WordSetDatabase db;
    int wordset;

    WordListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wordset_browser);

        searchView = findViewById(R.id.word_search);
        listView = findViewById(R.id.word_list);

        searchView.setOnQueryTextListener(this);

        db = SharedRes.openDatabase();

        Intent i = getIntent();
        wordset = i.getIntExtra("wordset", -1);
        adapter = new WordListAdapter(this, db.searchWords(wordset, "", null));

        listView.setAdapter(adapter);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        adapter.replaceArray(db.searchWords(wordset, newText, null));

        return true;
    }
}
