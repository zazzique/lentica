package com.example.zazzique.lentica;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.SurfaceHolder;
import android.view.View;

import java.io.File;

/**
 * Created by zazzique on 06.10.2015.
 */
public class CameraView extends GLSurfaceView
{
    private static final String TAG = CameraView.class.getName();

    private CameraViewDragEventListener mDragListener = null;

    private CameraRenderer mRenderer;

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRenderer = new CameraRenderer((Activity)context, this);
        setEGLContextClientVersion(2);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mDragListener = new CameraViewDragEventListener();
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

    public void setOverlayTexture(String path) {
        mRenderer.setOverlayTexture(path);
    }

    public CameraWrapper getCameraWrapper() {
        return mRenderer.getCameraWrapper();
    }

    // TODO: remove later
    protected class CameraViewDragEventListener implements View.OnDragListener {

        @Override
        public boolean onDrag(View v, DragEvent event) {

            ComposeCellView cellView = (ComposeCellView)v;

            final int action = event.getAction();
            switch(action) {

                case DragEvent.ACTION_DRAG_STARTED:

                    if (!cellView.isEnabled()) {
                        return false;
                    }

                    if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        return true;
                    }

                    return false;

                case DragEvent.ACTION_DRAG_ENTERED:

                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:

                    return true;

                case DragEvent.ACTION_DRAG_EXITED:

                    return true;

                case DragEvent.ACTION_DROP:

                    // TODO: remove image
                    ClipData.Item item = event.getClipData().getItemAt(0);

                    String path = item.getText().toString();

                    File file = new File(path);
                    boolean deleted = file.delete();

                    return true;

                case DragEvent.ACTION_DRAG_ENDED:


                    return true;
            }

            return false;
        }
    };
}