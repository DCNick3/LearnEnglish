package org.duckdns.dcnick3.learnenglish.layout;

import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.FloatingActionButton;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;

import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.SharedRes;
import org.duckdns.dcnick3.learnenglish.wordsets.IconManager;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSet;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSetDatabase;

import java.io.File;

public class WordSetsFragment extends Fragment {
    public WordSetsFragment() {
    }

    private WordSet[] words;
    private IconManager iman;
    private WordSetListAdapter adapter;
    private boolean isSelectionMode;
    private WordSetDatabase database;
    GridView gridview;
    FloatingActionButton doneFab;
    LinearLayout nothingIsHereMessage;

    public static WordSetsFragment newInstance() {
        WordSetsFragment fragment = new WordSetsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.wordsets, menu);
        if (isSelectionMode)
            menu.removeItem(R.id.edit_active);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.install_wordsets:
                installWordsets();
                return true;
            case R.id.edit_active:
                enterSelectionMode();
                return true;
            default:
                return false;
        }
    }

    private void installWordsets() {
        Intent i = new Intent(getActivity(), RemoteWordsetsActivity.class);
        startActivity(i);
    }

    private void updateData() {
        words = database.getWordSets();
        gridview.setAdapter(adapter = new WordSetListAdapter(getActivity(), iman, words, isSelectionMode, database));
        if (words.length == 0) {
            nothingIsHereMessage.setVisibility(View.VISIBLE);
        } else {
            nothingIsHereMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        database = SharedRes.openDatabase();


        View v = inflater.inflate(R.layout.fragment_word_sets, container, false);

        nothingIsHereMessage = v.findViewById(R.id.nothing_is_here_message);
        nothingIsHereMessage.setVisibility(View.GONE);

        doneFab = v.findViewById(R.id.done_fab);
        doneFab.setVisibility(View.GONE);
        doneFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSelectionMode(false);
            }
        });

        v.findViewById(R.id.install_wordsets).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installWordsets();
            }
        });

        gridview = v.findViewById(R.id.wordsets_grid_view);
        iman = new IconManager(getActivity(), new File(getActivity().getFilesDir(), "database/icons"));
        updateData();

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                WordSet item = (WordSet) adapterView.getItemAtPosition(i);

                if (isSelectionMode) {
                    item.isActive = !item.isActive;
                    adapter.notifyDataSetChanged();
                } else {
                    Intent intent = new Intent(getActivity(), WordSetActivity.class);
                    intent.putExtra("wordset", item.id);
                    startActivity(intent);
                }
            }
        });

        gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (!isSelectionMode) {
                    enterSelectionMode();
                    return true;
                }
                return false;
            }
        });

        v.setFocusableInTouchMode(true);
        v.requestFocus();
        v.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(isSelectionMode && keyCode == KeyEvent.KEYCODE_BACK ) {
                    exitSelectionMode(false);
                    return true;
                }
                return false;
            }
        });

        return v;
    }

    private void exitSelectionMode(boolean exiting) {
        isSelectionMode = false;
        if (!exiting) {
            adapter.setShowCheckboxes(false);
            getActivity().invalidateOptionsMenu();
            AnimHelper.fadeOut(doneFab, 200);
        }
        for (WordSet set : words) {
            database.updateWordSet(set);
        }
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        adapter.setShowCheckboxes(true);
        getActivity().invalidateOptionsMenu();
        AnimHelper.fadeIn(doneFab, 200);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
    }

    @Override
    public void onPause() {
        exitSelectionMode(true);
        super.onPause();
    }

    @Override
    public void onDetach() {
        exitSelectionMode(true);
        super.onDetach();
    }
}
