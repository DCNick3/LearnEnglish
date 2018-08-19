package org.duckdns.dcnick3.learnenglish.layout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.wordsets.WordEntry;

/**
 * Created by nikit_000 on 4/28/2018.
 */

public class WordListAdapter extends BaseAdapter {
    public WordListAdapter(Context context, WordEntry[] words)
    {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        array = words;
    }

    private LayoutInflater layoutInflater;
    private WordEntry[] array;
    private Context context;

    public void replaceArray(WordEntry[] array) {
        this.array = array;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return array.length;
    }

    @Override
    public Object getItem(int i) {
        return array[i];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = layoutInflater.inflate(R.layout.word_item, viewGroup, false);
        }
        TextView name = view.findViewById(R.id.word);
        TextView translation = view.findViewById(R.id.word_translation);
        ImageView learnedIcon = view.findViewById(R.id.word_learned_icon);

        WordEntry entry = array[i];
        name.setText(entry.word);
        translation.setText(entry.translation);
        if (entry.learned) {
            learnedIcon.setImageResource(R.drawable.ic_star_black_24dp);
        } else {
            learnedIcon.setImageDrawable(null);
        }

        return view;
    }
}
