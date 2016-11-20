package com.example.zazzique.lentica;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;


public class CameraActivity extends Activity {

    private static final String TAG = CameraActivity.class.getName();

    private CameraView mView;
    private PhotoStripView mPhotoStripView;
    private CameraWrapper mCameraWrapper;

    private ImageView mDeleteButton;
    private ImageView mFlashButton;
    private ImageView mExposureLockButton;
    private ImageView mSwitchCameraButton;

    private boolean mExposureLock = false;
    private int mFlashMode = CameraWrapper.FLASH_AUTO;

    private ImageView shutterFlashView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mView = (CameraView)findViewById(R.id.cameraView);
        mCameraWrapper = mView.getCameraWrapper();
        mCameraWrapper.setListener(mCameraWrapperListener);
        mPhotoStripView = (PhotoStripView)findViewById(R.id.photoStripView);
        mPhotoStripView.setListener(mPhotoStripListener);

        //mDeleteButton = (ImageView)findViewById(R.id.deleteButton);
        //mDeleteButton.setEnabled(false);

        mFlashButton = (ImageView)findViewById(R.id.flashButton);
        if (!mCameraWrapper.isFlashAvilable()) {
            mFlashButton.setVisibility(View.INVISIBLE);
            mFlashButton.setEnabled(false);
        }

        mExposureLockButton = (ImageView)findViewById(R.id.expLockButton);
        if (!mCameraWrapper.isExposureLockSupported()) {
            mExposureLockButton.setVisibility(View.INVISIBLE);
            mExposureLockButton.setEnabled(false);
        }

        mSwitchCameraButton = (ImageView)findViewById(R.id.switchCameraButton);
        if (mCameraWrapper.getCamerasCount() <= 1) {
            mSwitchCameraButton.setVisibility(View.INVISIBLE);
            mSwitchCameraButton.setEnabled(false);
        }

        shutterFlashView = (ImageView)findViewById(R.id.shutterFlash);
    }

    @Override
    protected void onPause() {
        mView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mView.onResume();
    }

    PhotoStripView.PhotoStripListener mPhotoStripListener = new PhotoStripView.PhotoStripListener() {

        @Override
        public void onItemSelected(String imagePath) {
            mView.setOverlayTexture(imagePath);
        }
    };

    public void onTakePhotoButton(View view) {
        mCameraWrapper.takePicture();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            shutterFlashView.animate().alpha(1.0f).setDuration(125).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        shutterFlashView.animate().alpha(0.0f).setDuration(1000).setStartDelay(300);
                    }
                }
            });;

        }
    }

    public void onComposeButton(View view) {
        Intent intent = new Intent(this, ComposeActivity.class);
        startActivity(intent);
        finish();
    }

    CameraWrapper.CameraWrapperListener mCameraWrapperListener = new CameraWrapper.CameraWrapperListener() {

        @Override
        public void onPictureTaken(String imagePath) {
            if (imagePath != null) mPhotoStripView.addPhoto(imagePath, 0);
        }
    };

    public void onDeleteButton(View view) {

       // TODO: dialog box
    }

    public void onFlashButton(View view) {

        switch (mFlashMode) {
            case CameraWrapper.FLASH_AUTO:
                mFlashMode = CameraWrapper.FLASH_OFF;
                mFlashButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_flash_off_white_24dp));
                break;
            case CameraWrapper.FLASH_OFF:
                mFlashMode = CameraWrapper.FLASH_ON;
                mFlashButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_flash_on_white_24dp));
                break;
            case CameraWrapper.FLASH_ON:
                mFlashMode = CameraWrapper.FLASH_AUTO;
                mFlashButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_flash_auto_white_24dp));
                break;
        }

        mCameraWrapper.setFlashMode(mFlashMode);
    }

    public void onExposureLockButton(View view) {
        if (mExposureLock == true) {
            mExposureLock = false;
            mExposureLockButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_lock_open_white_24dp));
        } else {
            mExposureLock = true;
            mExposureLockButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_lock_white_24dp));
        }

        mCameraWrapper.setExposureLock(mExposureLock);
    }

    public void onSwitchCameraButton(View view) {
        mCameraWrapper.switchCameras();
    }

    public void onGalleryButton(View view) {

        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
        finish();
    }
}
