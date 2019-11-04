package com.ceiv.videopost;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;


/**
 * SharedPreferences工具
 */
public class SpUtils {
    private static String PREFERENCE_NAME = "sp";
    public static Context context;

    public SpUtils(Context co){
        context = co;
    }

    public static boolean putString(String key, String value) {
        SharedPreferences settings =context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        return editor.commit();
    }
    public static boolean putInt(String key, int value) {
        SharedPreferences settings =context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        return editor.commit();
    }

    public static boolean containsKey(Context context, String key){
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.contains(key);
    }


    public static String getString(String key) {
        return getString(key, "");
    }

    public static String getString(String key, String defaultValue) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.getString(key, defaultValue);
    }

    public static int getInt(String key, int def) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.getInt(key,def);
    }


    public static boolean putBoolean(String key, boolean value) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }


    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }


    public static boolean getBoolean(String key, boolean defaultValue) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.getBoolean(key, defaultValue);
    }

    public static void remove(Context context, String key){
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(key);
        editor.commit();
    }

    public static String getVersionName() {
        try {
            String var1 = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            return var1;
        } catch (PackageManager.NameNotFoundException var2) {
            var2.printStackTrace();
            return null;
        }
    }

    public static String getHardVersion() {
        String var1 = Build.PRODUCT;
        return var1 != null ? var1.replace(" ", "") : "1";
    }

    public static String getServerAppPort() {
        StringBuilder var1 = new StringBuilder();
        var1.append(":");
        var1.append(String.valueOf(getInt("serverAppPort", 9000)));
        var1.append("/MIPS/");
        return var1.toString();
    }
}
