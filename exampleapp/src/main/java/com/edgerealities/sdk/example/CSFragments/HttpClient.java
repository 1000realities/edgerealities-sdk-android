package com.edgerealities.sdk.example.CSFragments;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Wrapper for OkHttpClient that runs on separate thread and enables single endpoint polling.
 *
 * Remember to release this object once you don't need it anymore.
 */
class HttpClient extends HandlerThread {

    private OkHttpClient httpClient;

    private Handler httpHandler;

    private Runnable polling;
    private volatile boolean quitPolling;
    private volatile boolean isPolling;
    private final Object LOCK = new Object();

    private static int instances = 0;

    private static final String TAG = "CS_HTTP_CLIENT";

    interface Callback {
        void onResponse(Response res);

        void onError(IOException ex);
    }

    HttpClient(){
        super("HTTP_THREAD_"+instances++);
        httpClient = new OkHttpClient();
        quitPolling = false;
        isPolling = false;
    }

    void init(){
        super.start();
        httpHandler = new Handler(getLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
    }

    void release(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            super.quitSafely();
        } else {
            stopPolling();
            if(!isAlive()) {
                Log.d(TAG, "poll: thread was dead");
                return;
            }
            httpHandler.post(new Runnable() {
                @Override
                public void run() {
                    quit();
                }
            });
        }
    }

    void poll(final Request req, final int interval, final Callback callback){
        stopPolling();
//        Log.d(TAG,"Starting polling");
        if(!isAlive()) {
            Log.d(TAG, "poll: thread was dead");
            return;
        }
        httpHandler.post(new Runnable() {
            @Override
            public void run() {
                isPolling = true;
            }
        });
        httpHandler.post(polling = new Runnable() {
            @Override
            public void run() {
                long timestamp = System.currentTimeMillis();
                Response res;
                try {
//                    Log.d(TAG, "Polling "+req.url());
                    res = httpClient.newCall(req).execute();
                } catch (IOException e) {
                    callback.onError(e);
                    return;
                }
                if (!quitPolling) {
                    callback.onResponse(res);
                    int queryTime = (int) (System.currentTimeMillis() - timestamp);
                    if(!currentThread().isAlive()){
                        Log.d(TAG, "poll: thread was dead");
                        return;
                    }
                    if (interval > queryTime) {
                        httpHandler.postDelayed(polling, interval - queryTime);
                    } else {
                        httpHandler.post(polling);
                    }
                } else {
                    isPolling = false;
                    synchronized (LOCK) {
                        quitPolling = false;
                    }
//                    Log.d(TAG,"Stopped polling");
                }
            }
        });
    }

    public void sendRequest(final Request req, final Callback callback){
        httpHandler.post(new Runnable() {
            @Override
            public void run() {
                Response res;
                try {
                    res = httpClient.newCall(req).execute();
                } catch (IOException e) {
                    callback.onError(e);
                    return;
                }
                callback.onResponse(res);
            }
        });
    }

    void stopPolling(){
//        Log.d(TAG,"Stopping polling");
        if(isPolling) {
            synchronized (LOCK) {
                quitPolling = true;
            }
        }
    }
}