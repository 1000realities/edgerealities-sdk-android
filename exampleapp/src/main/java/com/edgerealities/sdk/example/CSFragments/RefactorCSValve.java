package com.edgerealities.sdk.example.CSFragments;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import com.edgerealities.sdk.example.R;
import org.json.JSONObject;
import okhttp3.Response;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.IOException;
import okhttp3.Request;

/**
 * Created by michal on 14.11.18.
 */

@SuppressLint({"SetTextI18n","ShowToast"})
public class RefactorCSValve extends CSFragment {

    protected int valveValue = -1;
    protected TextView valveText;
    protected Bitmap resource;
    protected ImageView icon;

    protected ConstraintLayout constraintLayout;
    protected Handler iotHandler;
    protected PassedData data;
    protected Toast toast;

    protected HttpClient client;
    protected String pollUrl;
    protected String port;
    protected String ip;

    protected static final int UPDATE_UI_MSG = 0;
    protected static final int ERROR = -1;
    protected static final int INFO = -2;

    protected static final String TAG = "CS_R_Valve";

    public static <F extends RefactorCSValve> F buildInstance(F rawObject, HttpClient client, String iotIP, String iotPort, String pollUrl, PassedData passedData, Bitmap resource) {

        rawObject.client = client;
        rawObject.ip = iotIP;
        rawObject.port = iotPort;
        rawObject.pollUrl = pollUrl;
        rawObject.data = passedData;
        rawObject.resource = resource;

        return rawObject;
    }

    public static CSFragment newInstance(HttpClient client, String iotIP, String iotPort, PassedData passedData, Bitmap resource) {
        return buildInstance(new RefactorCSValve(), client, iotIP, iotPort, passedData.getValvePollUrl(), passedData, resource);
    }

    protected boolean handleMessage(Message msg) {
        if (msg.what == UPDATE_UI_MSG) {
            updateUI(valveValue);
            return true;
        } else {
            switch (msg.what) {
                case ERROR:
                    toast.setText(msg.obj.toString());
                    toast.getView().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                    toast.show();
                    return true;
                case INFO:
                    toast.setText(msg.obj.toString());
                    toast.getView().getBackground().setColorFilter(Color.CYAN, PorterDuff.Mode.SRC_IN);
                    toast.show();
                    return true;
            }
            return false;
        }
    }

    protected String buildPollingUrl() {
        return "http://" + ip + ":" + port + "/" + data.getValvePollUrl() + data.getValveId();
    }

    protected void updateDeviceState(Response res) {
        int val = -1;
        if (res != null) {
            try {
                JSONObject body = null;
                if (res.body() != null) {
                    body = new JSONObject(res.body().string());
                }
                if (body != null && body.has("value")) {
                    val = body.getInt("value");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                res.close();
            }
        }
        valveValue = val <= 100 ? val : 100;
    }

    protected void updateUI(int state) {
        if(state >= 0) {
            if(valveText != null) {
                valveText.setText("Otwarty: " + state + "%");
            }
        } else {
            if(valveText != null) {
                valveText.setText("Niedostepny");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.refactor_iot_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        constraintLayout = getActivity().findViewById(R.id.constraintlayout);
        valveText = getActivity().findViewById(R.id.status_text);
        icon = getActivity().findViewById(R.id.frame);
        fadeInAnimation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fadein);
        fadeOutAnimation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fadeout);

        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.setVerticalBias(valveText.getId(), data.getVerticalBias());
        set.setHorizontalBias(valveText.getId(), data.getHorizontalBias());
        set.applyTo(constraintLayout);

        valveText.setAnimation(fadeInAnimation);
        valveText.setAlpha(1.f);
        icon.setAnimation(fadeInAnimation);
        icon.setAlpha(1.f);

        icon.setImageBitmap(resource);
        updateUI(valveValue);
    }

    @Override
    public void close() {
        if (getActivity() == null) {
            return;
        }
        valveText.startAnimation(fadeOutAnimation);
        icon.startAnimation(fadeOutAnimation);
        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }
            @Override
            public void onAnimationEnd(Animation animation) {
                valveText.setAlpha(0.f);
                icon.setAlpha(0.f);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }


    @Override
    public void open(Activity activity, int layoutId) {
        toast = Toast.makeText(activity, "", Toast.LENGTH_LONG);
        ((TextView) toast.getView().findViewById(android.R.id.message)).setGravity(Gravity.CENTER);

        iotHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return RefactorCSValve.this.isAdded() && RefactorCSValve.this.handleMessage(msg);
            }
        });

        startPollingStatus();
        super.open(activity, layoutId);
    }

    protected void startPollingStatus() {
        String pollingUrl = buildPollingUrl();
        if (pollingUrl == null) {
            return;
        }
        try {
            Request req = new Request.Builder().url(pollingUrl).build();
            client.poll(req, 500, new HttpClient.Callback() {
                @Override
                public void onResponse(Response res) {
                    updateDevice(res.code() == 200 ? res : null);
                }

                @Override
                public void onError(IOException ex) {
                    updateDevice(null);
                    ex.printStackTrace();
                }
            });
        } catch (IllegalArgumentException e){
            e.printStackTrace();
            iotHandler.obtainMessage(ERROR, "Cannot get IoT data. Please configure your client.").sendToTarget();
        }
    }

    protected void stopPollingStatus() {
        client.stopPolling();
    }

    protected void updateDevice(Response res) {
        updateDeviceState(res);
        if(iotHandler != null) {
            iotHandler.obtainMessage(UPDATE_UI_MSG).sendToTarget();
        }
    }

    @Override
    public void onDestroy() {
        stopPollingStatus();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        stopPollingStatus();
        super.onStop();
    }

    @Override
    public void onPause() {
        stopPollingStatus();
        super.onPause();
    }
}
