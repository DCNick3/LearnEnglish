package org.duckdns.dcnick3.learnenglish.layout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.wordsets.remote.RemoteRepository;


class RemoteRepositoryListAdapter extends BaseAdapter {
    public RemoteRepositoryListAdapter(Context context, RemoteRepository[] repositories) {
        repos = repositories;
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    RemoteRepository[] repos;
    Context context;
    LayoutInflater inflater;

    @Override
    public int getCount() {
        return repos.length;
    }

    @Override
    public Object getItem(int position) {
        return repos[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = inflater.inflate(R.layout.remote_repository_item, parent, false);
        }

        TextView repoNameView = view.findViewById(R.id.repository_name);
        TextView repoUrlView = view.findViewById(R.id.repository_url);

        RemoteRepository item = repos[position];

        repoNameView.setText(item.localizedName);
        repoUrlView.setText(item.url.toString());

        return view;
    }
}
