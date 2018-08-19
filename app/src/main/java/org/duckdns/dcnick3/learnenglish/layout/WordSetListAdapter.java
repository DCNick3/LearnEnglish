package org.duckdns.dcnick3.learnenglish.layout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.wordsets.IconManager;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSet;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSetDatabase;

import cn.refactor.library.SmoothCheckBox;

/**
 * Created by nikit_000 on 2/5/2018.
 */

public class WordSetListAdapter extends BaseAdapter {
    public WordSetListAdapter(Context c, IconManager icons, WordSet[] baseList,
                              boolean showCheckboxes, WordSetDatabase database) {
        sets = baseList;
        mContext = c;
        iman = icons;
        this.showCheckboxes = showCheckboxes;
        this.database = database;
    }
    private Context mContext;
    private IconManager iman;
    private WordSet[] sets;
    private boolean showCheckboxes;
    private WordSetDatabase database;


    public boolean isShowCheckboxes() {
        return showCheckboxes;
    }

    public void setShowCheckboxes(boolean showCheckboxes) {
        this.showCheckboxes = showCheckboxes;
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return sets.length;
    }

    @Override
    public Object getItem(int i) {
        return sets[i];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        final WordSet item = sets[i];
        final View tv;
        if (view == null)
            tv = LayoutInflater.from(mContext).inflate(R.layout.wordset_item, viewGroup, false);
        else
            tv = view;

        TextView nameView = tv.findViewById(R.id.wordset_name);
        ImageView iconView = tv.findViewById(R.id.wordset_icon);
        final SmoothCheckBox checkBox = tv.findViewById(R.id.wordset_checkbox);
        ProgressBar progress = tv.findViewById(R.id.wordset_progress);
        TextView progressText = tv.findViewById(R.id.wordset_progress_text);


        nameView.setText(item.localizedName);
        Bitmap bmp = iman.loadIcon(item.iconHash);
        Drawable icn = bmp != null ? new BitmapDrawable(mContext.getResources(), bmp) : null;

        if (!showCheckboxes) {
            if (checkBox.getVisibility() != View.INVISIBLE)
                AnimHelper.fadeOut(checkBox, 200);
        }
        else {
            if (checkBox.getVisibility() != View.VISIBLE)
                AnimHelper.fadeIn(checkBox, 200);
            if (checkBox.isChecked() != item.isActive) {
                checkBox.setChecked(item.isActive, true);
            }
        }



        int newColor = item.isActive ? ContextCompat.getColor(mContext, R.color.colorSelectedTransparent) : Color.TRANSPARENT;
        ColorDrawable bg = (ColorDrawable)tv.getBackground();
        if (bg != null) {
            int oldColor = bg.getColor();

            if (newColor != oldColor) {
                ValueAnimator anim = ValueAnimator.ofArgb(oldColor, newColor);
                anim.setDuration(250);
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        tv.setBackgroundColor((int) animation.getAnimatedValue());
                    }
                });
                anim.start();
            }
        } else {
            tv.setBackgroundColor(newColor);
        }

        Pair<Integer, Integer> wprogress = database.getWordsetProgress(item.id);
        progress.setProgress((int)((double)wprogress.first / wprogress.second * 100));
        progressText.setText(wprogress.first.toString() + "/" + wprogress.second.toString());

        checkBox.setOnCheckedChangeListener(new SmoothCheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SmoothCheckBox smoothCheckBox, boolean b) {
                item.isActive = b;
                notifyDataSetChanged();
            }
        });

        if (icn == null)
            icn = mContext.getResources().getDrawable(R.drawable.ic_learnenglish_plain, null);

        iconView.setImageDrawable(icn);
        return tv;
    }
}
