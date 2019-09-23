package com.edgerealities.sdk.example;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.r1k.cloudslam.sdk.CSConfig;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class QrConfigurationActivity extends Activity {

    private BarcodeView scanner;
    private OkHttpClient httpClient;
    private Request request;
    private Request requestchecksum;
    private Request requestgraphics;
    private Request requestgraphicslist;
    private Response response;
    private URL graphics_url;
    private String mainURL;
    boolean pending_query = false;
    private static final String TAG = "CS_QR_CONFIG";
    private Handler qrHandler;
    private CSConfig config;
    private Toast toast;
    private String checksum;
    private boolean isRequestProcessing = false;
    private boolean isPartialContent = false;
    private ProgressBar progressBar;
    private TextView iconsInfo;
    private int IconIndex = 0;
    private OverlaysManager overlaysManager;

    private static final int ERROR = 0;
    private static final int INFO = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_configuration);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        config = (CSConfig) getIntent().getSerializableExtra("CONFIG");

        scanner = findViewById(R.id.qr_scanner);
        httpClient = new OkHttpClient();
        httpClient = new OkHttpClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        overlaysManager = new OverlaysManager(getExternalFilesDir(null) + "/overlays/");

        iconsInfo = findViewById(R.id.infoText);
        progressBar = findViewById(R.id.progressBar);

        toast = Toast.makeText(this, "" , Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);

        qrHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case ERROR:
                        toast.setText(msg.obj.toString());
                        toast.getView().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                        toast.show();
                        break;
                    case INFO:
                        toast.setText(msg.obj.toString());
                        toast.getView().getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
                        toast.show();
                        break;
                    default:
                        break;
                }
            }
        };

        setResult(RESULT_CANCELED);

        scanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                String qrMsg = result.getText();
                mainURL = qrMsg;
                Settings.ipAdminPanel = qrMsg.split(":")[0];
                Settings.portAdminPanel = qrMsg.split(":")[1];
                if(!pending_query) {
                    pending_query = true;
                    if(!isConnectedInSameWifi(qrMsg)){
                        qrHandler.obtainMessage(ERROR,"You aren't connected in the same wifi as the server!").sendToTarget();
                        pending_query = false;
                    }
                    else {
                        queryServerForConfig(
                                "http://" + qrMsg + "/client/config/" + Build.MODEL,
                                "http://" + qrMsg + "/client/graphicschecksum",
                                "http://" + qrMsg + "/client/graphics",
                                "http://" + qrMsg + "/client/graphics/list");
                    }
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {

            }
        });
    }

    private String getDeviceIP(){
        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        return ip;
    }

    private boolean isConnectedInSameWifi(String serverSocket){
        String deviceIP = getDeviceIP();
        String[] deviceIPparts = deviceIP.split("\\.");

//        todo : need to be test
//        if ( deviceIPparts[0].equals("10") ||
//             deviceIPparts[0].equals("172") ||
//             ( deviceIPparts[0].equals("192") && deviceIPparts[1].equals("169") ) ) {
//            return true;
//        }

        String[] serverSocketParts = serverSocket.split(":");
        if(serverSocketParts.length != 2){
            return false;
        }
        String[] serverIPparts = serverSocketParts[0].split("\\.");
        if(serverIPparts.length != 4){
            return false;
        }
        if(!serverIPparts[0].equals("192") && !serverIPparts[1].equals("168")) {
            return true;
        }
        if(serverIPparts[0].equals(deviceIPparts[0]) &&
           serverIPparts[1].equals(deviceIPparts[1]) &&
           serverIPparts[2].equals(deviceIPparts[2])){
            return true;
        }
        return false;
    }

    private void close(){
        Settings.save(getApplicationContext());
        Intent goBack = new Intent();
        goBack.putExtra("CONFIG", config);
        setResult(RESULT_OK, goBack);
        finish();
    }

    private void queryServerForConfig(String url, String checksumurl, String graphicsurl, String graphicslisturl) {
        Log.d(TAG, "urls: " + url + "   " + checksumurl + "   " + graphicsurl);
        URL server_url;
        URL checksum_url;
        URL graphicslist_url;
        try {
            server_url = new URL(url);
            checksum_url = new URL(checksumurl);
            graphics_url = new URL(graphicsurl);
            graphicslist_url = new URL(graphicslisturl);
        } catch (MalformedURLException e) {
            Log.d(TAG, "QR code invalid");
            qrHandler.obtainMessage(ERROR, "QR code invalid").sendToTarget();
            pending_query = false;
            return;
        }
        request = new Request.Builder().url(server_url).build();
        requestchecksum = new Request.Builder().url(checksum_url).build();
        requestgraphicslist = new Request.Builder().url(graphicslist_url).build();

        (new Thread(new Runnable() {
            @Override
            public void run() {
                int step = 0;
                try {
                    qrHandler.obtainMessage(INFO, "Querying server for configuration").sendToTarget();
                    response = httpClient.newCall(request).execute();
                    if (processResponse(response)) {
                        step++;
                    } else {
                        pending_query = false;
                    }
                } catch (IOException e) {
                    qrHandler.obtainMessage(ERROR, "Could not connect to server").sendToTarget();
                    pending_query = false;
                    e.printStackTrace();
                }
                if(step==1) {
                    try {
                        response = httpClient.newCall(requestchecksum).execute();
                        int processed = processResponseChecksum(response);
                        if (processed == 2) {
                            step++;
                        } else if (processed == 1){
                            qrHandler.obtainMessage(INFO, "Configuration complete").sendToTarget();
                            close();
                        } else {
                            pending_query = false;
                        }
                    } catch (IOException e) {
                        qrHandler.obtainMessage(ERROR, "Could not connect to server").sendToTarget();
                        pending_query = false;
                        e.printStackTrace();
                    }
                }
                if(step==2) {
                    try {
                        overlaysManager.deleteOverlaysFiles();
                        response = httpClient.newCall(requestgraphicslist).execute();
                        if(processResponseGraphicsList(response)) {
                            step++;
                        } else {
                            pending_query = false;
                        }
                    } catch (IOException e) {
                        qrHandler.obtainMessage(ERROR, "Could not connect to server").sendToTarget();
                        pending_query = false;
                        e.printStackTrace();
                    }
                }
                if(step==3) {
                    try {
                        if(!Settings.iconsIndexList.isEmpty()){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    iconsInfo.setVisibility(View.VISIBLE);
                                    progressBar.setVisibility(View.VISIBLE);
                                }
                            });
                            processPartialContent(0, null);
                        } else {
                            processResponseGraphics();
                            qrHandler.obtainMessage(INFO, "Configuration complete").sendToTarget();
                        }
                        close();
                    } catch (Exception e) {
                        qrHandler.obtainMessage(ERROR, "Could not connect to server").sendToTarget();
                        pending_query = false;
                        e.printStackTrace();
                    }
                }

            }
        })).start();
    }

    private void processPartialContent (int startRange, String iconId) {
        if (!isRequestProcessing || isPartialContent) {
            isRequestProcessing = true;

            if(iconId == null) {
                iconId = Settings.iconsIndexList.get(IconIndex);
            }

            try {

                URL newURL = new URL("http://" + mainURL + graphics_url.getPath() + "?id=" + iconId);

                requestgraphics = new Request.Builder()
                        .url(newURL)
                        .addHeader("Connection", "keep-alive")
                        .addHeader("Content-Range", "bytes=" + startRange + "-")
                        .build();

                response = httpClient.newCall(requestgraphics).execute();

                //Log status and headers;
                Log.d(TAG, "Status code: " + response.code());
                Headers headers = response.headers();
                Set<String> names = headers.names();
                for (String name : names) {
                    Log.d(TAG, name + ": " + headers.values(name));
                }

                String range = response.header("Content-Range").split("bytes=")[1];
                int start = Integer.parseInt(range.split("-")[0]);
                int end = Integer.parseInt(range.split("-")[1].split("/")[0]);

                if (start == 0){
                    overlaysManager.createOverlayFile(iconId, response, IconIndex);
                } else {
                    overlaysManager.appendToOverlayFile(iconId, response, IconIndex);
                }

                if(response.code() == 200) {
                    if (IconIndex == Settings.iconsIndexList.size()-1) {
                        response.close();
                        IconIndex = 0;
                        if (processResponseGraphics()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    iconsInfo.setVisibility(View.INVISIBLE);
                                    progressBar.setVisibility(View.INVISIBLE);
                                }
                            });
                            qrHandler.obtainMessage(INFO, "Icons downloaded").sendToTarget();
                        } else {
                            pending_query = false;
                        }
                    } else {
                        isPartialContent = true;
                        IconIndex++;
                        processPartialContent(0, Settings.iconsIndexList.get(IconIndex));
                    }
                } else if (response.code() == 206) {
                    isPartialContent = true;
                    processPartialContent(end+1, Settings.iconsIndexList.get(IconIndex));
                } else {
                    Log.d(TAG, "Code status: " + response.code());
                    qrHandler.obtainMessage(ERROR, "Code status: " + response.code()).sendToTarget();
                }
            } catch (MalformedURLException e) {
                Log.d(TAG, "Wrong URL request");
                qrHandler.obtainMessage(ERROR, "Wrong URL request").sendToTarget();
            } catch (IOException e) {
                qrHandler.obtainMessage(ERROR, "Could not connect to server").sendToTarget();
                pending_query = false;
            }
        }
    }

    private boolean processResponse(Response response){
        try {

            String jsonData = response.body().string();
            response.close();
            Log.d(TAG, jsonData);
            JSONObject json = new JSONObject(jsonData);

            config = CSConfig.parseJSON(json);
            Settings.iotIP = json.getString("iov_ip") != null? json.getString("iov_ip") : "";
            Settings.iotPort = json.getString("iov_port") != null? json.getString("iov_port") : "";
            return true;
        } catch (IOException e) {
            Log.d(TAG,"Error while reading message");
            e.printStackTrace();
            qrHandler.obtainMessage(ERROR, "Error while reading message").sendToTarget();
        } catch (JSONException e) {
            Log.d(TAG,"Invalid configuration parameters");
            e.printStackTrace();
            qrHandler.obtainMessage(ERROR, "Invalid configuration parameters").sendToTarget();
        } catch (NullPointerException e){
            Log.d(TAG,"Config message empty");
            e.printStackTrace();
            qrHandler.obtainMessage(ERROR, "Config message empty").sendToTarget();
        }
        return false;
    }

    private int processResponseChecksum(Response response) {
        try {
            checksum = response.body().string();
            response.close();
            Log.d(TAG, checksum);
            if( checksum.equals(Settings.checksum) ){
                return 1;
            }
            return 2;
        } catch (IOException e) {
            Log.d(TAG, "Error while reading message");
            e.printStackTrace();
            qrHandler.obtainMessage(ERROR, "Error while reading message").sendToTarget();
        } catch (NullPointerException e) {
            Log.d(TAG, "Checksum message empty");
            e.printStackTrace();
            qrHandler.obtainMessage(ERROR, "Checksum message empty").sendToTarget();
        }
        return 0;
    }

    private boolean processResponseGraphicsList (Response response) {
        try {
            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONArray iconsIndexArray = jsonObject.getJSONArray("overlays");
            Settings.iconsIndexList = new ArrayList<String>();
            for (int i = 0; i < iconsIndexArray.length(); i++){
                Settings.iconsIndexList.add(iconsIndexArray.get(i).toString());
                Log.d(TAG, Settings.iconsIndexList.get(i));
            }
            return true;
        } catch (JSONException e) {
            Log.d(TAG, "Error while reading json");
            e.printStackTrace();
            qrHandler.obtainMessage(ERROR, "Error while reading json").sendToTarget();
        } catch (Exception e) {
            Log.d(TAG, "Error while reading message");
            e.printStackTrace();
            qrHandler.obtainMessage(ERROR, "Error while reading message").sendToTarget();
        }
        return false;
    }

    private boolean processResponseGraphics() {
        try {
            Settings.checksum = checksum;
            return true;
        } catch (NullPointerException e) {
            Log.d(TAG, "Devices message empty");
            e.printStackTrace();
            qrHandler.obtainMessage(ERROR, "Devices message empty").sendToTarget();
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(scanner != null) {
            scanner.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(scanner != null) {
            scanner.pause();
        }
    }

    @Override
    protected void onStop() {
        if(scanner != null) {
            scanner.pause();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(scanner != null) {
            scanner.pause();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(scanner != null) {
            scanner.pause();
        }
        finish();
        super.onBackPressed();
    }
}
