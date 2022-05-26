package com.metadapp.videoplayerapp.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public class Widget {
    public static boolean canPip(Context ctx){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && ctx.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }
}
