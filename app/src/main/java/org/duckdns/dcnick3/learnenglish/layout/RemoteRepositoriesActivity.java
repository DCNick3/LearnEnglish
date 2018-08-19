package org.duckdns.dcnick3.learnenglish.layout;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.SharedRes;
import org.duckdns.dcnick3.learnenglish.wordsets.remote.RemoteRepository;
import org.duckdns.dcnick3.learnenglish.wordsets.remote.RemoteWordpackManager;
import org.markdownj.TextEditor;
import org.w3c.dom.Text;

import java.util.Objects;

import bolts.Continuation;
import bolts.Task;

public class RemoteRepositoriesActivity extends Activity {

    RemoteWordpackManager rwman;
    ListView listView;
    RemoteRepositoryListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_remote_repositories);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);
        Objects.requireNonNull(getActionBar()).setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);

        rwman = SharedRes.openWordpackManager();

        listView = findViewById(R.id.repository_list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.performLongClick();
            }
        });
        updateList();

        registerForContextMenu(listView);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.remote_repositories_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_repository:
                showAddRepositoryDialog();
                return true;
            default:
                return false;
        }

    }

    /*
     * Sorry for this monstrous method, I just didn't come up with something more smart than this
     */
    private void showAddRepositoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_repo);
        builder.setMessage(R.string.enter_repo_url);

        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.dialog_add_repository, null);
        builder.setView(view);

        final EditText urlView = view.findViewById(R.id.repository_url);
        final ProgressBar progressView = view.findViewById(R.id.progress_view);
        urlView.setVisibility(View.VISIBLE);
        progressView.setVisibility(View.GONE);

        builder.setPositiveButton(R.string.OK, null);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setCancelable(false);

        final AlertDialog dialog = builder.create();

        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.GONE);
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
                urlView.setVisibility(View.GONE);
                /*
                View view = dialog.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }*/

                progressView.setVisibility(View.VISIBLE);
                dialog.setTitle(R.string.please_wait);
                dialog.setMessage(getString(R.string.validating_repository));

                rwman.tryAddRepositoryAsync(urlView.getText().toString()).continueWith(new Continuation<RemoteWordpackManager.InstallResult, Void>() {
                    @Override
                    public Void then(final Task<RemoteWordpackManager.InstallResult> task) throws Exception {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (task.isFaulted() || task.getResult() != RemoteWordpackManager.InstallResult.OK) {
                                    dialog.dismiss();
                                    String text = task.isFaulted() ? getString(R.string.unknown_error) :
                                            task.getResult().toString(RemoteRepositoriesActivity.this);
                                    Toast.makeText(RemoteRepositoriesActivity.this, text, Toast.LENGTH_SHORT).show();
                                    showAddRepositoryDialog();
                                } else {
                                    dialog.dismiss();
                                    updateList();
                                }
                            }
                        });
                        return null;
                    }
                });
            }
        });
    }


    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return true;
    }

    private void updateList() {
        adapter = new RemoteRepositoryListAdapter(this, rwman.getRepos());
        listView.setAdapter(adapter);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.repository, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        RemoteRepository repo = (RemoteRepository)adapter.getItem(
                ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.menu_repo_delete:
                rwman.removeRepository(repo);
                updateList();
                return true;
            case R.id.menu_repo_copy_url:
                ((ClipboardManager)Objects.requireNonNull(getSystemService(CLIPBOARD_SERVICE)))
                        .setPrimaryClip(ClipData.newPlainText("repository_url", repo.url.toString()));
                return true;
            default:
                return false;
        }
    }

    public void onAddClick(View view) {
        showAddRepositoryDialog();
    }
}
