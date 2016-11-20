package com.example.zazzique.lentica;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Created by zazzique on 17.11.2015.
 */
public class LenticularImageTransformView extends View implements RotationGestureDetector.OnRotationGestureListener {

    private static final int INVALID_POINTER_ID = -1;

    private ScaleGestureDetector mScaleDetector;
    private RotationGestureDetector mRotationDetector;
    private float mLastTouchX = 0.0f;
    private float mLastTouchY = 0.0f;
    private int mActivePointerId = INVALID_POINTER_ID;

    ComposeViewInterface mComposeView = null;

    public LenticularImageTransformView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mRotationDetector = new RotationGestureDetector(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        setMeasuredDimension(width, width);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        mScaleDetector.onTouchEvent(event);
        mRotationDetector.onTouchEvent(event);

        final int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {

                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                mLastTouchX = MotionEventCompat.getX(event, pointerIndex);
                mLastTouchY = MotionEventCompat.getY(event, pointerIndex);
                mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);

                final float x = MotionEventCompat.getX(event, pointerIndex);
                final float y = MotionEventCompat.getY(event, pointerIndex);

                final float dx = x - mLastTouchX;
                final float dy = y - mLastTouchY;

                if (mComposeView != null) {
                    int pos[] = mComposeView.getSelectedCellPos();

                    if (pos != null) {
                        mComposeView.getLenticularImage().mOffsetX[pos[0]][pos[1]] -= dx / 2600.0f;
                        mComposeView.getLenticularImage().mOffsetY[pos[0]][pos[1]] -= dy / 2600.0f;
                    }
                }

                invalidate();

                mLastTouchX = x;
                mLastTouchY = y;

                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {

                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);

                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = MotionEventCompat.getX(event, newPointerIndex);
                    mLastTouchY = MotionEventCompat.getY(event, newPointerIndex);
                    mActivePointerId = MotionEventCompat.getPointerId(event, newPointerIndex);
                }
                break;
            }
        }

        return true;
    }

    @Override
    public void OnRotation(RotationGestureDetector rotationDetector) {

        if (mComposeView != null) {
            int pos[] = mComposeView.getSelectedCellPos();

            if (pos == null) return;

            mComposeView.getLenticularImage().mRotation[pos[0]][pos[1]] += (float) Math.toRadians(-rotationDetector.getAngle() * 0.5f);
            //mLenticularImage.mRotation[pos[0]][pos[1]] = Math.max(0.1f, Math.min(mLenticularImage.mRotation[pos[0]][pos[1]], 1.5f));
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            if (mComposeView != null) {
                int pos[] = mComposeView.getSelectedCellPos();

                if (pos == null) return false;

                if (detector.getScaleFactor() != 0.0f) {
                    mComposeView.getLenticularImage().mScale[pos[0]][pos[1]] /= detector.getScaleFactor();
                    mComposeView.getLenticularImage().mScale[pos[0]][pos[1]] = Math.max(0.1f, Math.min(mComposeView.getLenticularImage().mScale[pos[0]][pos[1]], 1.5f));
                }
            }
            invalidate();

            return true;
        }
    }

    public void setComposeView(ComposeViewInterface composeView) {
        mComposeView = composeView;
    }
}
