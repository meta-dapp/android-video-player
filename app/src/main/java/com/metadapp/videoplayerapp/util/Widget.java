package com.metadapp.videoplayerapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class Widget {
    public static boolean canPip(Context ctx){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && ctx.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    public  static void putDataPref(Context ctx, String key, String value){
        try{
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
            editor.putString(key, value);
            editor.apply();
        }catch (Exception exx){

        }
    }

    public static String getDataPref(Context ctx, String key){
        try{
            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(ctx);
            return SP.getString(key, "");
        }catch (Exception e){
            return "";
        }
    }

    public static int getSavedBrightness(Context mContext){
        String br = getDataPref(mContext, "screen_cur_br");
        if (br.isEmpty()){
            return (int)getBrightness(mContext);
        }else{
            return (int)Float.parseFloat(br);
        }
    }

    private static float getBrightness(Context mContext) {
        try {
            float curBrightnessValue = android.provider.Settings.System.getInt(
                    mContext.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
            return curBrightnessValue;
        } catch (Exception e) {
            return -1;
        }
    }
}
