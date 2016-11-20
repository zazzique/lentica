package com.example.zazzique.lentica;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zazzique on 19.10.2015.
 */
public class LenticularView extends GLSurfaceView implements SensorEventListener {
    private static final String TAG = LenticularView.class.getName();

    private final SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private LenticularRenderer mRenderer;

    private static int SMOOTH_SIZE = 5;
    private float[] mRollSmooth = new float[SMOOTH_SIZE];
    private float[] mPitchSmooth = new float[SMOOTH_SIZE];

    private boolean mSetBias = true;
    private float mVerticalBias = 0.0f;

    private boolean mPositionLock = false;

    public LenticularView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (mAccelerometer == null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        /*List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
            Log.d(TAG, "Sensor " + sensor.getType());
        }*/


        mRenderer = new LenticularRenderer(context);
        setEGLContextClientVersion(2);
        setRenderer(mRenderer);
        //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        for (int i = 0; i < SMOOTH_SIZE; i++) {
            mRollSmooth[i] = 0.0f;
            mPitchSmooth[i] = 0.0f;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mRenderer.close();
        super.surfaceDestroyed(holder);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        setMeasuredDimension(width, width);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if ((event.sensor.getType() != Sensor.TYPE_GRAVITY) && (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER))
            return;

        float rollK = -LenticaConfig.SENSITIVITY_HORIZONTAL * event.values[0] / SensorManager.GRAVITY_EARTH;
        float pitchK = LenticaConfig.SENSITIVITY_VERTICAL * event.values[1] / SensorManager.GRAVITY_EARTH;

        if (mSetBias) {
            mVerticalBias = pitchK;
            mSetBias = false;
        }

        pitchK -= mVerticalBias;

        for (int i = 0; i < SMOOTH_SIZE - 1; i++) {
            mRollSmooth[i] = mRollSmooth[i + 1];
            mPitchSmooth[i] = mPitchSmooth[i + 1];
        }

        mRollSmooth[SMOOTH_SIZE - 1] = rollK;
        mPitchSmooth[SMOOTH_SIZE - 1] = pitchK;

        rollK = 0.0f;
        pitchK = 0.0f;

        for (int i = 0; i < SMOOTH_SIZE; i++) {
            rollK += mRollSmooth[i];
            pitchK += mPitchSmooth[i];
        }

        rollK /= (float) SMOOTH_SIZE;
        pitchK /= (float) SMOOTH_SIZE;

        if (rollK < -1.0f) {
            rollK = -1.0f;
        } else if (rollK > 1.0f) {
            rollK = 1.0f;
        }

        if (pitchK < -1.0f) {
            pitchK = -1.0f;
        } else if (pitchK > 1.0f) {
            pitchK = 1.0f;
        }

        if (!mPositionLock) {
            mRenderer.setKoefs(rollK, pitchK);
        }

        //Log.d(TAG, "Values: " + event.values[0] + " " + event.values[1] + " " + event.values[2]);
    }

    public void startSensors() {
        if (mAccelerometer != null) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
            mSetBias = true;
        }
    }

    public void stopSensors() {
        mSensorManager.unregisterListener(this);
    }

    public void setLenticularImage(LenticularImage lenticularImage) {

        mRenderer.setLenticularImage(lenticularImage);
    }

    public void setLock(boolean lock) {
        mPositionLock = lock;
    }

    public boolean getLock() {
        return mPositionLock;
    }

    public LenticularImage getLenticularImage() {
        return mRenderer.getLenticularImage();
    }
}
