package com.example.zazzique.lentica;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by zzq on 14.10.2015.
 */


class PhotoStripItemView extends LinearLayout {
    private static final String TAG = PhotoStripItemView.class.getName();

    private PhotoStripView mPhotoStripView = null;
    private String mFilePath = null;

    private class LongPressRunnable implements Runnable {
        private View mView;

        public LongPressRunnable(View v) {
            mView = v;
        }

        public void run() {
            ClipData clipData = ClipData.newPlainText(LenticaConfig.CLIP_LABEL, mFilePath);
            StripItemShadowBuilder dragShadow = new StripItemShadowBuilder(mView);
            mView.startDrag(clipData, dragShadow, null, 0);
        }
    }

    LongPressRunnable mLongPressed = new LongPressRunnable(this);

    final Handler mHandler = new Handler();

    public PhotoStripItemView(Context context, PhotoStripView photoStripView, String path, int size, int frameSize) {
        super(context);

        mPhotoStripView = photoStripView;

        mFilePath = path;

        Bitmap thumbnailBitmap = Image.loadThumbnail(path, size, size);

        setLayoutParams(new ViewGroup.LayoutParams(frameSize, frameSize));
        setGravity(Gravity.CENTER);
        setBackgroundColor(Color.BLACK);
        setOnClickListener(onStripItemSelect);
        setOnTouchListener(onStripItemDrag);

        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(thumbnailBitmap); // TODO: load it by streaming

        addView(imageView);
    }

    public String getFilePath() {
        return mFilePath;
    }

    View.OnClickListener onStripItemSelect = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            ViewGroup parent = (ViewGroup)v.getParent();

            if (parent == null) return;

            View selectedItem = mPhotoStripView.getSelectedItem();

            for (int i = 0; i < parent.getChildCount(); i++) {
                final View child = parent.getChildAt(i);

                if (child == v) {
                   if (selectedItem == v) {
                        child.setBackgroundColor(Color.BLACK);
                        mPhotoStripView.setSelectedItem(null);
                   } else {
                        child.setBackgroundColor(Color.WHITE);
                        mPhotoStripView.setSelectedItem(v);
                    }
                } else if (child != null) {
                    child.setBackgroundColor(Color.BLACK);
                }
            }
        }
    };

    View.OnTouchListener onStripItemDrag = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            //if (mPhotoStripView.isListenerAvilable()) return false; // TODO: temporary

            if (event.getAction() == MotionEvent.ACTION_DOWN)
                mHandler.postDelayed(mLongPressed, 200);

            if ((event.getAction() == MotionEvent.ACTION_UP))
                mHandler.removeCallbacks(mLongPressed);

            return false;
        }
    };

    private static class StripItemShadowBuilder extends View.DragShadowBuilder {
        private Drawable shadow = null;

        public StripItemShadowBuilder(View v) {
            super(v);

            ViewGroup viewGroup = (ViewGroup)v;
            if (viewGroup == null) return;

            ImageView child = (ImageView)viewGroup.getChildAt(0);
            if (child == null) return;

            shadow = child.getDrawable();
        }

        @Override
        public void onProvideShadowMetrics(Point size, Point touch) {
            int width, height;

            width = getView().getWidth();
            height = getView().getHeight();

            if (shadow != null) {
                shadow.setBounds(0, 0, width, height);
            }

            size.set(width, height);
            touch.set(width / 2, height / 2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            if (shadow != null) {
                shadow.draw(canvas);
            }
        }
    }
}

public class PhotoStripView extends HorizontalScrollView {
    private static final String TAG = PhotoStripView.class.getName();

    private Context mContext;
    private LinearLayout mLayout;

    private View mSelectedItem = null;

    private int mCellSize;
    private int mCellFrameSize;

    private PhotoStripListener mListener = null;

    public interface PhotoStripListener {
        void onItemSelected(String imagePath);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = (int)(width / 6);
        setMeasuredDimension(width, height);
    }

    public PhotoStripView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        mCellFrameSize = getResources().getDisplayMetrics().widthPixels / 6;
        mCellSize = mCellFrameSize - (int)(4.0f * getResources().getDisplayMetrics().density);

        mLayout = new LinearLayout(context, attrs);
        mLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mLayout.setOrientation(LinearLayout.HORIZONTAL);
        mLayout.setBackgroundColor(Color.BLACK);
        mLayout.setMinimumHeight(mCellFrameSize);

        addView(mLayout);

        loadPhotos();
    }

    public void setListener(PhotoStripListener listener) {
        mListener = listener;
    }

    public void loadPhotos() {
        File dir = new File(LenticaConfig.getDcimPath());
        if (dir.exists()) {
            File[] files = dir.listFiles();

            if (files != null && files.length > 1) {
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {

                        if (o1.lastModified() > o2.lastModified()) {
                            return -1;
                        } else if (o1.lastModified() < o2.lastModified()) {
                            return +1;
                        } else {
                            return 0;
                        }
                    }
                });
            }

            for (int i = 0; i < files.length; i++) {
                addPhoto(files[i].getAbsolutePath());
            }
        }
    }

    public void addPhoto(String path) {
        PhotoStripItemView item = new PhotoStripItemView(mContext, this, path, mCellSize, mCellFrameSize);
        mLayout.addView(item);
    }

    public void addPhoto(String path, int position) {
        PhotoStripItemView item = new PhotoStripItemView(mContext, this, path, mCellSize, mCellFrameSize);
        mLayout.addView(item, position);
    }

    public void deleteSelectedPhoto() {
        if (mSelectedItem == null) return;

        if (mSelectedItem instanceof PhotoStripItemView) {
            File file = new File(((PhotoStripItemView)mSelectedItem).getFilePath());
            boolean deleted = file.delete();

            if (deleted) {
                mLayout.removeView(mSelectedItem);
                mSelectedItem = null;
                mListener.onItemSelected(null);
            }
        }
    }

    public View getSelectedItem() {
        return mSelectedItem;
    }

    public void setSelectedItem(View item) {
        mSelectedItem = item;

        if (isListenerAvilable()) {

            PhotoStripItemView stripItem = (PhotoStripItemView)item;
            if (stripItem == null) {
                mListener.onItemSelected(null);
            } else {
                mListener.onItemSelected(stripItem.getFilePath());
            }
        }
    }

    public boolean isListenerAvilable() {

        if (mListener != null)
            return true;

        return false;

    }
}
