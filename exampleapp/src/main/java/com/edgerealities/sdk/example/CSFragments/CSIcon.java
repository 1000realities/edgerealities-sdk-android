package com.edgerealities.sdk.example.CSFragments;

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

public class CSIcon extends CSFragment {

    private int resource;
    ImageView icon;
    private static final String TAG = "CS_Icon";

    public static CSFragment newInstance(int resource) {

        if (resource == -1) {
            return null;
        }

        CSIcon icon = new CSIcon();
        icon.resource = resource;
        return icon;
    }

    @Override
    public void close() {
        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                icon.setAlpha(0.f);
                CSIcon.super.close();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        icon.startAnimation(fadeOutAnimation);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.drawable_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        fadeInAnimation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fadein);
        fadeOutAnimation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fadeout);
        icon = getActivity().findViewById(R.id.img);
        icon.setImageResource(resource);
        icon.setAnimation(fadeInAnimation);
        icon.setAlpha(1.f);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void kill() {
        Log.d(TAG, "kill: " + icon.toString());
        if (icon != null) {
            icon.setAlpha(0.f);
        } else {
            Log.d(TAG, "icon null");
        }
        CSIcon.super.kill();
    }

}