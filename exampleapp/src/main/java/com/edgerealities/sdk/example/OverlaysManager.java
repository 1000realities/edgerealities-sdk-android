package com.edgerealities.sdk.example;

import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;

import okhttp3.Response;

public class OverlaysManager {

    private String overlaysPath;
    private File overlaysDir;
    private File overlaysFile;
    private static final String TAG = "CS_OVERLAYS";

    public OverlaysManager(String path){
        overlaysPath = path;
        overlaysDir = new File(overlaysPath);
    }

    public void appendToOverlayFile (String iconID, Response response, int IconIndex) {
        try {
            if (overlaysFile.exists() && overlaysFile.getName().equals(iconID + ".json"))
            {
                FileOutputStream outputStream_append = new FileOutputStream(overlaysFile, true);
                outputStream_append.write(response.body().string().getBytes());
                outputStream_append.close();
                Log.d(TAG, "Append to file overlay: " + Settings.iconsIndexList.get(IconIndex));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createOverlayFile (String iconID, Response response, int IconIndex) {
        try {
            if (!overlaysDir.isDirectory()) {
                overlaysDir.mkdirs();
            }

            overlaysFile = new File(overlaysPath,  iconID + ".json");
            FileOutputStream outputStream = new FileOutputStream(overlaysFile, false);
            outputStream.write((response.body().string().getBytes()));
            outputStream.close();
            Log.d(TAG, "Create new file for overlay: " + Settings.iconsIndexList.get(IconIndex));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteOverlaysFiles () {
        if (overlaysDir.isDirectory()){
            for (File child : overlaysDir.listFiles()) {
                child.delete();
            }
        }
    }
}
