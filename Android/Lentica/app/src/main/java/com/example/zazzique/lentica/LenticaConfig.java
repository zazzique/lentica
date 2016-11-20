package com.example.zazzique.lentica;

import android.os.Environment;

import java.io.File;

/**
 * Created by zzq on 14.10.2015.
 */
public final class LenticaConfig {
    private static final String TAG = LenticaConfig.class.getName();

    public static final int MAX_ROWS = 3;
    public static final int MAX_COLUMNS = 5;

    public static final float SENSITIVITY_HORIZONTAL = 3.0f;
    public static final float SENSITIVITY_VERTICAL = 4.0f;
    public static final float THRESHHOLD = 0.2f;

    public static final float DEFAULT_SCALE = 1.0f;

    public static final String CLIP_LABEL = "LENTICA_LABEL";

    public static String getDcimPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "Lentica";
    }

    public static String getLenticularImagesPath() {
        return Environment.getExternalStorageDirectory() + File.separator + "Lentica";
    }
}
