package com.example.zazzique.lentica;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

/**
 * Created by zazzique on 19.10.2015.
 */
public class Texture {
    String mImagePath = null;
    private int[] mTexture;
    float mAspectRatio = 1.0f;
    boolean mNeedUpdate = false;
    boolean mValid = false;

    public void setFile(String path) {
        if (path == null) {
            mValid = false;
            return;
        } else if (path.equalsIgnoreCase(mImagePath) && mValid) {
            return;
        } else {
            mImagePath = path;
            mNeedUpdate = true;
        }
    }

    public void update(Context context) {
        if (mNeedUpdate && mImagePath != null) {
            delete();
            loadFromFile(context, mImagePath);
        }
        mNeedUpdate = false;
    }

    public int[] getGlTexture() {
        return mTexture;
    }

    public float getAspectRatio() {
        return mAspectRatio;
    }

    public boolean isValid() {
        return mValid;
    }

    private void loadFromFile(Context context, String path) {
        Bitmap bitmap = Image.load(context, path);

        mAspectRatio = 1.0f;

        if (bitmap != null) {
            if (bitmap.getWidth() > 0 && bitmap.getHeight() > 0) {
                mAspectRatio = (float) bitmap.getWidth() / (float) bitmap.getHeight();
            }
            mTexture = new int[1];
            GLES20.glGenTextures(1, mTexture, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, 0);
            bitmap.recycle();

            mValid = true;
        }
    }

    public void delete() {
        if (mTexture != null) {
            GLES20.glDeleteTextures(1, mTexture, 0);
            mTexture = null;
        }

        mValid = false;
    }
}
