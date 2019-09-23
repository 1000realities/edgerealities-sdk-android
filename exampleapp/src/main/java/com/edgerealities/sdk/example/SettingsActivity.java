package com.edgerealities.sdk.example;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.r1k.cloudslam.sdk.CSConfig;

import java.util.Arrays;


public class SettingsActivity extends Activity {

    CSConfig config;

    private EditText ipBox;
    private EditText udpPortBox;
    private EditText webSocketPortBox;
    private Spinner protoSpinner;
    private Spinner resSpinner;
    private Spinner rotSpinner;
    private EditText framerateBox;
    private EditText bitrateBox;
    private EditText keyframeBox;
    private EditText contrastBox;
    private EditText iotIpBox;
    private EditText iotPortBox;
    private CheckBox cameraColor;
    private CheckBox performanceBox;
    private CheckBox hideStatusBox;
    private CheckBox show_preview;

    private TextView versionTextView;
    private java.lang.CharSequence versionText;

    private static final int M300_SELECT_BUTTON = 66;
    private static final int M300_BACK_BUTTON = 21;
    private static final int M300_FORWARD_BUTTON = 22;
    private static final int M300_TOUCH = 23;

    private static final String TAG = "CS_Settings_A";

    private boolean saved;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Log.d(TAG, "model " + Build.MODEL);

        if(Build.MODEL.equals("ZEISS")) {
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        if(Build.MODEL.equals("ZEISS")) {
            setContentView(R.layout.connection_settings_zeiss);
        } else {
            setContentView(R.layout.connection_settings);
        }

        config = (CSConfig) getIntent().getSerializableExtra("CONFIG");

        ipBox = findViewById(R.id.ipBox);
        udpPortBox = findViewById(R.id.udpPort);
        webSocketPortBox = findViewById(R.id.WsPortBox);
        cameraColor = findViewById(R.id.cameraColor);
        performanceBox = findViewById(R.id.performance);
        hideStatusBox = findViewById(R.id.hide_status);
        resSpinner = findViewById(R.id.resSpinner);
        protoSpinner = findViewById(R.id.protocolSpinner);
        rotSpinner = findViewById(R.id.rotationSpinner);
        framerateBox = findViewById(R.id.framerate);
        keyframeBox = findViewById(R.id.keyframeInterval);
        bitrateBox = findViewById(R.id.bitrate);
        contrastBox = findViewById(R.id.contrast);
        iotIpBox = findViewById(R.id.iotIp);
        iotPortBox = findViewById(R.id.iotPort);

        versionTextView = findViewById(R.id.versionTextView);
        versionText = versionTextView.getText();
        show_preview = findViewById(R.id.show_preview);

        updateUI();

        saved = false;
    }

    private void updateUI(){

        Log.d(TAG, "updateUI ...");

        Settings.load(getApplicationContext());

        if(Settings.cameraSizes == null){
            Toast toast = Toast.makeText(this, "You have to grant camera permissions before configuring CloudSLAM" , Toast.LENGTH_LONG);
            ((TextView)toast.getView().findViewById(android.R.id.message)).setGravity(Gravity.CENTER);
            toast.getView().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
            toast.show();

            Intent goBack = new Intent();
            setResult(RESULT_CANCELED, goBack);
            finish();
        }

        try {
            PackageInfo info = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = String.valueOf(info.versionName).contains("RELEASE") ? String.valueOf(info.versionName).replace("-RELEASE", "") : String.valueOf(info.versionName).replace("-DEBUG","-D");
            versionTextView.setText(versionText+versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            versionTextView.setText(versionText+"Error in getting app's version");
        }

        iotIpBox.setText(Settings.iotIP);
        iotPortBox.setText(Settings.iotPort);

        if(config != null) {

            Log.d(TAG, "Settings: " + Settings.performanceDataOn + " " +  Settings.hideStatus);

            ipBox.setText(config.ip);
            udpPortBox.setText("" + config.udpPort);
            webSocketPortBox.setText("" + config.webSocketPort);
            performanceBox.setChecked(Settings.performanceDataOn);
            cameraColor.setChecked(config.cameraColor);

            if ( hideStatusBox != null ) {
                hideStatusBox.setChecked(Settings.hideStatus);
            } else {
                Log.d(TAG, "hideStatusBox is null");
            }

            if ( show_preview != null ) {
                show_preview.setChecked(Settings.show_preview);
            } else {
                Log.d(TAG, "show_preview is null");
            }

            framerateBox.setText("" + config.frameRate);
            bitrateBox.setText("" + config.bitrate);
            keyframeBox.setText("" + config.keyFrameInterval);
            contrastBox.setText("" + config.contrast);
        }

        int resource = Build.MODEL.equals("ZEISS")? R.layout.spinner_layout_zeiss : android.R.layout.simple_spinner_item;

        resSpinner.setAdapter(new ArrayAdapter<>(this, resource, Settings.cameraSizes));
        protoSpinner.setAdapter(new ArrayAdapter<>(this, resource, CSConfig.Protocol.values()));
        rotSpinner.setAdapter(new ArrayAdapter<>(this, resource, CSConfig.Rotation.values()));

        if (config != null) {
            int res = Settings.cameraSizes.indexOf(config.frameWidth + "x" + config.frameHeight);
            if(res != -1) {
                resSpinner.setSelection(res);
            } else {
                resSpinner.setSelection(0);
            }
            int proto = Arrays.asList(CSConfig.Protocol.values()).indexOf(config.protocol);
            if (proto != -1) {
                protoSpinner.setSelection(proto);
            } else {
                protoSpinner.setSelection(0);
            }
            int rot = Arrays.asList(CSConfig.Rotation.values()).indexOf(config.frameRotation);
            if (rot != -1) {
                rotSpinner.setSelection(rot);
            } else {
                rotSpinner.setSelection(0);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == M300_TOUCH){
            if(Build.MODEL.equals("M300")) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null) {
                    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }
        }
        if (keyCode == M300_BACK_BUTTON) {
            if(!saved) {
                save();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onSave(View view) {
        goBack();
    }

    private void goBack(){

        save();

        Intent goBack = new Intent();

        goBack.putExtra("CONFIG", config);

        setResult(RESULT_OK, goBack);

        finish();
    }

    private void save() {

        if(config == null){
            config = new CSConfig();
        }

        config.ip = ipBox.getText().toString().trim();
        config.udpPort = parseIntOrZero(udpPortBox.getText().toString().trim());
        config.webSocketPort = parseIntOrZero(webSocketPortBox.getText().toString().trim());

        String res[] = resSpinner.getSelectedItem().toString().split("x");
        config.frameWidth = Integer.parseInt(res[0]);
        config.frameHeight = Integer.parseInt(res[1]);

        config.protocol = CSConfig.Protocol.valueOf(protoSpinner.getSelectedItem().toString());
        config.frameRotation = CSConfig.Rotation.valueOf(rotSpinner.getSelectedItem().toString());

        config.frameRate = parseIntOrZero(framerateBox.getText().toString().trim());
        config.bitrate = parseIntOrZero(bitrateBox.getText().toString().trim());
        config.keyFrameInterval = parseIntOrZero(keyframeBox.getText().toString().trim());
        config.contrast = parseIntOrZero(contrastBox.getText().toString().trim());

        config.cameraColor= cameraColor.isChecked();

        Settings.performanceDataOn = performanceBox.isChecked();
        if ( hideStatusBox != null ) {
            Settings.hideStatus = hideStatusBox.isChecked();
        }
        if ( show_preview != null ) {
            Settings.show_preview = show_preview.isChecked();
        }
        Settings.iotIP = iotIpBox.getText().toString().trim();
        Settings.iotPort = iotPortBox.getText().toString().trim();

        Settings.save(getApplicationContext());

        saved = true;
    }

    private int parseIntOrZero(String s){
        try {
            return !s.isEmpty() ? Integer.parseInt(s) : 0;
        } catch (NumberFormatException e){
            return 0;
        }
    }

    @Override
    protected void onResume() {
        updateUI();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        goBack();
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        if(!saved) {
            save();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(!saved) {
            save();
        }
        super.onDestroy();
    }

}