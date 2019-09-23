package com.edgerealities.sdk.example;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public final class Settings {

    public static String iotIP;
    public static String iotPort;
    public static String checksum;
    public static ArrayList<String> iconsIndexList;
    public static boolean performanceDataOn;
    public static boolean hideStatus;
    public static boolean show_preview = false;
    public static ArrayList<String> cameraSizes;
    public static String ipAdminPanel;
    public static String portAdminPanel;

    private Settings(){}

    private static final Settings singleton = new Settings();

    public static Settings getInstance(){
        return singleton;
    }

    public static void save(Context context){

        SharedPreferences preferences = context.getSharedPreferences(singleton.getClass().getName(), MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Log.d("SAVING",singleton.getClass().getName() );
        try {
            for (Field field : singleton.getClass().getDeclaredFields()) {
                if(!Modifier.isFinal(field.getModifiers()) && field.get(singleton) != null) {
                    if (field.getType() == String.class) {
                        Log.d("SAVING",field.getName() + ": " + field.get(singleton).toString());
                        editor.putString(field.getName(), field.get(singleton).toString());
                    }
                    if (field.getType() == Integer.class || field.getType() == int.class) {
                        Log.d("SAVING",field.getName() + ": " + field.get(singleton));
                        editor.putInt(field.getName(), (Integer) field.get(singleton));
                    }
                    if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                        Log.d("SAVING",field.getName() + ": " + field.get(singleton));
                        editor.putBoolean(field.getName(), (Boolean) field.get(singleton));
                    }
                    if (field.getType() == Float.class || field.getType() == float.class) {
                        Log.d("SAVING",field.getName() + ": " + field.get(singleton));
                        editor.putFloat(field.getName(), (Float) field.get(singleton));
                    }
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        Log.d("SAVING",field.getName() + " size: " + ((Collection) field.get(singleton)).size());
                        editor.putInt(field.getName() + "Size",((Collection) field.get(singleton)).size());
                        int i = 0;
                        for(Object obj : (Collection) field.get(singleton)){
                            editor.putString(field.getName() + i, obj.toString());
                            i++;
                        }
                    }
                }
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        editor.commit();
    }

    public static void load(Context ctx){

        SharedPreferences preferences = ctx.getSharedPreferences(singleton.getClass().getName(), MODE_PRIVATE);

        Log.d("LOADING",singleton.getClass().getName() );
        try {
            for (Field field : singleton.getClass().getDeclaredFields()) {
                if(!Modifier.isFinal(field.getModifiers())) {
                    if (field.getType() == String.class) {
                        Log.d("LOADING",field.getName() + ": " + preferences.getString(field.getName(), ""));
                        field.set(singleton, preferences.getString(field.getName(), ""));
                    }
                    if (field.getType() == Integer.class || field.getType() == int.class) {
                        Log.d("LOADING",field.getName() + ": " + preferences.getInt(field.getName(), 0));
                        field.set(singleton, preferences.getInt(field.getName(), 0));
                    }
                    if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                        Log.d("LOADING",field.getName() + ": " + preferences.getBoolean(field.getName(), false));
                        field.set(singleton, preferences.getBoolean(field.getName(), false));
                    }
                    if (field.getType() == Float.class || field.getType() == float.class) {
                        Log.d("LOADING",field.getName() + ": " + preferences.getFloat(field.getName(), 0.f));
                        field.set(singleton, preferences.getFloat(field.getName(), 0.f));
                    }
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        Log.d("LOADING",field.getName() + ": " + preferences.getInt(field.getName()+"Size", 0));
                        int size = preferences.getInt(field.getName()+"Size",0);
                        Collection<String> collection = new ArrayList<>();
                        for(int i = 0; i != size; i++) {
                            collection.add(preferences.getString(field.getName()+i, ""));
                        }
                        field.set(singleton,collection);
                    }
                }
            }
            if(cameraSizes.isEmpty() && ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                cameraSizes = getCameraSizes();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<String> getCameraSizes()
    {
        Camera camera = Camera.open();
        Camera.Parameters params = camera.getParameters();
        final List<Camera.Size> prevSizes = params.getSupportedPreviewSizes();
        ArrayList<String> choiceItems = new ArrayList<>();
        for (Camera.Size s : prevSizes)
        {
            choiceItems.add(s.width + "x" + s.height);
        }
        camera.release();
        return choiceItems;
    }
}