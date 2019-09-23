package com.edgerealities.sdk.example;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.hardware.camera2.CameraAccessException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.edgerealities.sdk.example.CSFragments.CSFragment;
import com.edgerealities.sdk.example.CSFragments.CSFragmentFactory;
import com.edgerealities.sdk.example.CSFragments.PassedData;
import com.r1k.cloudslam.sdk.CSCameraException;
import com.r1k.cloudslam.sdk.CSConfig;
import com.r1k.cloudslam.sdk.CSEncoderException;
import com.r1k.cloudslam.sdk.CSException;
import com.r1k.cloudslam.sdk.CSNetworkException;
import com.r1k.cloudslam.sdk.CloudSLAM;
import com.r1k.cloudslam.sdk.UserPose;

import com.r1k.cloudslam.shapes.ShapeStorage;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private CloudSLAM cloudSlam;
    private CSConfig cloudSlamConfig;
    private CloudSLAM.Callback cloudSlamCallback;

    private SurfaceView cloudSlamSurfaceView;
    private SurfaceHolder cloudsSlamSurfaceViewHolder;

    private static Handler uiThreadHandler;

    private Snackbar connectingSnackbar;

    private static boolean permissionsGranted;

    private static volatile boolean readyToRun;

    private static final int M300_SELECT_BUTTON = 66;
    private static final int M300_BACK_BUTTON = 21;
    private static final int M300_FORWARD_BUTTON = 22;
    private static final int M300_TOUCH = 23;
    private static final int BLADE_FORWARD_SWIPE = 22;
    private static final int BLADE_BACK_SWIPE = 21;
    private static final int BLADE_TWO_FINGERS_FORWARD_SWIPE = 112;
    private static final int BLADE_TWO_FINGERS_BACK_SWIPE = 67;
    private static final int BLADE_TWO_FINGERS_TAP = 4;

    private static final int NOT_CONNECTED = 1;
    private static final int LOST = 2;
    private static final int TRACKING = 3;

    private static final int CONNECTED = 2;
    private static final int PENDING = 3;

    private static final int ERROR = 0;
    private static final int INFO = 1;
    private static final int CS_CALLBACK_OPEN = 2;
    private static final int CS_CALLBACK_STATUS_CHANGED = 3;
    private static final int CS_CALLBACK_ID_CHANGED = 4;
    private static final int CS_CALLBACK_ERROR = 5;
    private static final int CS_CALLBACK_CLOSE = 6;
    private static final int CS_CALLBACK_CAMERA_POSE_UPDATE = 7;
    private static final int CS_CALLBACK_RUN_QR_SCAN = 8;
    private static final int CS_CALLBACK_RUN_CLOUDSLAM = 9;

    private static final int SETTINGS_REQUEST_CODE = 1;
    private static final int QR_CONFIG_REQUEST_CODE = 2;

    private static final int BLADE_KEY_TIMEOUT_MS = 1500;
    private static long bladeLastKeyTime;

    private static final String TAG = "CS_MAIN";

    private Toast toast;

    private CSFragmentFactory csFactory;
    private CSFragment currentCSFragment;
    private int currentId;

    private PassedData passedData;

    private WebSocketClient webSocketToAdminPanel;
    private ShapeStorage mShapeStorage;
    private int lastIconId;

    private boolean shouldStartQrConfiguration = false;
    private boolean shouldStartCloudSlam = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);

        String model = null;
        switch(Build.MODEL) {
            case "Blade": {
                model = "bl";
                break;
            }
            case "M300": {
                model = "mt";
                break;
            }
        }

        if(model != null) {
            getResources().getConfiguration().locale = new Locale(model);
            getResources().updateConfiguration(getResources().getConfiguration(), getResources().getDisplayMetrics());
        }

        if (Build.MODEL.equals("ZEISS")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.video_stream);

        switch(Build.MODEL) {
            case "Blade": {
                ((TextView) findViewById(R.id.connectMessage)).setText(R.string.BladeConnectMessage);
                break;
            }
            case "M300": {
                ((TextView) findViewById(R.id.connectMessage)).setText(R.string.M300ConnectMessage);
                break;
            }
            case "T1100G": {
                ((TextView) findViewById(R.id.connectMessage)).setText(R.string.HMT1ConnectMessage);
                break;
            }
        }

        cloudSlam = new CloudSLAM(this);
        cloudSlamSurfaceView = findViewById(R.id.CloudSlamView);

        connectingSnackbar = Snackbar.make(cloudSlamSurfaceView, "Connecting ...", Snackbar.LENGTH_INDEFINITE);

        toast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        ((TextView) toast.getView().findViewById(android.R.id.message)).setGravity(Gravity.CENTER);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        readyToRun = true;
        if(shouldStartQrConfiguration) {
            shouldStartCloudSlam = false;
            shouldStartQrConfiguration = false;
            uiThreadHandler.obtainMessage(CS_CALLBACK_RUN_QR_SCAN).sendToTarget();
        }
        if(shouldStartCloudSlam) {
            shouldStartCloudSlam = false;
            shouldStartQrConfiguration = false;
            uiThreadHandler.obtainMessage(CS_CALLBACK_RUN_CLOUDSLAM).sendToTarget();
        }

        permissionsGranted = true;

        requestPermissionsIfNotGranted();

        setSurfaceViewScaleFromCameraOrientation();

        Settings.load(this);

        passedData = new PassedData();
        passedData.setPollUrls(
                getResources().getString(R.string.ValvePollValve)
        );

        (csFactory = new CSFragmentFactory(Settings.iotIP, Settings.iotPort, getApplicationContext(), passedData)).open();

        cloudSlamCallback = buildCloudSLAMCallback();

        uiThreadHandler = buildUIThreadHandler();

        try {
            cloudSlamConfig = CSConfig.load(this);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        mShapeStorage = new ShapeStorage();

        if (Build.MODEL.equals("M300") || Build.MODEL.equals("Blade")) {
            ((FloatingActionButton) findViewById(R.id.Stream)).hide();
            ((FloatingActionButton) findViewById(R.id.Settings)).hide();
        }

        if (Build.MODEL.equals("Blade")) {
            findViewById(R.id.connection).setLayoutParams(new ConstraintLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.connection_icon_size_blades),
                    (int) getResources().getDimension(R.dimen.connection_icon_size_blades)
            ));
            ConstraintSet cs = new ConstraintSet();
            cs.clone((ConstraintLayout) findViewById(R.id.stream_layout));
            cs.clear(R.id.connection, ConstraintSet.START);
            cs.connect(R.id.connection, ConstraintSet.END, R.id.stream_layout, ConstraintSet.END, 30);
            cs.clear(R.id.connection, ConstraintSet.TOP);
            cs.connect(R.id.connection, ConstraintSet.BOTTOM, R.id.stream_layout, ConstraintSet.BOTTOM, 15);
            cs.applyTo((ConstraintLayout) findViewById(R.id.stream_layout));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.v("PERM", Integer.toString(grantResults.length));
        if (requestCode == 1) {
            permissionsGranted = CloudSLAM.checkPermissions(permissions, grantResults, uiThreadHandler, INFO);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        Log.d("CS_KEY_EVENT", "Swipe connect");

        Log.d("KEY_EVENT", keyCode + " " + event.toString());

        if(keyCode == BLADE_TWO_FINGERS_TAP && cloudSlam.isRunning()) {

            Log.d("CS_KEY_EVENT", "While swipe connect is running or two fingers tap");

            return true;
        }

        if (currentCSFragment != null && currentCSFragment.inputCallback != null ) {
            currentCSFragment.inputCallback.onKeyDown(keyCode, event);
            if (Build.MODEL.equals("Blade") || Build.MODEL.equals("M300")) {
                return super.onKeyDown(keyCode, event);
            }
        }

        if (Build.MODEL.equals("M300") && event.getFlags() != 0x400) {
            if (keyCode == M300_FORWARD_BUTTON) {
                if (!cloudSlam.isRunning()) {
                    connectClient();
                } else {
                    closeClient();
                }
            }
            if (keyCode == M300_BACK_BUTTON) {
                View tmpView = new View(this);
                ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(1, 1);
                lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                ((ConstraintLayout) findViewById(R.id.stream_layout)).addView(tmpView, lp);
                onSettingsClick(tmpView);
            }
        }
        if (Build.MODEL.equals("Blade")) {
            if(System.currentTimeMillis() - bladeLastKeyTime > BLADE_KEY_TIMEOUT_MS){
                bladeLastKeyTime = System.currentTimeMillis();
            } else {
                Log.d(TAG, "onKeyDown: blade key timeout exceeded");
                return false;
            }
            if (keyCode == BLADE_TWO_FINGERS_BACK_SWIPE) {
                if (!cloudSlam.isRunning()) {
                    connectClient();
                } else {
                    closeClient();
                }
            }
            if (keyCode == BLADE_TWO_FINGERS_FORWARD_SWIPE) {
                View tmpView = new View(this);
                ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(1, 1);
                lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                ((ConstraintLayout) findViewById(R.id.stream_layout)).addView(tmpView, lp);
                onSettingsClick(tmpView);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onStreamClick(View view) {
        if (!cloudSlam.isRunning()) {
            connectClient();
        } else {
            closeClient();
        }
    }

    public void onSettingsClick(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.connection_settings:
                        if (cloudSlam.isRunning()) {
                            closeClient();
                        }
                        Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                        settingsIntent.putExtra("CONFIG", cloudSlamConfig);
                        startActivityForResult(settingsIntent, SETTINGS_REQUEST_CODE);
                        return true;
                    case R.id.qr_config:
                        if (cloudSlam.isRunning()) {
                            closeClient();
                        }
                        if (!readyToRun) {
                            shouldStartCloudSlam = false;
                            shouldStartQrConfiguration = true;
                        } else {
                            uiThreadHandler.obtainMessage(CS_CALLBACK_RUN_QR_SCAN).sendToTarget();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, popupMenu.getMenu());
        popupMenu.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK && (requestCode == SETTINGS_REQUEST_CODE || requestCode == QR_CONFIG_REQUEST_CODE)) {

            cloudSlamConfig = (CSConfig) data.getSerializableExtra("CONFIG");

            try {
                CSConfig.save(cloudSlamConfig, this);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Could not save configuration file");
                uiThreadHandler.obtainMessage(ERROR, "Could not save configuration file").sendToTarget();
            }

            (csFactory = new CSFragmentFactory(Settings.iotIP, Settings.iotPort, getApplicationContext(), passedData)).open();

            if (Settings.performanceDataOn) {
                findViewById(R.id.performanceText).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.performanceText).setVisibility(View.GONE);
            }
        }
    }

    public void connectClient() {

        Log.d(TAG, "connectClient");

        synchronized (this) {

            if (cloudSlamConfig == null) {
                uiThreadHandler.obtainMessage(ERROR, "Could not find configuration, please configure your client.").sendToTarget();
                return;
            }

            if (isConnectedOnDifferentLocalWifi(cloudSlamConfig.ip)) {
                uiThreadHandler.obtainMessage(ERROR, "You are not connected to the same Wi-fi as the server!").sendToTarget();
                return;
            }

            if (!permissionsGranted) {
                uiThreadHandler.obtainMessage(ERROR, "You have not granted appropriate permissions for this app!").sendToTarget();
                return;
            }

            if(!readyToRun) {
                shouldStartCloudSlam = true;
                shouldStartQrConfiguration = false;
            } else {
                uiThreadHandler.obtainMessage(CS_CALLBACK_RUN_CLOUDSLAM).sendToTarget();
            }
        }
    }

    public void closeClient() {

        Log.d(TAG, "SLAM closeClient");

        cloudSlam.stop();
        if(webSocketToAdminPanel != null) {
            webSocketToAdminPanel.close();
            webSocketToAdminPanel = null;
        }
        resetUI();
        if(Settings.show_preview) {
            findViewById(R.id.blackout).setAlpha(1f);
        }
        findViewById(R.id.connectMessage).setAlpha(1f);
    }

    @Override
    protected void onResume() {

        Log.d(TAG, "SLAM onResume");

        super.onResume();
    }

    @Override
    protected void onStop() {

        Log.d(TAG, "SLAM onStop");

        closeClient();
        super.onStop();
    }

    @Override
    protected void onPause() {

        Log.d(TAG, "SLAM onPause");

        closeClient();
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        Log.d(TAG, "SLAM onDestroy");

        closeClient();
        csFactory.close();
        super.onDestroy();
    }

    private boolean isConnectedOnDifferentLocalWifi(String serverIP) {
        String deviceIP = getDeviceIP();
        String[] deviceIPparts = deviceIP.split("\\.");
        String[] serverIPparts = serverIP.split("\\.");
        if (serverIPparts.length != 4 || !("192".equals(serverIPparts[0]) && "168".equals(serverIPparts[1]))) {
            // CloudSLAM is not working over local Wifi, so we assume everything is set up correctly.
            return false;
        }
        return !(serverIPparts[0].equals(deviceIPparts[0]) &&
                serverIPparts[1].equals(deviceIPparts[1]) &&
                serverIPparts[2].equals(deviceIPparts[2]));
    }

    private String getDeviceIP() {
        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return ip;
    }

    private void requestPermissionsIfNotGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            ArrayList<String> permissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (this.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
            }
            if (this.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
            }
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            if (permissions.size() > 0) {
                String[] perm = new String[permissions.size()];
                permissions.toArray(perm);
                this.requestPermissions(perm, 1);
                permissionsGranted = false;
            }
        }
    }

    private void setSurfaceViewScaleFromCameraOrientation() {

        Display display = getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        final float scale = (float) displaySize.y/displaySize.x;

        try{
            if(cloudSlam.getCameraOrientation() % 180 == 0 && !Build.MODEL.equals("M300")) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                //portrait -> scale y
                cloudSlamSurfaceView.setScaleY((float) 4/3/scale);
            }
            else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                //landscape -> scale x
                cloudSlamSurfaceView.setScaleX((float) 4/3*scale);
            }
            cloudSlam.logCameraAECharacteristics();
        } catch (CameraAccessException ex) {
            Log.e(TAG,"Could not access camera. Check your app permissions.");
        }
    }

    private CloudSLAM.Callback buildCloudSLAMCallback() {
        return new CloudSLAM.Callback() {
            @Override
            public void onOpen() {
                uiThreadHandler.obtainMessage(CS_CALLBACK_OPEN).sendToTarget();
            }

            @Override
            public void onStatusChanged(CloudSLAM.Status status) {
                uiThreadHandler.obtainMessage(CS_CALLBACK_STATUS_CHANGED, status).sendToTarget();
            }

            @Override
            public void onIdChanged(int id) {
                uiThreadHandler.obtainMessage(CS_CALLBACK_ID_CHANGED, id).sendToTarget();
            }

            @Override
            public void onCameraPoseUpdated() {
                uiThreadHandler.obtainMessage(CS_CALLBACK_CAMERA_POSE_UPDATE).sendToTarget();
            }

            @Override
            public void onBarcodeDetected(String barcode) {
                Log.d(TAG, "Barcode: " + barcode);
            }

            @Override
            public void onError(CSException e) {
                uiThreadHandler.obtainMessage(CS_CALLBACK_ERROR, e).sendToTarget();
            }

            @Override
            public void onClose() {
                uiThreadHandler.obtainMessage(CS_CALLBACK_CLOSE).sendToTarget();
            }
        };
    }

    private Handler buildUIThreadHandler() {
        return new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case ERROR: {
                        toast.setText(msg.obj.toString());
                        toast.getView().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                        toast.show();
                        break;
                    }
                    case INFO: {
                        toast.setText(msg.obj.toString());
                        toast.getView().getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
                        toast.show();
                        break;
                    }
                    case CS_CALLBACK_OPEN: {
                        updateConnectionMarker(LOST);
                        Log.v("CONNECTING", "connected");
                        connectingSnackbar.dismiss();
                        updateStreamButton(CONNECTED);
                        if (Settings.hideStatus) {
                            uiThreadHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (cloudSlam.isRunning()) {
                                        findViewById(R.id.connection).setAlpha(0.f);
                                    }
                                }
                            }, 3000);
                        }
                        break;
                    }
                    case CS_CALLBACK_CAMERA_POSE_UPDATE: {
                        int id = mShapeStorage.getVisibleShapeImgID(UserPose.readFromBufferTransposed(cloudSlam.getPoseBuffer())); // TODO: getVisibleShapeImgID should not require a transposed pose matrix.
                        if(id != lastIconId) {
                            uiThreadHandler.obtainMessage(CS_CALLBACK_ID_CHANGED, id).sendToTarget();
                        }
                        break;
                    }
                    case CS_CALLBACK_STATUS_CHANGED: {
                        CloudSLAM.Status status = (CloudSLAM.Status) msg.obj;
                        int s = status == CloudSLAM.Status.TRACKING ? TRACKING : LOST;
                        updateConnectionMarker(s);
                        Log.d(TAG, "Status: " + status);
                        break;
                    }
                    case CS_CALLBACK_ID_CHANGED: {
                        lastIconId = (int) msg.obj;
                        updateIcon(lastIconId);
                        break;
                    }
                    case CS_CALLBACK_ERROR: {
                        Exception e = (Exception) msg.obj;
                        connectingSnackbar.dismiss();
                        if (e instanceof CSNetworkException) {
                            uiThreadHandler.obtainMessage(ERROR, e.getMessage()).sendToTarget();
                            Log.e(TAG, "Network exception: " + e);
                        }
                        if (e instanceof CSCameraException) {
                            uiThreadHandler.obtainMessage(ERROR, e.getMessage()).sendToTarget();
                            Log.e(TAG, "Camera exception: " + e);
                        }
                        if (e instanceof CSEncoderException) {
                            uiThreadHandler.obtainMessage(ERROR, e.getMessage()).sendToTarget();
                            Log.e(TAG, "Encoder exception: " + e);
                        }
                        closeClient();
                        e.printStackTrace();
                        break;
                    }
                    case CS_CALLBACK_CLOSE: {
                        connectingSnackbar.dismiss();
                        readyToRun = true;
                        if (shouldStartQrConfiguration) {
                            shouldStartQrConfiguration = false;
                            shouldStartCloudSlam = false;
                            uiThreadHandler.obtainMessage(CS_CALLBACK_RUN_QR_SCAN).sendToTarget();
                        }
                        if (shouldStartCloudSlam) {
                            shouldStartCloudSlam = false;
                            shouldStartQrConfiguration = false;
                            uiThreadHandler.obtainMessage(CS_CALLBACK_RUN_CLOUDSLAM).sendToTarget();
                        }
                        resetUI();
                        closeClient();
                        break;
                    }
                    case CS_CALLBACK_RUN_QR_SCAN: {
                        Intent qrConfigurationIntent = new Intent(getApplicationContext(), QrConfigurationActivity.class);
                        qrConfigurationIntent.putExtra("CONFIG", cloudSlamConfig);
                        startActivityForResult(qrConfigurationIntent, QR_CONFIG_REQUEST_CODE);
                        break;
                    }
                    case CS_CALLBACK_RUN_CLOUDSLAM: {
                        if (!cloudSlam.isRunning()) {
                            cloudsSlamSurfaceViewHolder = cloudSlamSurfaceView.getHolder();
                            cloudSlam.start(cloudSlamConfig, cloudSlamCallback, cloudsSlamSurfaceViewHolder.getSurface());
                            try {
                                webSocketToAdminPanel = buildWebSocketConnectionToAdminPanel();
                                webSocketToAdminPanel.connectBlocking();
                            } catch (URISyntaxException e) {
                                uiThreadHandler.obtainMessage(ERROR, new CSNetworkException().new WebSocket().new InvalidURI(e)).sendToTarget();
                                closeClient();
                            } catch (InterruptedException e) {
                                closeClient();
                            }

                            Log.v("CONNECTING", "started connecting");
                            connectingSnackbar.show();
                            updateStreamButton(PENDING);
                            findViewById(R.id.connectMessage).setAlpha(0f);
                            if (Settings.show_preview) {
                                findViewById(R.id.blackout).setAlpha(0f);
                            }
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        };
    }

    private WebSocketClient buildWebSocketConnectionToAdminPanel() throws URISyntaxException {

        return new WebSocketClient(new URI("ws://" + Settings.ipAdminPanel + ":" + Settings.portAdminPanel)) {

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d(TAG, "webSocketToAdminPanel onOpen");
            }

            @Override
            public void onMessage(String message) {
//                    Log.d(TAG, message);
                mShapeStorage.loadShapes(message);

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG, "webSocketToAdminPanel onClose");
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "webSocketToAdminPanel onError");
                uiThreadHandler.obtainMessage(ERROR, new CSNetworkException().new WebSocket().new ConnectionAdminPanelRefused()).sendToTarget();
            }
        };
    }

    void updateConnectionMarker(int state) {
        ImageView connectionState = findViewById(R.id.connection);
        switch (state) {
            case NOT_CONNECTED:
                connectionState.setImageResource(R.drawable.ic_no_connection_50dp);
                ImageViewCompat.setImageTintList(connectionState, ColorStateList.valueOf(Color.parseColor("#ffff0000")));
                break;
            case LOST:
                connectionState.setImageResource(R.drawable.ic_not_tracking2_24dp);
                ImageViewCompat.setImageTintList(connectionState, ColorStateList.valueOf(Color.parseColor("#fffeaa0c")));
                break;
            case TRACKING:
                connectionState.setImageResource(R.drawable.ic_tracking_50dp);
                ImageViewCompat.setImageTintList(connectionState, ColorStateList.valueOf(Color.parseColor("#ff33cc00")));
                break;
        }
    }

    void updateStreamButton(int connection) {
        FloatingActionButton fb = findViewById(R.id.Stream);
        if (fb != null && fb.isShown()) {
            switch (connection) {
                case NOT_CONNECTED:
                    fb.setImageResource(R.drawable.ic_play_arrow_black_50dp);
                    break;
                case PENDING:
                    fb.setImageResource(R.drawable.ic_loop_black_50dp);
                    break;
                case CONNECTED:
                    fb.setImageResource(R.drawable.ic_pause_black_50dp);
                    break;
            }
        }
    }

    void updateIcon(int id) {

        if(!csFactory.isOpen()){
            currentId = id;
            return;
        }

        if (id != currentId) {
            Log.v(TAG, "updateIcon: The current ID has changed from "+currentId+" to "+id);
            if (isCSFragmentDisplayed()) {
                Log.v(TAG, "updateIcon: A UI fragment is already displayed.");
                if (currentCSFragment.isClosing() && currentCSFragment.listId == id) {
                    Log.v(TAG, "updateIcon: The UI fragment is closing, but should be displayed after all. Abort closing and carry on.");
                    currentCSFragment.abortClosing();
                } else {
                    if (id != -1 && !currentCSFragment.isPersistent) {
                        Log.v(TAG, "updateIcon: Another UI fragment should be displayed now. Immediately kill this one and show the next one.");
                        currentCSFragment.kill();
                    } else {
                        Log.v(TAG, "updateIcon: User has looked away and no other UI fragment should be displayed. Schedule current UI fragment for closing.");
                        if(currentCSFragment.isPersistent){
                            Log.v(TAG, "updateIcon: current CSFragment is persistent");
                        }
                        currentCSFragment.scheduleForClosing(2);
                        //currentCSFragment.close();
                    }
                }
            }
            if (id != -1 && !isCSFragmentDisplayed()) {
                Log.v(TAG, "updateIcon: A new UI fragment should be displayed.");
                currentCSFragment = csFactory.createCSFragment(id);
                if (currentCSFragment != null) {
                    Log.v(TAG, "updateIcon: Found UI fragment do display. Opening.");
                    currentCSFragment.listId = id;
                    currentCSFragment.open(this, R.id.stream_layout);
                    uiBringToFront();
                }
            }
            currentId = id;
        }
    }

    public void uiBringToFront() {
        FloatingActionButton b = findViewById(R.id.Stream);
        if (b != null) {
            b.bringToFront();
        }
        findViewById(R.id.connection).bringToFront();
        findViewById(R.id.Settings).bringToFront();
    }

    public void resetUI() {
        ImageView connectionState = findViewById(R.id.connection);
        if(connectionState.getAlpha() == 0.f){
            connectionState.setAlpha(1.f);
        }
        updateStreamButton(NOT_CONNECTED);
        updateConnectionMarker(NOT_CONNECTED);
        if (isCSFragmentDisplayed()) {
            currentCSFragment.kill();
            currentId = -1;
        }
    }

    private boolean isCSFragmentDisplayed() {
        boolean result;
        try {
            getFragmentManager().executePendingTransactions();
        } catch (NullPointerException ex) {
            Log.v(TAG, "Could not execute pending fragment transactions, because the activity is being destroyed. Doing nothing.");
        } finally {
            result = currentCSFragment != null && currentCSFragment.isAdded() && currentCSFragment.getActivity() != null;
        }
        return result;
    }
}