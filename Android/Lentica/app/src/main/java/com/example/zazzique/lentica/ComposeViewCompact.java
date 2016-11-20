package com.example.zazzique.lentica;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zazzique on 17.11.2015.
 */
public class ComposeViewCompact extends LinearLayout implements ComposeViewInterface {
    private static final String TAG = ComposeViewCompact.class.getName();

    private final int ROWS = LenticaConfig.MAX_ROWS;
    private final int COLUMNS = LenticaConfig.MAX_COLUMNS;

    private final int CENTRAL_ROW = (ROWS - 1) / 2;
    private final int CENTRAL_COLUMN = (COLUMNS - 1) / 2;

    private Bitmap mBitmapEmpty;
    private Bitmap mBitmapEmptyCenter; // TODO: replace with drawable

    private ComposeCellView[][] mCells;

    private LenticularImage mLenticularImage;

    private View mSelectedCell = null;

    public ComposeViewCompact(Context context, AttributeSet attrs) {

        super(context, attrs);

        mBitmapEmpty = BitmapFactory.decodeResource(getResources(), R.drawable.empty);
        mBitmapEmptyCenter = BitmapFactory.decodeResource(getResources(), R.drawable.empty_center);

        mLenticularImage = new LenticularImage();

        final int cellSize = context.getResources().getDisplayMetrics().widthPixels / 6;

        mCells = new ComposeCellView[ROWS][COLUMNS];

        TableLayout.LayoutParams tableLayoutParams = new TableLayout.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);

        LayoutParams layoutParams = new LayoutParams(cellSize, cellSize);
        layoutParams.setMargins(4, 4, 4, 4);

        for (int i = 0; i < ROWS; i++) {

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

                if (i == CENTRAL_ROW) {
                    addView(mCells[i][j], layoutParams);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = (int)(width / 6);
        setMeasuredDimension(width, height);
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
}
