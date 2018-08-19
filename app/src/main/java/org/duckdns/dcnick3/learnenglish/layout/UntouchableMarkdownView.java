package org.duckdns.dcnick3.learnenglish.layout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import us.feras.mdv.MarkdownView;

public class UntouchableMarkdownView extends MarkdownView {

    public UntouchableMarkdownView(Context context) {
        super(context);
    }

    public UntouchableMarkdownView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
