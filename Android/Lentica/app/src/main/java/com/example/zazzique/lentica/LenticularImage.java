package com.example.zazzique.lentica;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by zazzique on 22.10.2015.
 */
public class LenticularImage {
    private static final String TAG = LenticularImage.class.getName();

    public static final int FILTER_SIMPLE = 0;
    public static final int FILTER_INTERLACE = 1;

    public static final String[] LOOKS_ARRAY = { "normal", "blackandwhite", "sepia", "lark", "valencia" };

    private int mFilter = FILTER_SIMPLE;
    private String mLook = LOOKS_ARRAY[0];

    private float mBrightness = 0.0f;
    private float mContrast = 1.0f;
    private float mSaturation = 1.0f;

    private String[][] mImagePaths = null;

    public float[][] mOffsetX;
    public float[][] mOffsetY;
    public float[][] mRotation;
    public float[][] mScale;

    LenticularImageListener mListener = null;

    public interface LenticularImageListener {
        void onUpdate();
    }

    public LenticularImage() {
        mImagePaths = new String[LenticaConfig.MAX_ROWS][LenticaConfig.MAX_COLUMNS];

        mOffsetX = new float[LenticaConfig.MAX_ROWS][LenticaConfig.MAX_COLUMNS];
        mOffsetY = new float[LenticaConfig.MAX_ROWS][LenticaConfig.MAX_COLUMNS];
        mRotation = new float[LenticaConfig.MAX_ROWS][LenticaConfig.MAX_COLUMNS];
        mScale = new float[LenticaConfig.MAX_ROWS][LenticaConfig.MAX_COLUMNS];

        for (int i = 0; i < LenticaConfig.MAX_ROWS; i++) {
            for (int j = 0; j < LenticaConfig.MAX_COLUMNS; j++) {
                mImagePaths[i][j] = null;
                mOffsetX[i][j] = 0.0f;
                mOffsetY[i][j] = 0.0f;
                mRotation[i][j] = 0.0f;
                mScale[i][j] = LenticaConfig.DEFAULT_SCALE;
            }
        }
    }

    public void loadFromFile(String path) {

        for (int i = 0; i < LenticaConfig.MAX_ROWS; i++) {
            for (int j = 0; j < LenticaConfig.MAX_COLUMNS; j++) {
                mImagePaths[i][j] = null;
            }
        }

        String jsonString = null;

        File file = new File(path);
        try {
            FileInputStream fileStream = new FileInputStream(file);
            int size = fileStream.available();
            byte[] buffer = new byte[size];
            fileStream.read(buffer);
            fileStream.close();
            jsonString = new String(buffer, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        if (jsonString == null) return;

        try {
            JSONObject jObject = new JSONObject(jsonString);
            JSONArray jArray = jObject.optJSONArray("images");

            if (jObject.has("filter")) {
                mFilter = jObject.optInt("filter");
            }
            if (jObject.has("look")) {
                mLook = jObject.optString("look");
            }

            if (jObject.has("brightness")) {
                mBrightness = (float)jObject.optDouble("brightness");
            }
            if (jObject.has("contrast")) {
                mContrast = (float) jObject.optDouble("contrast");
            }

            if (jObject.has("saturation")) {
                mSaturation = (float) jObject.optDouble("saturation");
            }

            for (int i = 0; i < jArray.length(); i++) {
                JSONObject jSubObject = jArray.optJSONObject(i);

                if (jSubObject == null) continue;

                int row = jSubObject.optInt("row");
                int column = jSubObject.optInt("column");

                float offsetX = (float)jSubObject.optDouble("offsetX");
                float offsetY = (float)jSubObject.optDouble("offsetY");
                float rotation = (float)jSubObject.optDouble("rotation");
                float scale = (float)jSubObject.optDouble("scale");
                if (scale <= 0.05f) scale = 1.0f; // TODO: to config

                setImage(jSubObject.optString("file"), row, column, offsetX, offsetY, rotation, scale);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return;
        }

        if (mListener != null)
            mListener.onUpdate();
    }

    public void saveToFile(String path) {

        int imagesCount = 0;

        JSONObject jObject = new JSONObject();

        JSONArray jArray = new JSONArray();
        for (int i = 0; i < LenticaConfig.MAX_ROWS; i++) {
            for (int j = 0; j < LenticaConfig.MAX_COLUMNS; j++) {
                if (mImagePaths[i][j] != null) {
                    imagesCount ++;

                    JSONObject jSubObject = new JSONObject();
                    try {
                        jSubObject.put("row", i);
                        jSubObject.put("column", j);
                        jSubObject.put("offsetX", mOffsetX[i][j]);
                        jSubObject.put("offsetY", mOffsetY[i][j]);
                        jSubObject.put("rotation", mRotation[i][j]);
                        jSubObject.put("scale", mScale[i][j]);
                        jSubObject.put("file", mImagePaths[i][j]);
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }

                    jArray.put(jSubObject);
                }
            }
        }

        if (imagesCount <= 0) return;

        try {
            jObject.put("filter", mFilter);
            jObject.put("look", mLook);
            jObject.put("brightness", mBrightness);
            jObject.put("contrast", mContrast);
            jObject.put("saturation", mSaturation);
            jObject.put("images", jArray);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        String jsonString = null;

        try {
            jsonString = jObject.toString();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return;
        }

        File file = new File(path);
        try {
            FileOutputStream fileStream = new FileOutputStream(file);
            fileStream.write(jsonString.getBytes());
            fileStream.close();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void setImage(String path, int row, int column, float offsetX, float offsetY, float rotation, float scale) {

        if (row < 0 || row >= LenticaConfig.MAX_ROWS) return;
        if (column < 0 || column >= LenticaConfig.MAX_COLUMNS) return;
        if (path == null) return;

        mImagePaths[row][column] = path;
        mOffsetX[row][column] = offsetX;
        mOffsetY[row][column] = offsetY;
        mRotation[row][column] = rotation;
        mScale[row][column] = scale;

        if (mListener != null)
            mListener.onUpdate();
    }

    public String getTexturePath(int row, int column) {

        if (row < 0 || row >= LenticaConfig.MAX_ROWS) return null;
        if (column < 0 || column >= LenticaConfig.MAX_COLUMNS) return null;


        return mImagePaths[row][column];
    }

    public float getOffsetX(int row, int column) {

        if (row < 0 || row >= LenticaConfig.MAX_ROWS) return 0.0f;
        if (column < 0 || column >= LenticaConfig.MAX_COLUMNS) return 0.0f;


        return mOffsetX[row][column];
    }

    public float getOffsetY(int row, int column) {

        if (row < 0 || row >= LenticaConfig.MAX_ROWS) return 0.0f;
        if (column < 0 || column >= LenticaConfig.MAX_COLUMNS) return 0.0f;


        return mOffsetY[row][column];
    }

    public float getRotation(int row, int column) {

        if (row < 0 || row >= LenticaConfig.MAX_ROWS) return 0.0f;
        if (column < 0 || column >= LenticaConfig.MAX_COLUMNS) return 0.0f;


        return mRotation[row][column];
    }

    public float getScale(int row, int column) {

        if (row < 0 || row >= LenticaConfig.MAX_ROWS) return 0.0f;
        if (column < 0 || column >= LenticaConfig.MAX_COLUMNS) return 0.0f;


        return mScale[row][column];
    }

    public void setFilter(int filter) {
        mFilter = filter;
    }

    public int getFilter() {
        return mFilter;
    }

    public void setLook(String look) {
        mLook = look;
        if (mListener != null)
            mListener.onUpdate();
    }

    public String getLook() {
        return mLook;
    }

    public void setBrightness(float brightness) {
        mBrightness = brightness;
    }

    public void setContrast(float contrast) {
        mContrast = contrast;
    }

    public void setSaturation(float saturation) {
        mSaturation = saturation;
    }

    public float getBrightness() {
        return mBrightness;
    }

    public float getContrast() {
        return mContrast;
    }

    public float getSaturation() {
        return mSaturation;
    }

    public void setListener(LenticularImageListener listener) {
        mListener = listener;
    }
}
