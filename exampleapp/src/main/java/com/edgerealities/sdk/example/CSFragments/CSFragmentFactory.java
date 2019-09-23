package com.edgerealities.sdk.example.CSFragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.widget.Toast;

import com.edgerealities.sdk.example.R;
import com.edgerealities.sdk.example.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

public class CSFragmentFactory {

    private static final String TAG = "CS_DATA_FACTORY";

    /**
     *  Copied from android.view.displaylistcanvas that throw excetpion if bitmap size is bigger that this value
     */
    private static final int MAX_BITMAP_SIZE = 100 * 1024 * 1024; // 100 MB

    /**
     * Just a wrapper for a HashMap allowing chained PUT for convenience
     */
    private class CSInfo {
        HashMap<String, Object> info;
        CSInfo(){
            info = new HashMap<>();
        }

        CSInfo(int capacity){
            info = new HashMap<>(capacity);
        }

        CSInfo put(String key, Object value){
            info.put(key, value);
            return this;
        }

        Object get(String key){
            return info.get(key);
        }

        Object get(String key, Object defaultValue){
            Object ret = get(key);
            return ret != null ? ret : defaultValue;
        }

        boolean contains(String key){
            return info.containsKey(key);
        }
    }

    enum Type {
        refactorvalve,
        refactorimage,
        refactorgif,
        image,
        gif
    }

    private SparseArray<CSInfo> fragmentList;
    private Handler csfHandler;
    private Toast toast;
    private static final int ERROR = 0;
    private static final int INFO = 1;
    private Context mContext;

    private HttpClient client;
    private String ip;
    private String port;
    private boolean open;
    private PassedData passedData;

    private String overlaysPath;

    public CSFragmentFactory(String ip, String port, Context context, PassedData passedData){
        this.passedData = passedData;
        mContext = context;
        this.ip = ip;
        this.port = port;
        overlaysPath = (mContext.getExternalFilesDir(null) + "/overlays/");
        client = new HttpClient();
    }

    public void open(){

        toast = Toast.makeText(mContext, "" , Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        csfHandler = new Handler(Looper.getMainLooper()){
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

        client.init();
        fragmentList = createCSInfoList();

        initializeDeviceStates();

        open = true;
    }

    public void close(){
        client.release();
        open = false;
    }

    public boolean isOpen(){
        return open;
    }

    /**
     * Creates Fragment representing IoT device associated with this id. This fragment should be
     * added to activity and displayed to user.
     * @param id received from CloudSLAM server.
     * @return Fragment representing IoT device associated with this id or null in case of invalid
     * parameters or nonexistent id.
     */
    public CSFragment createCSFragment(int id){

        CSInfo info = null;

        if (id != 0) {
            if (fragmentList.get(id) != null) {
                info = fragmentList.get(id);
            } else if (Settings.iconsIndexList.contains(String.valueOf(id))) {
                info = readOverlayFromFile(id);
            } else {
                Log.v(TAG, "createCSFragment: Icon was not downloaded from AdminPanel.");
                showAlert("Could not display this overlay, please scan QR code.");
            }
        } else {
            Log.v(TAG, "createCSFragment: Cannot find appropriate UI fragment. Doing nothing.");
            showAlert("This shape does not have any overlay attached. Select shape in the \"Map\" tab of administration panel and add an overlay");
        }

        if(info == null || !info.contains("type")){
            return null;
        }
        switch ((Type)info.get("type")){
            case refactorimage:
                return RefactorCSIcon.newInstance((Bitmap)info.get("resource"));
            case refactorgif:
                return RefactorCSGif.newInstance(
                        (byte[])info.get("resource",0),
                        (int)info.get("loops", 0)
                );
            case refactorvalve:
                return RefactorCSValve.newInstance(client, ip, port, (PassedData) info.get("data", null), (Bitmap)info.get("resource") );
            case image:
                return CSIcon.newInstance((int)info.get("resource"));
            case gif:
                return CSGif.newInstance(
                        (int)info.get("resource",0),
                        (int)info.get("loops", 0)
                );
            default:
                return null;
        }
    }

    private SparseArray<CSInfo> createCSInfoList(){
        SparseArray<CSInfo> list = new SparseArray<>();
        list.append(98, new CSInfo(2).put("type", Type.image).put("resource", R.drawable.device));
        list.append(99, new CSInfo(2).put("type", Type.image).put("resource", R.drawable.language));
        return list;
    }

    private CSInfo readOverlayFromFile (int id) {
        try {
            String readData = readJsonData(id);
            if (readData != null) {
                JSONObject icon = new JSONObject(readData);
                if (icon.has("type")) {
                    switch (icon.getString("type")) {
                        case "image": {
                            String encodedString = icon.getString("image");
                            final String pureBase64Encoded = encodedString.substring(encodedString.indexOf(",") + 1);
                            final byte[] decodedBytes = Base64.decode(pureBase64Encoded, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                            int bitmapSize = decodedBitmap.getByteCount();
                            if (bitmapSize > MAX_BITMAP_SIZE) {
                                throw new RuntimeException(
                                        "Canvas: trying to draw too large(" + bitmapSize + "bytes) bitmap.");
                            } else {
                                return new CSInfo(2).put("type", Type.refactorimage).put("resource", decodedBitmap);
                            }
                        }
                        case "valve": {
                            PassedData tmpPassData = new PassedData(passedData);
                            tmpPassData.setValveData(icon.getInt("valveId"), (float) icon.getDouble("valveX"), (float) icon.getDouble("valveY"));
                            String encodedString = icon.getString("image");
                            final String pureBase64Encoded = encodedString.substring(encodedString.indexOf(",") + 1);
                            final byte[] decodedBytes = Base64.decode(pureBase64Encoded, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                            int bitmapSize = decodedBitmap.getByteCount();
                            if (bitmapSize > MAX_BITMAP_SIZE) {
                                throw new RuntimeException(
                                        "Canvas: trying to draw too large(" + bitmapSize + "bytes) bitmap.");
                            } else  {
                                return new CSInfo(3).put("type", Type.refactorvalve).put("data", tmpPassData).put("resource", decodedBitmap);
                            }
                        }
                        case "gif": {
                            String encodedString = icon.getString("image");
                            final String pureBase64Encoded = encodedString.substring(encodedString.indexOf(",") + 1);
                            final byte[] decodedBytes = Base64.decode(pureBase64Encoded, Base64.DEFAULT);
                            return new CSInfo(3).put("type", Type.refactorgif).put("resource", decodedBytes).put("loops", icon.getInt("gifLoops"));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.d(TAG, "Error in loading icons: " + e);
            e.printStackTrace();
            return null;
        } catch (OutOfMemoryError e) {
            showAlert("Overlay with id: " + id + " cannot be read (Out of memory).");
            e.printStackTrace();
            return null;
        } catch (RuntimeException e) {
            e.printStackTrace();
            Log.v(TAG, "updateIcon: " + e.getMessage());
            showAlert("Overlay with id: " + id + " cannot be shown (too big resolution).");
            return null;
        }
        return null;
    }

    private String readJsonData (int iconID) {
        File ovlerlay = new File(overlaysPath,  iconID + ".json");
        String jsonData = null;
        try {
            if(ovlerlay.exists()) {
                FileInputStream inputStream = new FileInputStream(ovlerlay);
                int size = inputStream.available();
                byte[] buffer = new byte[size];
                inputStream.read(buffer);
                inputStream.close();
                jsonData = new String(buffer, "UTF-8");
            }
            Log.d(TAG, "Overlay with id: " + iconID + " read from file.");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } catch (OutOfMemoryError e) {
            showAlert("Overlay with id: " + iconID + " cannot be read (Out of memory).");
            e.printStackTrace();
            return null;
        }
        return jsonData;
    }

    private void initializeDeviceStates() {
        //TODO: this should be done based on the results of createCSInfoList.
    }

    private void showAlert (String message) {
        toast = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
        toast.getView().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        toast.show();
    }
}