package com.example.zazzique.lentica;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zazzique on 22.10.2015.
 */
public class Image {
    private static final String TAG = Image.class.getName();

    public static Bitmap loadThumbnail(String path, int preferredWidth, int preferredHeight) {

        Bitmap bitmap = null;
        String pathArray[] = path.split("\\.");
        String extension = pathArray[pathArray.length - 1];

        int exifOrientation = ExifInterface.ORIENTATION_NORMAL;
        boolean rescaleImage = true;

        if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")){
            try {
                ExifInterface exif = new ExifInterface(path);

                exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                if (exif.hasThumbnail()){
                    byte[] thumbnail =  exif.getThumbnail();
                    bitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
                    rescaleImage = false;
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Error while loading image EXIF " + ioe.getMessage());
            }
        }

        if (rescaleImage && (preferredWidth > 0) && (preferredHeight > 0)) { // TODO: to method
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            options.inSampleSize = 1;

            if (options.outHeight > preferredHeight || options.outWidth > preferredWidth) {
                if (options.outWidth > options.outHeight) {
                    options.inSampleSize = Math.round((float)options.outHeight / (float)preferredHeight);
                } else {
                    options.inSampleSize = Math.round((float)options.outWidth / (float)preferredWidth);
                }
            }

            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFile(path, options);
        }

        if (bitmap == null) return null;

        if (exifOrientation != ExifInterface.ORIENTATION_NORMAL) {

            float rotation = 0.0f;

            switch(exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: rotation = 90.0f; break;
                case ExifInterface.ORIENTATION_ROTATE_180: rotation = 180.0f; break;
                case ExifInterface.ORIENTATION_ROTATE_270: rotation = 270.0f; break;
            }

            Matrix m = new Matrix();
            m.setRotate(rotation, (float)bitmap.getWidth() * 0.5f, (float)bitmap.getHeight() * 0.5f);

            try {
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (rotatedBitmap != bitmap) bitmap.recycle();

                return rotatedBitmap;

            } catch (Exception e) {
                Log.e(TAG, "Failed to rotate image" + e.getMessage());
            }
        }

        return bitmap;
    }

    public static Bitmap load(Context context, String path) {

        Bitmap bitmap = null;
        String pathArray[] = path.split("\\.");
        String extension = pathArray[pathArray.length - 1];

        int exifOrientation = ExifInterface.ORIENTATION_NORMAL;
        boolean rescaleImage = true;

        if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")){
            try {
                ExifInterface exif = new ExifInterface(path);
                exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            } catch (IOException ioe) {
                Log.e(TAG, "Error while loading image EXIF " + ioe.getMessage());
            }
        }

        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(path);
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (final IOException e) {
            bitmap = null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }

            if (bitmap == null) {
                bitmap = BitmapFactory.decodeFile(path);
            }
        }

        if (bitmap == null) return null;

        if (exifOrientation != ExifInterface.ORIENTATION_NORMAL) { // TODO: separate method

            float rotation = 0.0f;

            switch(exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: rotation = 90.0f; break;
                case ExifInterface.ORIENTATION_ROTATE_180: rotation = 180.0f; break;
                case ExifInterface.ORIENTATION_ROTATE_270: rotation = 270.0f; break;
            }

            Matrix m = new Matrix();
            m.setRotate(rotation, (float)bitmap.getWidth() * 0.5f, (float)bitmap.getHeight() * 0.5f);

            try {
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (rotatedBitmap != bitmap) bitmap.recycle();

                return rotatedBitmap;

            } catch (Exception e) {
                Log.e(TAG, "Failed to rotate image" + e.getMessage());
            }
        }

        return bitmap;
    }
}
