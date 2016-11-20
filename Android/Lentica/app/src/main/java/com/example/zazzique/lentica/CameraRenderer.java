package com.example.zazzique.lentica;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zazzique on 06.10.2015.
 */
public class CameraRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = CameraRenderer.class.getName();

    private Activity mActivity;
    private CameraView mView;
    private CameraWrapper mCameraWrapper;

    private SurfaceTexture mSurfaceTexture;
    private boolean mUpdateTexture = false;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTcBuffer;
    private int mShader;
    private int[] mTexture;
    private Texture mOverlayTexture;
    private int mOverlayShader;

    // TODO: put in assets or resources
    private final String vss = "attribute vec2 aPos;\n" +
            "attribute vec2 aTC;\n" +
            "attribute float aspectRatio;\n" +
            "attribute float aRot;\n" +
            "varying vec2 tc;\n" +
            "void main() {\n" +
            "  tc = aTC;\n" +
            "  float scaleX = 1.0;\n" +
            "  float scaleY = 1.0;\n" +
            "  if (aspectRatio > 1.0) {\n" +
            "    scaleX *= aspectRatio;\n" +
            "  } else if (aspectRatio > 0.05) {\n" +
            "    scaleY /= aspectRatio;\n" +
            "  }\n" +
            "  vec2 p = vec2(aPos.x * scaleX, aPos.y * scaleY);\n" +
            "  gl_Position.x = cos(aRot) * p.x - sin(aRot) * p.y;\n" +
            "  gl_Position.y = sin(aRot) * p.x + cos(aRot) * p.y;\n" +
            "  gl_Position.z = 0.0;\n" +
            "  gl_Position.w = 1.0;\n" +
            "}";

    private final String fss = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES tex;\n" +
            "varying vec2 tc;\n" +
            "void main() {\n" +
            "  gl_FragColor.rgb = texture2D(tex,tc).rgb;\n" +
            "  gl_FragColor.a = 1.0;\n" +
            "}";

    /*private final String ofss = "precision mediump float;\n" +
            "uniform sampler2D tex;\n" +
            "uniform float alpha;\n" +
            "varying vec2 tc;\n" +
            "void main() {\n" +
            "  gl_FragColor.rgb = texture2D(tex,tc).rgb;\n" +
            "  gl_FragColor.a = 0.45 * texture2D(tex,tc).r + (0.25 * alpha);\n" +
            "}";*/

    private final String ofss = "precision mediump float;\n" +
            "uniform sampler2D tex;\n" +
            "uniform float alpha;\n" +
            "varying vec2 tc;\n" +
            "float step = 1.0 / 512.0;\n" +
            "void main() {\n" +
            "  gl_FragColor.rgb = texture2D(tex,tc).rgb;\n" +
            "vec4 color_center = texture2D(tex, vec2(tc.s, tc.t));\n" +
            "vec4 color_left = texture2D(tex, vec2(tc.s - step, tc.t));\n" +
            "vec4 color_right = texture2D(tex, vec2(tc.s + step, tc.t));\n" +
            "vec4 color_up = texture2D(tex, vec2(tc.s, tc.t + step));\n" +
            "vec4 color_down = texture2D(tex, vec2(tc.s, tc.t + step));\n" +
            "vec4 color = color_center * 5.0;\n" +
            "color += color_left * -1.0;\n" +
            "color += color_right * -1.0;\n" +
            "color += color_up * -1.0;\n" +
            "color += color_down * -1.0;\n" +
            "color = clamp(color, 0.0, 1.0);\n" +
            "float l = (color.r + color.g + color.b) * 0.33;\n" +
            "  gl_FragColor.a = l;\n" +
            "}";

    // TODO: send aspect ratio to shader
    /*private final String ofss = "precision mediump float;\n" +
            "float step = 1.0 / 512.0;\n" +
            "uniform sampler2D tex;\n" +
            "varying vec2 tc;\n" +
            "void main() {\n" +
            "vec4 color_center = texture2D(tex, vec2(tc.s, tc.t));\n" +
            "vec4 color_left = texture2D(tex, vec2(tc.s - step, tc.t));\n" +
            "vec4 color_right = texture2D(tex, vec2(tc.s + step, tc.t));\n" +
            "vec4 color_up = texture2D(tex, vec2(tc.s, tc.t + step));\n" +
            "vec4 color_down = texture2D(tex, vec2(tc.s, tc.t + step));\n" +
            "vec4 color = color_center * 8.0;\n" +
            "color += color_left * -2.0;\n" +
            "color += color_right * -2.0;\n" +
            "color += color_up * -2.0;\n" +
            "color += color_down * -2.0;\n" +
            "color = clamp(color, 0.0, 1.0);\n" +
            "float l = (color.r + color.g + color.b) * 0.33;\n" +
            "color.r = 1.0;\n" +
            "color.g = 0.0;\n" +
            "color.b = 0.0;\n" +
            "color.a = l;\n" +
            "gl_FragColor = color;\n" +
            "}";*/


    CameraRenderer(Activity activity, CameraView view) {
        mActivity = activity;
        mView = view;
        mCameraWrapper = new CameraWrapper(activity);

        mOverlayTexture = new Texture();

        float[] vtmp = { 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f };
        float[] tctmp = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
        mVertexBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer.put(vtmp);
        mVertexBuffer.position(0);
        mTcBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTcBuffer.put(tctmp);
        mTcBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        initTexture();
        mSurfaceTexture = new SurfaceTexture(mTexture[0]);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mCameraWrapper.open();

        mCameraWrapper.setPreviewTexture(mSurfaceTexture);

        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);

        mShader = Shaders.init(vss, fss);
        mOverlayShader = Shaders.init(vss, ofss);
    }

    @Override
    public void onSurfaceChanged (GL10 unused, int width, int height) {
        mCameraWrapper.stopPreview();
        mCameraWrapper.startPreview(width, height, 96, 96, 1024, 1024); // TODO: better params
    }

    @Override
    public void onDrawFrame(GL10 unused) {

        int orientation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int rotation = 0;
        switch (orientation) {
            case Surface.ROTATION_0: rotation = -90; break;
            case Surface.ROTATION_90: rotation = 0; break;
            case Surface.ROTATION_180: rotation = -270; break;
            case Surface.ROTATION_270: rotation = 180; break;
        }


        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthMask(false);

        synchronized(this) {
            if (mUpdateTexture) {
                mSurfaceTexture.updateTexImage();
                mUpdateTexture = false;
            }

            mOverlayTexture.update(mActivity);
        }

        GLES20.glUseProgram(mShader);

        int aPos = GLES20.glGetAttribLocation(mShader, "aPos");
        int aTc = GLES20.glGetAttribLocation(mShader, "aTC");
        int aRot = GLES20.glGetAttribLocation(mShader, "aRot");
        int aAspectRatio = GLES20.glGetAttribLocation(mShader, "aspectRatio");
        int tex = GLES20.glGetUniformLocation(mShader, "tex");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture[0]);
        GLES20.glUniform1i(tex, 0);

        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 4 * 2, mVertexBuffer);
        GLES20.glVertexAttribPointer(aTc, 2, GLES20.GL_FLOAT, false, 4 * 2, mTcBuffer);
        GLES20.glVertexAttrib1f(aRot, (float)Math.toRadians(rotation));
        GLES20.glVertexAttrib1f(aAspectRatio, mCameraWrapper.getPreviewAspectRatio());
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aTc);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        if (mOverlayTexture.isValid()) {
            GLES20.glUseProgram(mOverlayShader);

            aPos = GLES20.glGetAttribLocation(mOverlayShader, "aPos");
            aTc = GLES20.glGetAttribLocation(mOverlayShader, "aTC");
            aRot = GLES20.glGetAttribLocation(mShader, "aRot");
            aAspectRatio = GLES20.glGetAttribLocation(mShader, "aspectRatio");
            tex = GLES20.glGetUniformLocation(mOverlayShader, "tex");
            int alpha = GLES20.glGetUniformLocation(mOverlayShader, "alpha");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOverlayTexture.getGlTexture()[0]);
            GLES20.glUniform1i(tex, 0);

            //long time = System.currentTimeMillis();
            //double timeK = (Math.sin((double)time / 1000.0 * 3.0) + 1.0) * 0.5;
            GLES20.glUniform1f(alpha, 1.0f);
            GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 4 * 2, mVertexBuffer);
            GLES20.glVertexAttribPointer(aTc, 2, GLES20.GL_FLOAT, false, 4 * 2, mTcBuffer);
            GLES20.glVertexAttrib1f(aRot, 0.0f);
            GLES20.glVertexAttrib1f(aAspectRatio, mOverlayTexture.getAspectRatio());
            GLES20.glEnableVertexAttribArray(aPos);
            GLES20.glEnableVertexAttribArray(aTc);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        GLES20.glFlush();
    }

    @Override
    public synchronized void onFrameAvailable ( SurfaceTexture st ) {
        mUpdateTexture = true;
        mView.requestRender();
    }

    public void close() {
        mCameraWrapper.close();
        mUpdateTexture = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mSurfaceTexture.release();
        }
        deleteTexture();
        mOverlayTexture.delete();
    }

    private void initTexture() {
        mTexture = new int[1];
        GLES20.glGenTextures(1, mTexture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    }

    private void deleteTexture() {
        if (mTexture != null) {
            GLES20.glDeleteTextures(1, mTexture, 0);
        }
    }

    public void setOverlayTexture(String path) {
        mOverlayTexture.setFile(path);
    }

    public CameraWrapper getCameraWrapper() {
        return mCameraWrapper;
    }
}
