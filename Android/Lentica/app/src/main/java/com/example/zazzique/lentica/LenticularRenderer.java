package com.example.zazzique.lentica;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zazzique on 19.10.2015.
 */

class LenticularImageCache {
    public ArrayList<String>textures = new ArrayList<String>();
    public int[][] indices = new int[LenticaConfig.MAX_ROWS][LenticaConfig.MAX_COLUMNS];
    public int minX, maxX;
    public int minY, maxY;

    public LenticularImageCache () {
        for (int i = 0; i < LenticaConfig.MAX_ROWS; i++) {
            for (int j = 0; j < LenticaConfig.MAX_COLUMNS; j++) {
                indices[i][j] = -1;
            }
        }
    }

    public void calculate(LenticularImage lenticularImage) {

        textures.clear();

        minX = LenticaConfig.MAX_COLUMNS;
        maxX = 0;
        minY = LenticaConfig.MAX_ROWS;
        maxY = 0;

        for (int i = 0; i < LenticaConfig.MAX_ROWS; i++) {
            for (int j = 0; j < LenticaConfig.MAX_COLUMNS; j++) {
                indices[i][j] = -1;

                String newTexture = lenticularImage.getTexturePath(i, j);
                if (newTexture == null) continue;

                minX = Math.min(minX, j);
                maxX = Math.max(maxX, j);
                minY = Math.min(minY, i);
                maxY = Math.max(maxY, i);

                boolean addTexture = true;
                int iterator = 0;
                for (String texture : textures) {
                    if (newTexture.equalsIgnoreCase(texture)) {
                        addTexture = false;
                        break;
                    }

                    iterator ++;
                }

                if (addTexture) {
                    textures.add(newTexture);
                    indices[i][j] = textures.size() - 1;
                } else {
                    indices[i][j] = iterator;
                }
            }
        }
    }
}

public class LenticularRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = LenticularRenderer.class.getName();

    private Context mContext;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTcBuffer;
    private int mShaderSimple;

    private Texture[] mTextures = new Texture[LenticaConfig.MAX_ROWS * LenticaConfig.MAX_COLUMNS];
    private Texture mLookTexture = new Texture();

    private LenticularImage mLenticularImage = null;
    private LenticularImageCache mLenticularImageCache = new LenticularImageCache();

    private float mRollK = 0.0f;
    private float mPitchK = 0.0f;

    private final String mVertexShaderSimple = "\n" +
            "attribute vec2 aPos;\n" +
            "attribute vec2 aTC;\n" +
            "uniform mat2 uTcmLL;\n" +
            "uniform mat2 uTcmLR;\n" +
            "uniform mat2 uTcmUL;\n" +
            "uniform mat2 uTcmUR;\n" +
            "uniform vec2 uOfsLL;\n" +
            "uniform vec2 uOfsLR;\n" +
            "uniform vec2 uOfsUL;\n" +
            "uniform vec2 uOfsUR;\n" +
            "varying vec2 tc;\n" +
            "varying vec2 tcLL;\n" +
            "varying vec2 tcLR;\n" +
            "varying vec2 tcUL;\n" +
            "varying vec2 tcUR;\n" +
            "void main() {\n" +
            "  tcLL = (aTC - 0.5 + uOfsLL) * uTcmLL;\n" +
            "  tcLL += 0.5;\n" +
            "  tcLR = (aTC - 0.5 + uOfsLR) * uTcmLR;\n" +
            "  tcLR += 0.5;\n" +
            "  tcUL = (aTC - 0.5 + uOfsUL) * uTcmUL;\n" +
            "  tcUL += 0.5;\n" +
            "  tcUR = (aTC - 0.5 + uOfsUR) * uTcmUR;\n" +
            "  tcUR += 0.5;\n" +
            "  tc = aTC;\n" +
            "  gl_Position = vec4(aPos.x, aPos.y, 0.0, 1.0);\n" +
            "}";

    private final String mFragmentShaderSimple = "precision mediump float;\n" +
            "uniform sampler2D texLL;\n" +
            "uniform sampler2D texLR;\n" +
            "uniform sampler2D texUL;\n" +
            "uniform sampler2D texUR;\n" +
            "uniform float xK;\n" +
            "uniform float yK;\n" +
            "varying vec2 tc;\n" +
            "varying vec2 tcLL;\n" +
            "varying vec2 tcLR;\n" +
            "varying vec2 tcUL;\n" +
            "varying vec2 tcUR;\n" +
            "void main() {\n" +
            "  vec4 colorL = texture2D(texLL, tcLL) * (1.0 - xK) + texture2D(texLR, tcLR) * (xK);\n" +
            "  vec4 colorU = texture2D(texUL, tcUL) * (1.0 - xK) + texture2D(texUR, tcUR) * (xK);\n" +
            "  gl_FragColor = colorL * (1.0 - yK) + colorU * (yK);\n" +
            "}";

    private final String mFragmentShaderInterlace = "precision mediump float;\n" +
            "uniform sampler2D texLL;\n" +
            "uniform sampler2D texLR;\n" +
            "uniform sampler2D texUL;\n" +
            "uniform sampler2D texUR;\n" +
            "uniform sampler2D texLut;\n" +
            "uniform float xK;\n" +
            "uniform float yK;\n" +
            "varying vec2 tc;\n" +
            "varying vec2 tcLL;\n" +
            "varying vec2 tcLR;\n" +
            "varying vec2 tcUL;\n" +
            "varying vec2 tcUR;\n" +
            "uniform float xShift;\n" +
            "uniform float brightness;\n" +
            "uniform float contrast;\n" +
            "uniform float saturation;\n" +
            "const float lensCount = 240.0;\n" +
            "vec3 rgb2hsv(vec3 c)\n" +
            "{\n" +
            "    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n" +
            "    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n" +
            "    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n" +
            "\n" +
            "    float d = q.x - min(q.w, q.y);\n" +
            "    float e = 1.0e-10;\n" +
            "    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n" +
            "}\n" +
            "vec3 hsv2rgb(vec3 c)\n" +
            "{\n" +
            "    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n" +
            "    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n" +
            "    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n" +
            "}\n" +
            "void main() {\n" +
            "  // Blend\n" +
            "  float lens_kX = (tc.s - 0.5) * 0.35;\n" +
            "  lens_kX *= abs(sin((tc.s * lensCount) * 3.1416 * 1.0));\n" +
            "  lens_kX += xK;\n" +
            "  lens_kX = clamp(lens_kX, 0.0, 1.0);\n" +
            "  float lens_kY = (tc.t - 0.5) * 0.35;\n" +
            "  lens_kY *= abs(sin((tc.t * lensCount) * 3.1416 * 1.0));\n" +
            "  lens_kY += yK;\n" +
            "  lens_kY = clamp(lens_kY, 0.0, 1.0);\n" +

            "  vec2 ltcLL = vec2(tcLL.s + (xShift * 2.0 / lensCount), tcLL.t);\n" +
            "  vec2 ltcLR = vec2(tcLR.s + (xShift * 2.0 / lensCount), tcLR.t);\n" +
            "  vec2 ltcUL = vec2(tcUL.s + (xShift * 2.0 / lensCount), tcUL.t);\n" +
            "  vec2 ltcUR = vec2(tcUR.s + (xShift * 2.0 / lensCount), tcUR.t);\n" +
            "  vec4 colorL = texture2D(texLL, ltcLL) * (1.0 - lens_kX) + texture2D(texLR, ltcLR) * (lens_kX);\n" +
            "  vec4 colorU = texture2D(texUL, ltcUL) * (1.0 - lens_kX) + texture2D(texUR, ltcUR) * (lens_kX);\n" +
            "  vec4 color = colorL * (1.0 - lens_kY) + colorU * (lens_kY);\n" +

            /*"  // color correction \n" +
            "  vec3 hsv = rgb2hsv(color.rgb);\n" +
            "  hsv.b *= brightness;\n" +
            "  hsv.g *= saturation;\n" +
            "  hsv = clamp(hsv, 0.0, 1.0);\n" +
            "  color.rgb = hsv2rgb(hsv);\n" +*/

            "  color.rgb = (color.rgb - 0.5) * contrast + 0.5;\n" +
            "  color.rgb += brightness;\n" +
            "  float l = dot(color.rgb, vec3(0.299, 0.587, 0.114));\n" +
            "  color.rgb = mix(vec3(l, l, l), color.rgb, saturation);\n" +
            "  color.rgb = clamp(color.rgb, 0.0, 1.0);\n" +

            "  // LUT \n" +
            "  vec2 tcLut = vec2(floor(color.b * 15.0) / 16.0 + (color.r * 15.0) / 256.0, (color.g * 15.0) / 16.0);\n" +
            "  tcLut.s += 0.5 / 256.0;\n" +
            "  tcLut.t += 0.5 / 16.0;\n" +
            "  float bK = (color.b * 15.0) - floor(color.b * 15.0);\n" +
            "  color.rgb = mix(texture2D(texLut, tcLut).rgb, texture2D(texLut, vec2(tcLut.s + (1.0 / 16.0), tcLut.t)).rgb, bK);\n" +

            "  gl_FragColor = color;\n" +
            " // Specular\n" +
            "  gl_FragColor.rgb += abs(lens_kX - 0.5) * 0.1;\n" +
            "  //gl_FragColor.rgb += abs(lens_kY - 0.5) * 0.1;\n" +
            "}";

    public LenticularRenderer(Context context) {

        mContext = context;

        for (int i = 0; i < LenticaConfig.MAX_ROWS * LenticaConfig.MAX_COLUMNS; i++) {
            mTextures[i] = new Texture();
        }

        float[] vtmp = { 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f };
        float[] tctmp = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
        mVertexBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer.put(vtmp);
        mVertexBuffer.position(0);
        mTcBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTcBuffer.put(tctmp);
        mTcBuffer.position(0);

        // TODO: load all look textures
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        mShaderSimple = Shaders.init(mVertexShaderSimple, mFragmentShaderInterlace);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int w, int h) {
        //
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (mLenticularImage == null) return;

        if (mLenticularImageCache.textures.size() <= 0) return;

        float x = (((float)LenticaConfig.MAX_COLUMNS - 1.0f) / 2.0f) * (mRollK + 1.0f);
        float y = (((float)LenticaConfig.MAX_ROWS - 1.0f) / 2.0f) * (mPitchK + 1.0f);

        int iX = (int)x;
        int iY = (int)y;

        if ((mLenticularImageCache.minX > mLenticularImageCache.maxX) ||
                (mLenticularImageCache.minY > mLenticularImageCache.maxY)) return;

        if (iX < mLenticularImageCache.minX) iX = mLenticularImageCache.minX;
        if (iY < mLenticularImageCache.minY) iY = mLenticularImageCache.minY;
        if (iX > mLenticularImageCache.maxX - 1) iX = mLenticularImageCache.maxX - 1;
        if (iY > mLenticularImageCache.maxY - 1) iY = mLenticularImageCache.maxY - 1;

        float xK = x - iX;
        float yK = y - iY;

        float scale = 1.0f / (1.0f - (LenticaConfig.THRESHHOLD * 2.0f));
        xK *= scale;
        xK -= LenticaConfig.THRESHHOLD;
        if (xK < 0.0f) xK = 0.0f;
        if (xK > 1.0f) xK = 1.0f;

        yK *= scale;
        yK -= LenticaConfig.THRESHHOLD;
        if (yK < 0.0f) yK = 0.0f;
        if (yK > 1.0f) yK = 1.0f;

        //Log.d(TAG, "Ints: " + iX + " " + iY + "    K: " + xK + " " + yK);

        int texIndexLL = mLenticularImageCache.indices[iY][iX];
        int texIndexLR = mLenticularImageCache.indices[iY][iX + 1];
        int texIndexUL = mLenticularImageCache.indices[iY + 1][iX];
        int texIndexUR = mLenticularImageCache.indices[iY + 1][iX + 1];

        if ((texIndexLL < 0) && (texIndexLR < 0) && (texIndexUL < 0) && (texIndexUR < 0)) {
            return;
        }

        if (texIndexLL < 0) {
            if (texIndexLR < 0) {
                yK = 1.0f;
            }
        }

        if (texIndexUL < 0) {
            if (texIndexUR < 0) {
                yK = 0.0f;
            }
        }

        if (texIndexLL < 0) {
            if (texIndexUL < 0) {
                xK = 1.0f;
            }
        }

        if (texIndexLR < 0) {
            if (texIndexUR < 0) {
                xK = 0.0f;
            }
        }

        if (texIndexLL < 0) texIndexLL = 0;
        if (texIndexLR < 0) texIndexLR = 0;
        if (texIndexUL < 0) texIndexUL = 0;
        if (texIndexUR < 0) texIndexUR = 0;

        float xShift = mRollK;

        synchronized(this) {
            for (int i = 0; i < LenticaConfig.MAX_ROWS * LenticaConfig.MAX_COLUMNS; i++) {
                mTextures[i].update(mContext);
            }
            mLookTexture.update(mContext);
        }

        if (!mTextures[texIndexLL].isValid()) return;
        if (!mTextures[texIndexLR].isValid()) return;
        if (!mTextures[texIndexUL].isValid()) return;
        if (!mTextures[texIndexUR].isValid()) return;
        if (!mLookTexture.isValid()) return;

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthMask(false);

        int shader = mShaderSimple;

        switch (mLenticularImage.getFilter())
        {
            case LenticularImage.FILTER_SIMPLE:
                shader = mShaderSimple;
                break;
        }

        GLES20.glUseProgram(shader);

        int aPos = GLES20.glGetAttribLocation(shader, "aPos");
        int aTc = GLES20.glGetAttribLocation(shader, "aTC");

        int uTcmLL = GLES20.glGetUniformLocation(shader, "uTcmLL");
        int uTcmLR = GLES20.glGetUniformLocation(shader, "uTcmLR");
        int uTcmUL = GLES20.glGetUniformLocation(shader, "uTcmUL");
        int uTcmUR = GLES20.glGetUniformLocation(shader, "uTcmUR");

        int uOfsLL = GLES20.glGetUniformLocation(shader, "uOfsLL");
        int uOfsLR = GLES20.glGetUniformLocation(shader, "uOfsLR");
        int uOfsUL = GLES20.glGetUniformLocation(shader, "uOfsUL");
        int uOfsUR = GLES20.glGetUniformLocation(shader, "uOfsUR");

        int uTexLL = GLES20.glGetUniformLocation(shader, "texLL");
        int uTexLR = GLES20.glGetUniformLocation(shader, "texLR");
        int uTexUL = GLES20.glGetUniformLocation(shader, "texUL");
        int uTexUR = GLES20.glGetUniformLocation(shader, "texUR");

        int uTexLut = GLES20.glGetUniformLocation(shader, "texLut");

        int uXK = GLES20.glGetUniformLocation(shader, "xK");
        int uYK = GLES20.glGetUniformLocation(shader, "yK");
        int uXShift = GLES20.glGetUniformLocation(shader, "xShift"); // TODO: maybe use matrix too

        int uBrightness = GLES20.glGetUniformLocation(shader, "brightness");
        int uContrast = GLES20.glGetUniformLocation(shader, "contrast");
        int uSaturation = GLES20.glGetUniformLocation(shader, "saturation");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); // TODO: check for
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[texIndexLL].getGlTexture()[0]);
        GLES20.glUniform1i(uTexLL, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[texIndexLR].getGlTexture()[0]);
        GLES20.glUniform1i(uTexLR, 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[texIndexUL].getGlTexture()[0]);
        GLES20.glUniform1i(uTexUL, 2);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[texIndexUR].getGlTexture()[0]);
        GLES20.glUniform1i(uTexUR, 3);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mLookTexture.getGlTexture()[0]); // TODO: check if available and only then render
        GLES20.glUniform1i(uTexLut, 4);

        GLES20.glUniform1f(uXK, xK);
        GLES20.glUniform1f(uYK, yK);
        GLES20.glUniform1f(uXShift, xShift);

        GLES20.glUniform1f(uBrightness, mLenticularImage.getBrightness());
        GLES20.glUniform1f(uContrast, mLenticularImage.getContrast());
        GLES20.glUniform1f(uSaturation, mLenticularImage.getSaturation());

        float mLL[] = calculateMatrix(mLenticularImage.getOffsetX(iY, iX),
                mLenticularImage.getOffsetY(iY, iX),
                mLenticularImage.getRotation(iY, iX),
                mLenticularImage.getScale(iY, iX),
                mTextures[0].getAspectRatio()); // TODO: get proper aspect ratio

        float mLR[] = calculateMatrix(mLenticularImage.getOffsetX(iY, iX + 1),
                mLenticularImage.getOffsetY(iY, iX + 1),
                mLenticularImage.getRotation(iY, iX + 1),
                mLenticularImage.getScale(iY, iX + 1),
                mTextures[0].getAspectRatio());

        float mUL[] = calculateMatrix(mLenticularImage.getOffsetX(iY + 1, iX),
                mLenticularImage.getOffsetY(iY + 1, iX),
                mLenticularImage.getRotation(iY + 1, iX),
                mLenticularImage.getScale(iY + 1, iX),
                mTextures[0].getAspectRatio());

        float mUR[] = calculateMatrix(mLenticularImage.getOffsetX(iY + 1, iX + 1),
                mLenticularImage.getOffsetY(iY + 1, iX + 1),
                mLenticularImage.getRotation(iY + 1, iX + 1),
                mLenticularImage.getScale(iY + 1, iX + 1),
                mTextures[0].getAspectRatio());

        GLES20.glUniformMatrix2fv(uTcmLL, 1, false, mLL, 0);
        GLES20.glUniformMatrix2fv(uTcmLR, 1, false, mLR, 0);
        GLES20.glUniformMatrix2fv(uTcmUL, 1, false, mUL, 0);
        GLES20.glUniformMatrix2fv(uTcmUR, 1, false, mUR, 0);

        GLES20.glUniform2f(uOfsLL, mLL[4], mLL[5]);
        GLES20.glUniform2f(uOfsLR, mLR[4], mLR[5]);
        GLES20.glUniform2f(uOfsUL, mUL[4], mUL[5]);
        GLES20.glUniform2f(uOfsUR, mUR[4], mUR[5]);

        /*GLES20.glVertexAttrib1f(aAspectRatio, mTextures[0].getAspectRatio());
        GLES20.glVertexAttrib2f(aOffset, mLenticularImage.getOffsetX(iY, iX), mLenticularImage.getOffsetY(iY, iX));
        GLES20.glVertexAttrib1f(aRot, mLenticularImage.getRotation(iY, iX)); // TODO: wrong
        GLES20.glVertexAttrib1f(aScale, mLenticularImage.getScale(iY, iX));*/

        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 4 * 2, mVertexBuffer);
        GLES20.glVertexAttribPointer(aTc, 2, GLES20.GL_FLOAT, false, 4 * 2, mTcBuffer);
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aTc);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glFlush();
    }

    private float[] calculateMatrix(float positionX, float positionY, float rotation, float scale, float aspectRatio) {

        float scaleX = scale;
        float scaleY = scale;
        if (aspectRatio > 1.0) {
            scaleX /= aspectRatio;
        } else if (aspectRatio > 0.05) {
            scaleY *= aspectRatio;
        }

        //float m[] = new float[6];

        /*Matrix.setIdentityM(m, 0);
        Matrix.rotateM(m, 0, rotation, 0.0f, 0.0f, 1.0f);
        Matrix.scaleM(m, 0, scaleX, scaleY, 1.0f);
        Matrix.translateM(m, 0, positionX, positionY, 0.0f);*/

        float sa = (float)Math.sin(rotation);
        float ca = (float)Math.cos(rotation);
        float m[] = new float[] { scaleX * ca,  sa,  -sa, scaleY * ca, positionX, positionY };

        /*"  float scaleX = 1.0;\n" +
                "  float scaleY = 1.0;\n" +
                "  if (aspectRatio > 1.0) {\n" +
                "    scaleX *= aspectRatio;\n" +
                "  } else if (aspectRatio > 0.05) {\n" +
                "    scaleY /= aspectRatio;\n" +
                "  }\n" +
                "  vec2 p = vec2(aPos.x * scaleX, aPos.y * scaleY);\n" +*/

        return m;
    }

    public void close() {
        for (int i = 0; i < LenticaConfig.MAX_ROWS * LenticaConfig.MAX_COLUMNS; i++) {
            mTextures[i].delete();
        }
    }

    public void setLenticularImage(LenticularImage lenticularImage) {

        mLenticularImage = lenticularImage;
        mLenticularImage.setListener(mLenticularImageListener);

        updateLenticularImage();
    }

    public LenticularImage getLenticularImage() {
        return mLenticularImage;
    }

    LenticularImage.LenticularImageListener mLenticularImageListener = new LenticularImage.LenticularImageListener() {

        @Override
        public void onUpdate() {
            updateLenticularImage();
        }
    };

    private void updateLenticularImage() {
        mLenticularImageCache.calculate(mLenticularImage);

        for (int i = 0; i < LenticaConfig.MAX_ROWS * LenticaConfig.MAX_COLUMNS; i++) {
            if (i < mLenticularImageCache.textures.size()) {
                mTextures[i].setFile(mLenticularImageCache.textures.get(i));
            } else {
                mTextures[i].delete();
            }
        }

        String lookTexturePath = "lut_" + mLenticularImage.getLook() + ".png";
        mLookTexture.setFile(lookTexturePath);
    }

    public void setKoefs(float rollK, float pitchK) {
        mRollK = rollK;
        mPitchK = pitchK;
    }
}
