package com.example.zazzique.lentica;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zazzique on 20.10.2015.
 */
public class ComposeView extends TableLayout implements RotationGestureDetector.OnRotationGestureListener, ComposeViewInterface {
    private static final String TAG = ComposeView.class.getName();

    private final int ROWS = LenticaConfig.MAX_ROWS;
    private final int COLUMNS = LenticaConfig.MAX_COLUMNS;

    private final int CENTRAL_ROW = (ROWS - 1) / 2;
    private final int CENTRAL_COLUMN = (COLUMNS - 1) / 2;

    private Bitmap mBitmapEmpty;
    private Bitmap mBitmapEmptyCenter;

    private Bitmap mBitmapCursor; // TODO: remove

    private TableRow[] mTableRows;
    private ComposeCellView[][] mCells;

    private LenticularImage mLenticularImage;

    private View mSelectedCell = null;

    private static final int INVALID_POINTER_ID = -1;

    private ScaleGestureDetector mScaleDetector;
    private RotationGestureDetector mRotationDetector;
    private float mLastTouchX = 0.0f;
    private float mLastTouchY = 0.0f;
    private int mActivePointerId = INVALID_POINTER_ID;

    public ComposeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mRotationDetector = new RotationGestureDetector(this);

        mBitmapEmpty = BitmapFactory.decodeResource(getResources(), R.drawable.empty);
        mBitmapEmptyCenter = BitmapFactory.decodeResource(getResources(), R.drawable.empty_center);

        mBitmapCursor = BitmapFactory.decodeResource(getResources(), R.drawable.empty_center);

        mLenticularImage = new LenticularImage();

        final int cellSize = context.getResources().getDisplayMetrics().widthPixels / 6;

        mCells = new ComposeCellView[ROWS][COLUMNS];
        mTableRows = new TableRow[ROWS];

        TableLayout.LayoutParams tableLayoutParams = new TableLayout.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);

        TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams(cellSize, cellSize);
        tableRowParams.setMargins(4, 4, 4, 4);
        tableRowParams.weight = 1;
        tableRowParams.gravity = Gravity.CENTER;

        for (int i = 0; i < ROWS; i++) {
            mTableRows[i] = new TableRow(context);

            for (int j = 0; j < COLUMNS; j++) {
                mCells[i][j] = new ComposeCellView(context, this);
                if (i == CENTRAL_ROW && j == CENTRAL_COLUMN) {
                    mCells[i][j].setImageBitmap(mBitmapEmptyCenter);
                } else {
                    mCells[i][j].setImageBitmap(mBitmapEmpty);
                }
                mCells[i][j].setEnabled(false);
                mCells[i][j].setVisibility(View.INVISIBLE);

                if (i == CENTRAL_ROW && j == CENTRAL_COLUMN) {
                    mCells[i][j].setEnabled(true);
                    mCells[i][j].setVisibility(View.VISIBLE);
                }

                mTableRows[i].addView(mCells[i][j], tableRowParams);
            }

            addView(mTableRows[i], tableLayoutParams);
        }
    }

    public View getSelectedCell() {
        return mSelectedCell;
    }

    public void setSelectedCell(View v) {
        mSelectedCell = v;

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                if (mCells[i][j] == mSelectedCell) {
                    mCells[i][j].setSelected(true);
                } else {
                    mCells[i][j].setSelected(false);
                }
            }
        }
    }

    public int[] getSelectedCellPos() {

        if (mSelectedCell == null) return null;

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                if (mCells[i][j] == mSelectedCell) {
                    int result[] = new int[]{i, j};
                    return result;
                }
            }
        }

        return null;
    }

    public void updateCells(ComposeCellView cell) {

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {

                boolean enable = false;

                if (!mCells[i][j].isEmpty()) enable = true;

                if (i > 0) {
                    if (!mCells[i - 1][j].isEmpty()) enable = true;
                }

                if (i < ROWS - 1) {
                    if (!mCells[i + 1][j].isEmpty()) enable = true;
                }

                if (j > 0) {
                    if (!mCells[i][j - 1].isEmpty()) enable = true;
                }

                if (j < COLUMNS - 1) {
                    if (!mCells[i][j + 1].isEmpty()) enable = true;
                }

                if (i != CENTRAL_ROW) enable = false;

                mCells[i][j].setEnabled(enable);

                if (enable) {
                    mCells[i][j].setVisibility(View.VISIBLE);
                } else {
                    mCells[i][j].setVisibility(View.INVISIBLE);
                }
            }
        }

        out:
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                if (cell == mCells[i][j]) {

                    mLenticularImage.setImage(cell.getImagePath(), i, j, 0.0f, 0.0f, 0.0f, 1.0f);
                    break out;
                }
            }
        }
    }

    public void saveLenticularImage() {

        File dir = new File(LenticaConfig.getLenticularImagesPath());
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "Failed to create directory");
                return;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filePath = dir.getPath() + File.separator + "LI_" + timeStamp + ".txt";

        mLenticularImage.saveToFile(filePath);
    }

    public LenticularImage getLenticularImage() {
        return mLenticularImage;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        mScaleDetector.onTouchEvent(ev);
        mRotationDetector.onTouchEvent(ev);

        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {

                final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                mLastTouchX = MotionEventCompat.getX(ev, pointerIndex);
                mLastTouchY = MotionEventCompat.getY(ev, pointerIndex);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);

                final float x = MotionEventCompat.getX(ev, pointerIndex);
                final float y = MotionEventCompat.getY(ev, pointerIndex);

                final float dx = x - mLastTouchX;
                final float dy = y - mLastTouchY;

                int pos[] = getSelectedCellPos();

                if (pos != null) {
                    mLenticularImage.mOffsetX[pos[0]][pos[1]] -= dx / 2600.0f;
                    mLenticularImage.mOffsetY[pos[0]][pos[1]] -= dy / 2600.0f;
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

                final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = MotionEventCompat.getX(ev, newPointerIndex);
                    mLastTouchY = MotionEventCompat.getY(ev, newPointerIndex);
                    mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
                }
                break;
            }
        }

        return true;
    }

    @Override
    public void OnRotation(RotationGestureDetector rotationDetector) {

        int pos[] = getSelectedCellPos();

        if (pos == null) return;

        mLenticularImage.mRotation[pos[0]][pos[1]] += (float)Math.toRadians(-rotationDetector.getAngle() * 0.5f);
        //mLenticularImage.mRotation[pos[0]][pos[1]] = Math.max(0.1f, Math.min(mLenticularImage.mRotation[pos[0]][pos[1]], 1.5f));
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            int pos[] = getSelectedCellPos();

            if (pos == null) return false;

            if (detector.getScaleFactor() != 0.0f) {
                mLenticularImage.mScale[pos[0]][pos[1]] /= detector.getScaleFactor();
                mLenticularImage.mScale[pos[0]][pos[1]] = Math.max(0.1f, Math.min(mLenticularImage.mScale[pos[0]][pos[1]], 1.5f));
            }
            invalidate();

            return true;
        }
    }
}
