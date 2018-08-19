package org.duckdns.dcnick3.learnenglish.layout;

import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.github.rahatarmanahmed.cpv.CircularProgressViewListener;

import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.wordsets.IconManager;
import org.duckdns.dcnick3.learnenglish.wordsets.remote.RemoteWordpack;
import org.duckdns.dcnick3.learnenglish.wordsets.remote.RemoteWordpackInstaller;

import at.markushi.ui.CircleButton;

public class RemoteWordsetListAdapter extends BaseAdapter {
    RemoteWordsetListAdapter(RemoteWordsetsActivity remoteWordsetsActivity, IconManager iconManager, RemoteWordpack[] wordSets) {
        this.wordSets = wordSets;
        this.remoteWordsetsActivity = remoteWordsetsActivity;
        inflater = LayoutInflater.from(remoteWordsetsActivity);
        iman = iconManager;
    }

    private RemoteWordpack[] wordSets;
    private RemoteWordsetsActivity remoteWordsetsActivity;
    private LayoutInflater inflater;
    private IconManager iman;

    @Override
    public int getCount() {
        return wordSets.length;
    }

    @Override
    public Object getItem(int position) {
        return wordSets[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = inflater.inflate(R.layout.remote_wordset_item, parent, false);
        }
        ImageView iconView = view.findViewById(R.id.wordset_icon);
        TextView titleView = view.findViewById(R.id.wordset_title);
        TextView wordCountView = view.findViewById(R.id.wordset_size);
        TextView repoView = view.findViewById(R.id.wordset_repo);
        final CircleButton addButton = view.findViewById(R.id.add_button);
        final CircularProgressView progressBar = view.findViewById(R.id.wordset_install_progress);

        final RemoteWordpack item = wordSets[position];

        iconView.setImageBitmap(iman.loadIcon(item.iconHash));
        titleView.setText(item.localizedName);
        repoView.setText(item.repository.localizedName);
        if (item.wordCount != null) {
            wordCountView.setVisibility(View.VISIBLE);
            wordCountView.setText(String.format(remoteWordsetsActivity.getString(R.string.wordset_size_format), item.wordCount));
        } else {
            wordCountView.setVisibility(View.GONE);
        }

        final View.OnClickListener cancelClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) { remoteWordsetsActivity.cancelInstallation(item); }
        };
        RemoteWordpackInstaller installer = item.installer;
        if (installer != null) {
            addButton.setEnabled(true);
            addButton.setImageResource(R.drawable.ic_cancel_gray_32dp);;
            addButton.setOnClickListener(cancelClickListener);
            progressBar.setVisibility(View.VISIBLE);
            Pair<Integer, Integer> progress = installer.getProgress();
            float c = progress.first;
            float t = progress.second;

            if (c < t) {
                if (progressBar.isIndeterminate()) {
                    progressBar.setIndeterminate(false);
                    progressBar.resetAnimation();
                }
                if (progressBar.getMaxProgress() != t)
                    progressBar.setMaxProgress(t);
                progressBar.setProgress(c);
            } else {
                progressBar.setIndeterminate(true);
                progressBar.resetAnimation();
            }
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            if (item.installed) {
                addButton.setEnabled(false);
                addButton.setImageResource(R.drawable.ic_check_gray_32dp);
                addButton.setOnClickListener(null);
            } else {
                addButton.setEnabled(true);
                addButton.setImageResource(R.drawable.ic_add_gray_32dp);
                addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        remoteWordsetsActivity.installWordpack(item);
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setIndeterminate(true);
                        progressBar.startAnimation();
                        addButton.setEnabled(false);
                        addButton.setImageResource(R.drawable.ic_cancel_gray_32dp);
                        addButton.setOnClickListener(cancelClickListener);
                    }
                });
            }
        }

        return view;
    }
}
