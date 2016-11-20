package com.example.zazzique.lentica;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("deprecation")

// TODO: delete all rotations

/**
 * Created by zzq on 12.10.2015.
 */
public class CameraWrapper {
    private static final String TAG = CameraWrapper.class.getName();

    public static final int FLASH_AUTO = -1;
    public static final int FLASH_OFF = 0;
    public static final int FLASH_ON = 1;

    private Camera mCamera = null;
    private int mCurrentCameraIndex = 0;
    private int mCamerasCount = 0;

    private Activity mActivity = null;
    private int mCameraPreviewWidth = 0;
    private int mCameraPreviewHeight = 0;
    private int mCameraPictureWidth = 0;
    private int mCameraPictureHeight = 0;
    private int mCameraThumbnailWidth = 0;
    private int mCameraThumbnailHeight = 0;

    private SurfaceTexture mSurfaceTexture = null;

    private CameraWrapperListener mListener = null;

    public interface CameraWrapperListener {
        void onPictureTaken(String imagePath);
    }

    CameraWrapper(Activity activity) {

        mActivity = activity;

        mCamerasCount = Camera.getNumberOfCameras();

        for (int i = 0; i < mCamerasCount; i ++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);

            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCurrentCameraIndex = i;
                break;
            }
        }
    }

    public void setListener (CameraWrapperListener listener) {
        mListener = listener;
    }

    public float getPreviewAspectRatio() {
        if (mCameraPreviewWidth > 0 && mCameraPreviewHeight > 0) {
            return (float) mCameraPreviewWidth / (float) mCameraPreviewHeight;
        } else {
            return 1.0f;
        }
    }

    public void open() {
        try {
            //if (mCamera == null) {
                mCamera = Camera.open(mCurrentCameraIndex);
            //}
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't open a camera " + e.getMessage());
            return;
        }
    }

    private void setPreferredParams(int previewWidth, int previewHeight,
                                    int thumbnailWidth, int thumbnailHeight,
                                    int pictureWidth, int pictureHeight) {

        int[] size;

        Camera.Parameters parameters = mCamera.getParameters();

        // Preview size
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        size = getOptimalSize(previewSizes, previewWidth, previewHeight);
        mCameraPreviewWidth = size[0];
        mCameraPreviewHeight = size[1];

        parameters.setPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);

        // Antibanding
        if (parameters.getAntibanding() != null) { // TODO: get supported for all
            parameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
        }

        // Color effect
        if (parameters.getColorEffect() != null) {
            parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
        }

        // Exposure compensation
        if (parameters.getMinExposureCompensation() == 0 && parameters.getMaxExposureCompensation() == 0) {
            parameters.setExposureCompensation(0);
        }

        //Flash
        if (parameters.getFlashMode() != null) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        }

        // Focus
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        // Scene mode
        if (parameters.getSceneMode() != null) {
            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        }

        // White balance
        if (parameters.getWhiteBalance() != null) {
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }

        // Zoom
        if (parameters.isZoomSupported()) {
            parameters.setZoom(0);
        }

        // Picture
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setJpegQuality(100);

        parameters.setJpegThumbnailQuality(100);

        List<Camera.Size> thumbnailSizes = parameters.getSupportedJpegThumbnailSizes();
        size = getOptimalSize(thumbnailSizes, thumbnailWidth, thumbnailHeight);
        mCameraThumbnailWidth = size[0];
        mCameraThumbnailHeight = size[1];

        parameters.setJpegThumbnailSize(mCameraThumbnailWidth, mCameraThumbnailHeight);

        List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        size = getOptimalSize(pictureSizes, pictureWidth, pictureHeight);
        mCameraPictureWidth = size[0];
        mCameraPictureHeight = size[1];

        parameters.setPictureSize(mCameraPictureWidth, mCameraPictureHeight);

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraIndex, cameraInfo);

        int orientation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (orientation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int rotation = 0;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (cameraInfo.orientation + degrees) % 360;
            rotation = (360 - rotation) % 360;
        } else {
            rotation = (cameraInfo.orientation - degrees + 360) % 360;
        }

        parameters.set("orientation", "portrait");
        parameters.setRotation(rotation);
        mCamera.setParameters(parameters);
    }

    private int[] getOptimalSize(List<Camera.Size> sizes, int desiredWidth, int desiredHeight) {

        int[] result = new int[2];
        ArrayList<Camera.Size> goodSizes = new ArrayList<Camera.Size>();

        for (int i = 0; i < sizes.size(); i++) {
            if (sizes.get(i).width >= desiredWidth && sizes.get(i).height >= desiredHeight)
                goodSizes.add(sizes.get(i));
        }

        if (goodSizes.size() > 0) {
            result[0] = goodSizes.get(0).width;
            result[1] = goodSizes.get(0).height;

            for (int i = 1; i < goodSizes.size(); i++) {
                Camera.Size size = goodSizes.get(i);
                if (size.width < result[0] || size.height < result[1]) {
                    result[0] = size.width;
                    result[1] = size.height;
                }
            }
        } else {
            result[0] = sizes.get(0).width;
            result[1] = sizes.get(0).height;

            for (int i = 1; i < sizes.size(); i++) {
                Camera.Size size = sizes.get(i);
                if (size.width > result[0] || size.height > result[1]) {
                    result[0] = size.width;
                    result[1] = size.height;
                }
            }
        }

        return result;
    }

    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        if (mCamera == null) return;

        try {
            mCamera.setPreviewTexture(surfaceTexture);
        }
        catch (IOException ioe ) {
            Log.e(TAG, "Could not set preview texture " + ioe.getMessage());
            return;
        }

        mSurfaceTexture = surfaceTexture;
    }

    public void startPreview(int previewWidth, int previewHeight,
                             int thumbnailWidth, int thumbnailHeight,
                             int pictureWidth, int pictureHeight) { // TODO: return previw size to pass in shader or so

        if (mCamera == null) return;

        setPreferredParams(previewWidth, previewHeight,
                thumbnailWidth, thumbnailHeight,
                pictureWidth, pictureHeight); // TODO: it could change in surface update

        try {
            mCamera.startPreview();
        }
        catch (Exception e ) {
            Log.e(TAG, "Could not start preview " + e.getMessage());
        }
    }

    public void stopPreview() {

        if (mCamera == null) return;

        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore
        }

    }

    public void close() {

        if (mCamera == null) return;

        stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private void takePictureNow() {

        if (mCamera == null) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            MediaActionSound sound = new MediaActionSound();
            sound.play(MediaActionSound.SHUTTER_CLICK);
        }

        mCamera.takePicture(null, null, mPictureCallback);
    }

    public void takePicture() {
        if (mCamera == null) return;

        try {
            Camera.Parameters params = mCamera.getParameters();
            if (params.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mCamera.cancelAutoFocus();

                List<String> lModes = params.getSupportedFocusModes();
                if (lModes != null) {
                    if (lModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        mCamera.setParameters(params);
                    }
                }

                try {
                    takePictureNow();
                }
                catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

                return;
            } else if (params.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
                mCamera.autoFocus(mAutoFocusCallback);
            } else {
                takePictureNow();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        mCamera.autoFocus(mAutoFocusCallback);
    }

    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus (boolean success, Camera camera) {

            /*if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                MediaActionSound sound = new MediaActionSound();
                sound.play(MediaActionSound.FOCUS_COMPLETE);
            }*/

            // TODO: set settings

            if (success) {
                try {
                    camera.takePicture(null, null, mPictureCallback);
                }
                catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    };

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO: then call custom callback

            // TODO: check for files not to be overwritten

            File dir = new File(LenticaConfig.getDcimPath());
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory");
                    return;
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filePath = dir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg";
            File file = new File(filePath);
            if (file == null) {
                Log.e(TAG, "Failed to create file");
            } else {
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(data);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "Error " + e.getMessage());
                }

                if (mListener != null) mListener.onPictureTaken(filePath);
            }

            // TODO: make some toasts in case of fail

            startPreview(mCameraPreviewWidth, mCameraPreviewHeight,
                    mCameraThumbnailWidth, mCameraThumbnailHeight,
                    mCameraPictureWidth, mCameraPictureHeight);
        }
    };

    // TODO: save current camera somehow

    public void switchCameras() {
        if (mCamerasCount <= 1) return;

        close();

        mCurrentCameraIndex ++;
        if (mCurrentCameraIndex >= mCamerasCount)
            mCurrentCameraIndex = 0;

        open();

        setPreviewTexture(mSurfaceTexture);

        startPreview(mCameraPreviewWidth, mCameraPreviewHeight,
                mCameraThumbnailWidth, mCameraThumbnailHeight,
                mCameraPictureWidth, mCameraPictureHeight);
    }

    public boolean isExposureLockSupported() {

        if (mCamera == null) open();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) return false;

        Camera.Parameters parameters = mCamera.getParameters();

        if (!parameters.isAutoWhiteBalanceLockSupported()) return false;
        if (!parameters.isAutoExposureLockSupported()) return false;

        return true;
    }

    public void setExposureLock(boolean locked) {

        if (!isExposureLockSupported()) return;

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) return;

        Camera.Parameters parameters = mCamera.getParameters();

        parameters.setAutoWhiteBalanceLock(locked);
        parameters.setAutoExposureLock(locked);

        mCamera.setParameters(parameters);
    }

    public int getCamerasCount() {
        return mCamerasCount;
    }

    public boolean isFlashAvilable() {

        if (mCamera == null) open();

        Camera.Parameters parameters = mCamera.getParameters();

        if (parameters.getFlashMode() == null) return false;

        List<String> flashModes = parameters.getSupportedFlashModes();
        if (!flashModes.contains(Camera.Parameters.FLASH_MODE_ON)) return false;

        return true;
    }

    public void setFlashMode(int value) {

        if (!isFlashAvilable()) return;

        Camera.Parameters parameters = mCamera.getParameters();
        String flashMode = null;

        switch (value) {
            case FLASH_AUTO:
                flashMode = Camera.Parameters.FLASH_MODE_AUTO;
                break;

            case FLASH_OFF:
                flashMode = Camera.Parameters.FLASH_MODE_OFF;
                break;

            case FLASH_ON:
                flashMode = Camera.Parameters.FLASH_MODE_ON;
                break;
        }

        if (flashMode != null) {
            parameters.setFlashMode(flashMode);
            mCamera.setParameters(parameters);
        }
    }
}
