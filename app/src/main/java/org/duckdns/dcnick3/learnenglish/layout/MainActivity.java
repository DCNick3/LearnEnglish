package org.duckdns.dcnick3.learnenglish.layout;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;
import android.widget.Toolbar;

import org.duckdns.dcnick3.learnenglish.CancellationTokenSource;
import org.duckdns.dcnick3.learnenglish.ProgressListener;
import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.SharedRes;
import org.duckdns.dcnick3.learnenglish.wordsets.IconManager;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSetDatabase;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSetPack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity implements GestureDetector.OnGestureListener {

    private CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

    private NavigationFragmentContainerHelper curremtPage = null;

    // TODO: rewrite fragmentMenu (it looks ugly now)
    private SparseArray<NavigationFragmentContainerHelper> fragmentMenuDict = new SparseArray<>();
    private int maxPagePosition;
    private GestureDetector gestureDetector;
    private BottomNavigationView navigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        fragmentMenuDict.append(R.id.navigation_vocabulary,
                new NavigationFragmentContainerHelper(new VocaburaryFragment(), 0));

        fragmentMenuDict.append(R.id.navigation_wordsets,
                new NavigationFragmentContainerHelper(new WordSetsFragment(),  1));

        fragmentMenuDict.append(R.id.navigation_stats,
                new NavigationFragmentContainerHelper(new StatisticsFragment(), 2));

        fragmentMenuDict.append(R.id.navigation_settings,
                new NavigationFragmentContainerHelper(new PrefsFragment(),  3));

        maxPagePosition = maxPage();

        setContentView(R.layout.activity_main);

        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener);

        selectPage(fragmentMenuDict.get(navigation.getSelectedItemId(), null));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);

        gestureDetector = new GestureDetector(this, this);

        SharedRes.initialize(getApplicationContext());

        WordSetDatabase db = SharedRes.openDatabase();
        IconManager iman = SharedRes.openIconManager();
        if (db.getWordSets().length == 0) {
            //installAssetPack("anime_pack.zip", db, iman);
            //installAssetPack("hhgttg.zip", db, iman);
            //installAssetPack("small_pack.zip", db, iman);
        }
    }

    private void selectPage(NavigationFragmentContainerHelper page) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        NavigationFragmentContainerHelper oldCurrentPage = curremtPage;
        curremtPage = page;

        if (curremtPage != null && oldCurrentPage != null)
        {
            if (curremtPage.position < oldCurrentPage.position)
                transaction.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
            else
                transaction.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right);
        }

        if (curremtPage != null)
        {
            transaction.replace(R.id.content, curremtPage.fragment);
        }
        else if (oldCurrentPage != null)
            transaction.remove(oldCurrentPage.fragment);
        transaction.commit();
    }

    private NavigationFragmentContainerHelper getPageAt(int position) {
        for (int i = 0; i < fragmentMenuDict.size(); i++) {
            if (fragmentMenuDict.valueAt(i).position == position)
                return fragmentMenuDict.valueAt(i);
        }
        return null;
    }

    private int maxPage() {
        int res = -1;
        for (int i = 0; i < fragmentMenuDict.size(); i++) {
            if (fragmentMenuDict.valueAt(i).position > res)
                res = fragmentMenuDict.valueAt(i).position;
        }
        return res;
    }

    private void pageMove(int delta) {
        if (curremtPage != null) {
            int current = curremtPage.position + delta;
            if (current < 0) current = 0;
            if (current > maxPagePosition) current = maxPagePosition;
            if (curremtPage.position != current)
                navigation.setSelectedItemId(fragmentMenuDict.keyAt(
                        fragmentMenuDict.indexOfValue(getPageAt(current))));
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            selectPage(fragmentMenuDict.get(item.getItemId(), null));
            return true;
        }

    };

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        return super.onPrepareOptionsMenu(menu);
    }

    // TODO: remove pack-install related stuff from here
    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private void installAssetPack(final String name, WordSetDatabase db, IconManager iman) {
        File tmpFile = null;
        InputStream assetis = null;
        FileOutputStream os = null;
        try {
            assetis = getAssets().open(name);
            tmpFile = File.createTempFile("pack", null, this.getCacheDir());
            os = new FileOutputStream(tmpFile);
            copyStream(assetis, os);
            os.close();
            //assetis.close();

            final WordSetPack pack = new WordSetPack(tmpFile);
            boolean res = pack.readInto(new ProgressListener() {
                @Override
                public void onProgress(int done, int total) {
                    Log.i("InitialPackInstall", "Installation of " + name + ": " + done + "/" + total);
                    //Toast.makeText(MainActivity.this, done + "/" + total, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void finished(int total, long time) {
                    Toast.makeText(MainActivity.this, "Installation of " + name + " ("
                            + total + " words) finished in " + time + " ms", Toast.LENGTH_LONG).show();
                }
            }, db, iman, cancellationTokenSource.getToken(), "local");
            if (!res) {
                Toast.makeText(MainActivity.this, "Installation of " + name + " failed", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, name + " install exception: " + e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        selectPage(fragmentMenuDict.get(navigation.getSelectedItemId(), null));
    }

    /* Gesture stuff */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }
    @Override
    public void onShowPress(MotionEvent e) {
    }
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }
    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (Math.abs(velocityX) > 300 && Math.abs(velocityX) > Math.abs(velocityY)) {
            if (velocityX > 0) {
                pageMove(-1);
            }
            else {
                pageMove(1);
            }
            return true;
        }
        return false;
    }
}

class NavigationFragmentContainerHelper
{
    NavigationFragmentContainerHelper(Fragment fragment, int position)
    {
        this.fragment = fragment;
        this.position = position;
    }

    Fragment fragment;
    int position;
}
