package org.duckdns.dcnick3.learnenglish.layout;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class AnimHelper {
    public static void fadeOut(final View v, int duration) {
        AlphaAnimation anim = new AlphaAnimation(1,0);
        anim.setDuration(duration);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        v.setAnimation(anim);
    }

    public static void fadeIn(final View v, int duration) {
        v.setVisibility(View.VISIBLE);
        AlphaAnimation anim = new AlphaAnimation(0,1);
        anim.setDuration(duration);
        anim.setInterpolator(new AccelerateInterpolator());
        v.setAnimation(anim);
    }
}
