package com.edgerealities.sdk.example.CSFragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import com.edgerealities.sdk.example.R;

public class CSFragment extends Fragment {

    /**
     * Callback for handling key input.
     */
    public interface InputCallback{
        boolean onKeyDown(int keyCode, KeyEvent event);
    }

    public InputCallback inputCallback;

    /**
     * Flag for handling CSFragments that have extended lifetime.
     */
    public boolean isPersistent;
    protected Animation fadeInAnimation;
    protected Animation fadeOutAnimation;

    private static final String TAG = "CS_FRAGMENT";

    private static int counter = 0;
    private int id;
    public int listId;
    private Runnable scheduledForClosing = null;
    private static final Handler handler = new Handler();

    /**
     * Adds this CSFragment to provided layout.
     * @param activity current activity.
     * @param layoutId id of layout that fragment will be attached to.
     */
    public void open(Activity activity, int layoutId) {
        FragmentManager fragmentManager = activity.getFragmentManager();
        id = counter++;
        fragmentManager.beginTransaction().add(layoutId, this,TAG+id).commit();
        fragmentManager.executePendingTransactions();
    }

    public void scheduleForClosing(int closeInSeconds) {
        if (isClosing()) {
            Log.v("MAIN", "UI fragment "+ listId +" scheduled for close, but already closing. Doing nothing.");
            return;
        }
        if (closeInSeconds <= 0) {
            Log.v("MAIN", "UI fragment "+ listId +" timeout was 0. Closing immediately.");
            close();
        } else {
            Log.v("MAIN", "UI fragment "+ listId +" closing in "+closeInSeconds+" s.");
            scheduledForClosing = new Runnable() {
                @Override
                public void run() {
                    Log.v("MAIN", "UI fragment "+ listId +" closing now.");
                    scheduledForClosing = null;
                    close();
                }
            };
            handler.postDelayed(scheduledForClosing, closeInSeconds * 1000);
        }
    }

    public void abortClosing() {
        if (isClosing()) {
            handler.removeCallbacks(scheduledForClosing);
            scheduledForClosing = null;
        }
    }

    public boolean isClosing() {
        return scheduledForClosing != null;
    }

    /**
     * Removes this CSFragment from activity. It can be overridden to ensure CSFragment completing
     * all task before being detached from activity.
     */
    public void close() {
        Log.v("MAIN", "Close of CSFragment " + listId+ " called.");
        if(!isAdded()) {
            return;
        }
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        FragmentManager fragmentManager = activity.getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(TAG + id);
        if (fragment == null) {
            return;
        }
        fragmentManager.beginTransaction().remove(fragment).commit();
    }

    /**
     * Removes this CSFragment from activity immediately.
     */
    public void kill(){
        Log.v(TAG, "Kill of CSFragment " + listId + " called.");
        if(!isAdded()) {
            return;
        }
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        FragmentManager fragmentManager = activity.getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(TAG+id);
        if (fragment == null) {
            return;
        }
        ViewGroup layout = getActivity().findViewById(R.id.constraintlayout);
        if(layout != null) {
            for (int index = 0; index < layout.getChildCount(); ++index) {
                View nextChild = layout.getChildAt(index);
                if ((nextChild.getClass().getName().equalsIgnoreCase("android.support.v7.widget.AppCompatTextView") || nextChild.getClass().getName().equalsIgnoreCase("android.support.v7.widget.AppCompatImageView")) && nextChild != null) {
                    nextChild.setAlpha(0.f);
                } else if(nextChild == null) {
                    Log.d(TAG, "nextChild is null while kill");
                }
            }
        } else{
            Log.d(TAG, "layout is null while kill");
        }
        abortClosing();
        fragmentManager.beginTransaction().remove(fragment).commit();
        fragmentManager.executePendingTransactions();
    }
}