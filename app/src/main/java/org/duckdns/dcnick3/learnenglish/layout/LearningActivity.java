package org.duckdns.dcnick3.learnenglish.layout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.LineHeightSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

import org.duckdns.dcnick3.learnenglish.LearnHelper;
import org.duckdns.dcnick3.learnenglish.R;
import org.duckdns.dcnick3.learnenglish.SharedRes;
import org.duckdns.dcnick3.learnenglish.Util;
import org.duckdns.dcnick3.learnenglish.wordsets.WordEntry;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSet;
import org.duckdns.dcnick3.learnenglish.wordsets.WordSetDatabase;

import at.markushi.ui.CircleButton;

public class LearningActivity extends Activity {
    private SpringSystem springSystem;
    private Spring horizontalSpring;
    private Spring verticalSpring;
    private RelativeLayout rootView;
    private CardView card;
    private CircleButton showButton;
    private TextView wordText;
    private TextView wordsetTitleText;
    private View translationView;
    private TextView wordTranslationText;
    private TextView infoText;
    private UntouchableMarkdownView wordSamplesText;
    private CardTracker tracker;
    private boolean wasGone = false;
    private boolean translationVisible = false;
    private WordSetDatabase database;
    private LearnHelper learnHelper;
    private WordEntry currentWord;
    private SparseArray<WordSet> wordsetCache = new SparseArray<>();
    private GradientDrawable gradientRed;
    private GradientDrawable gradientGreen;
    private int backgroundCurrentAlpha;


    private static final double DROP_SOURCE = -10000.0;
    private static final double FLING_DESTINATION = 2;
    private static final double FLING_ZONE_SIZE = 0.85;
    private static final double FLING_SPEED_THRESHOLD = 4;

    private boolean doesViewContain(View view, float x, float y) {
        Rect rect = new Rect(); view.getGlobalVisibleRect(rect);
        return (x >= rect.left && x < rect.right) && (y >= rect.top && y < rect.bottom);
    }

    private WordSet getWordset(int id) {
        WordSet ws = wordsetCache.get(id);
        if (ws != null)
            return ws;
        else {
            ws = database.getWordset(id);
            wordsetCache.append(ws.id, ws);
            return ws;
        }
    }

    private static class SetLineOverlap implements LineHeightSpan {
        private int originalBottom = 15;        // init value ignored
        private int originalDescent = 13;       // init value ignored
        private Boolean overlap;                // saved state
        private Boolean overlapSaved = false;   // ensure saved values only happen once

        SetLineOverlap(Boolean overlap) {
            this.overlap = overlap;
        }

        @Override
        public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v,
                                 Paint.FontMetricsInt fm) {
            if (overlap) {
                if (!overlapSaved) {
                    originalBottom = fm.bottom;
                    originalDescent = fm.descent;
                    overlapSaved = true;
                }
                fm.bottom += fm.top;
                fm.descent += fm.top;
            } else {
                // restore saved values
                fm.bottom = originalBottom;
                fm.descent = originalDescent;
                overlapSaved = false;
            }
        }
    }

    private void setCardSubtext(String leftText, String rightText) {
        String fullText = leftText + "\n " + rightText;     // only works if  linefeed between them! "\n ";

        int fullTextLength = fullText.length();
        int leftEnd = leftText.length();
        int rightTextLength = rightText.length();

        final SpannableString s = new SpannableString(fullText);
        AlignmentSpan alignmentSpan = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE);
        s.setSpan(alignmentSpan, leftEnd, fullTextLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new SetLineOverlap(true), 1, fullTextLength-2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new SetLineOverlap(false), fullTextLength-1, fullTextLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        infoText.setText(s);
    }

    private void dropCard(WordEntry word) {
        currentWord = word;

        WordSet ws = getWordset(currentWord.wordset);
        wordText.setText(currentWord.word);
        wordsetTitleText.setText(ws.localizedName);
        wordTranslationText.setText(currentWord.translation);
        wordSamplesText.loadMarkdown(currentWord.samples, "file:///android_asset/md_theme.css");
        setCardSubtext(word.hits == 0 ? "New Word" : (word.hits + (word.hits == 1 ? " hit" : " hits")), "by DCNick3");

        translationVisible = false;

        translationView.setVisibility(View.INVISIBLE);
        showButton.setVisibility(View.VISIBLE);

        wasGone = false;
        horizontalSpring.setCurrentValue(0.0);
        verticalSpring.setCurrentValue(DROP_SOURCE);

        verticalSpring.setEndValue(0.0);
        horizontalSpring.setEndValue(0.0);

        tracker.resetFlung();
    }

    private void gone(boolean direction) {
        //direction = !direction;

        learnHelper.submitWord(currentWord, direction);

        Log.i("learning", currentWord.word + " <-> " + direction +"; hd: "
                + currentWord.lastHitDate + "; score: " + currentWord.hits);

        wasGone = true;
        WordEntry we = learnHelper.nextWord();
        if (we == null) {
            Toast.makeText(this, R.string.all_done, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            dropCard(we);
        }
    }

    private void showTranslation() {
        if (!translationVisible) {
            translationVisible = true;
            showButton.setAlpha(1f);
            showButton.animate().setDuration(200).alpha(0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    showButton.setVisibility(View.INVISIBLE);
                    showButton.setAlpha(1f);

                    translationView.setVisibility(View.VISIBLE);
                    translationView.setAlpha(0.0f);
                    translationView.animate().setDuration(200).alpha(1f);
                }
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = SharedRes.openDatabase();

        if (!database.checkCanLearn()) {
            Toast.makeText(this, getResources().getText(R.string.no_selected_wordsets), Toast.LENGTH_SHORT).show();
            onBackPressed();
            return;
        }

        setContentView(R.layout.activity_learning);

        rootView = findViewById(R.id.root);
        card = findViewById(R.id.card);
        showButton = findViewById(R.id.show_button);
        wordText = findViewById(R.id.word_text);
        wordsetTitleText = findViewById(R.id.wordset_title);
        translationView = findViewById(R.id.translation_view);
        wordTranslationText = findViewById(R.id.word_translation);
        wordSamplesText = findViewById(R.id.word_sample);
        infoText = findViewById(R.id.word_info);

        wordSamplesText.setEnabled(false);

        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTranslation();
            }
        });

        tracker = new CardTracker();
        card.setOnTouchListener(tracker);

        rootView.setBackground(genBackground(0));

        springSystem = SpringSystem.create();
        horizontalSpring = springSystem.createSpring();
        verticalSpring = springSystem.createSpring();
        SpringConfig config = new SpringConfig(80, 15);

        horizontalSpring.setSpringConfig(config);
        verticalSpring.setSpringConfig(config);

        horizontalSpring.addListener(new SimpleSpringListener()
        {
            @Override
            public void onSpringUpdate(Spring spring) {
                int x = (int)(spring.getCurrentValue());
                card.setTranslationX(x);
                if (!wasGone && (x > getWindow().getDecorView().getWidth()
                        || card.getWidth() + x < 0)) {

                    gone(tracker.flungDirection);
                }
            }
            /*
            @Override
            public void onSpringAtRest(Spring spring) {
                if (tracker.wasFlinged()) {
                    tracker.resetFlinged();
                    dropCard();
                }
            }*/
        });
        verticalSpring.addListener(new SimpleSpringListener()
        {
            @Override
            public void onSpringUpdate(Spring spring) {
                card.setTranslationY((int)spring.getCurrentValue());
            }
        });

        verticalSpring.setCurrentValue(DROP_SOURCE);
        verticalSpring.setEndValue(DROP_SOURCE);

        learnHelper = new LearnHelper(database, getPreferences(Context.MODE_PRIVATE));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dropCard(learnHelper.nextWord());
            }
        }, 500);

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int c = event.getKeyCode();
        if (c == KeyEvent.KEYCODE_DPAD_LEFT) {
            tracker.fling(false);
            return true;
        } else if (c == KeyEvent.KEYCODE_DPAD_RIGHT) {
            tracker.fling(true);
            return true;
        } else if (c == KeyEvent.KEYCODE_SPACE || c == KeyEvent.KEYCODE_ENTER) {
            showTranslation();
            return true;
        }
        else
            return super.dispatchKeyEvent(event);
    }

    // TODO: rewrite background updater using shaders (cuz drawable's ugly and slow)
    private void backgroundFadeOut() {
        final ValueAnimator an = ValueAnimator.ofInt(backgroundCurrentAlpha, 0);
        an.setDuration(200);
        an.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                updateBackground((int)animation.getAnimatedValue());
            }
        });
        an.start();
    }

    private void updateBackground(int alpha) {
        backgroundCurrentAlpha = alpha;

        //gradientRed.setGradientRadius(alpha / 255 * 0.45f * getWindow().getDecorView().getWidth());
        //gradientGreen.setGradientRadius(alpha / 255 * 0.45f * getWindow().getDecorView().getWidth());

        rootView.setBackground(genBackground(alpha));
    }

    private Drawable genBackground(int alpha) {
        backgroundCurrentAlpha = alpha;
        GradientDrawable gdBackground = new GradientDrawable();
        gdBackground.setColor(Color.parseColor("#fffafafa"));

        /*GradientDrawable gdColor = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {Color.argb(alpha, 0xb0, 0, 0x20),
                Color.argb(alpha, 0xff, 0xff, 0xff), Color.argb(alpha, 0x64, 0xdd, 0x17)});

        GradientDrawable gdMask = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[] {Color.parseColor("#fffafafa"),
                        Color.parseColor("#f0fafafa"), Color.parseColor("#00fafafa") });
*/
        gradientRed = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[] {Color.argb(alpha, 0xb0, 0, 0x20), Color.parseColor("#00fafafa")}
        );
        gradientRed.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gradientRed.setGradientCenter(0f, 1f);
        gradientRed.setGradientRadius(alpha / 255f * 0.45f * getWindow().getDecorView().getWidth());

        gradientGreen = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[] {Color.argb(alpha, 0x64, 0xdd, 0x17), Color.parseColor("#00fafafa")}
        );
        gradientGreen.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gradientGreen.setGradientCenter(1f, 1f);
        gradientGreen.setGradientRadius(alpha / 255f * 0.45f * getWindow().getDecorView().getWidth());

        return new LayerDrawable(new Drawable[]{ gdBackground, gradientRed, gradientGreen });
    }

    class CardTracker implements View.OnTouchListener {
        private float x0;
        private VelocityTracker velocity = null;
        private boolean flung;
        private boolean flungDirection;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (flung) {
                //Log.i("Learning", "REJECTED");
                return false;
            }

            float x = event.getRawX();
            if (event.getAction() != MotionEvent.ACTION_DOWN)
                x = x - x0;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (velocity == null) {
                        velocity = VelocityTracker.obtain();
                    } else {
                        velocity.clear();
                    }
                    x0 = x;
                    break;
                case MotionEvent.ACTION_MOVE:
                    velocity.addMovement(event);
                    horizontalSpring.setCurrentValue(x);
                    horizontalSpring.setEndValue(x);
                    float perc = Math.abs(x) / LearningActivity.this.getWindow().getDecorView().getWidth();
                    perc = 0.75f * (float)Math.sqrt(perc);
                    int alpha = Math.min(200, (int)(perc * 255));
                    updateBackground(alpha);
                    break;
                case MotionEvent.ACTION_UP:
                    velocity.addMovement(event);

                    velocity.computeCurrentVelocity(100);
                    float speed = Util.dpFromPx(LearningActivity.this, velocity.getXVelocity());
                    String text = "Speed: " + speed + "; x: " + x;
                    if (Math.abs(speed) > FLING_SPEED_THRESHOLD
                            || Math.abs(x) > card.getWidth() * FLING_ZONE_SIZE) {
                        text += "; gone " + (x > 0 ? "right" : "left");
                        fling(x > 0);
                    } else {
                        horizontalSpring.setEndValue(0);
                    }
                    backgroundFadeOut();
                    //Toast.makeText(LearningActivity.this, text, Toast.LENGTH_SHORT).show();
                    break;
            }
            return true;
        }

        public boolean wasFlung() {
            return flung;
        }

        public void resetFlung() {
            flung = false;
        }

        public boolean getFlungDirection() {
            return flungDirection;
        }

        public void fling(boolean direction) {
            if (!flung) {
                flungDirection = direction;
                flung = true;
                horizontalSpring.setEndValue((direction ? 1 : -1) * card.getWidth() * FLING_DESTINATION);
            }
        }
    }
}
