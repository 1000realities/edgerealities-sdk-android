package com.edgerealities.sdk.example.CSFragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.edgerealities.sdk.example.R;

import java.io.IOException;

import pl.droidsonroids.gif.AnimationListener;
import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by michal on 23.11.18.
 */

public class CSGif extends CSFragment {

    private int resource;

    private boolean shouldClose;
    private ImageView frame;
    private GifDrawable gif;

    private int loops;

    private int currentLoop;

    private static final String TAG = "CS_GIF";

    public static CSFragment newInstance(int resource) {
        CSGif gif = new CSGif();
        gif.resource = resource;
        return gif;
    }

    public static CSFragment newInstance(int resource, int loops) {
        CSGif gif = (CSGif) newInstance(resource);
        gif.loops = loops;
        return gif;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.drawable_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        fadeInAnimation = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.fadein);
        fadeOutAnimation = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.fadeout);
        frame = activity.findViewById(R.id.img);
        try {
            gif = new GifDrawable(getResources(), resource);
            gif.setLoopCount(loops);
            gif.addAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationCompleted(int loopNumber) {
                    if (loops != 0 && ++currentLoop == loops) {
                        gif.pause();
                        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                frame.setAlpha(0.f);
                                CSGif.super.close();
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                        frame.startAnimation(fadeOutAnimation);
                    }
                }
            });
            frame.setImageDrawable(gif);
            frame.startAnimation(fadeInAnimation);
            frame.setAlpha(1.f);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            Log.d(TAG, "close: fragment was not attached");
            e.printStackTrace();
        }
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void close() {
        gif.pause();
        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                frame.setAlpha(0.f);
                CSGif.super.close();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        frame.startAnimation(fadeOutAnimation);
    }

    @Override
    public void kill() {
        if (gif != null) {
            gif.pause();
            gif.setAlpha(0);
        } else {
            Log.d(TAG, "gif null");
        }
        if (frame != null) {
            frame.setAlpha(0.f);
        } else {
            Log.d(TAG, "frame null");
        }
        CSGif.super.kill();
    }

}