package com.example.zazzique.lentica;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by zazzique on 28.10.2015.
 */
public class LenticularImageAdapter extends BaseAdapter {
    private Context mContext;

    ArrayList<LenticularImage> mLenticularImages = new ArrayList<LenticularImage>();

    private final int ROWS = LenticaConfig.MAX_ROWS;
    private final int COLUMNS = LenticaConfig.MAX_COLUMNS;

    private final int CENTRAL_ROW = (ROWS - 1) / 2;
    private final int CENTRAL_COLUMN = (COLUMNS - 1) / 2;

    private int mCellSize;
    private int mCellFrameSize;

    public LenticularImageAdapter(Context c) {
        mContext = c;
        mCellFrameSize = c.getResources().getDisplayMetrics().widthPixels / 4;
        mCellSize = mCellFrameSize - (int)(4.0f * c.getResources().getDisplayMetrics().density);
        loadLenticularImages();
    }

    @Override
    public int getCount() {
        return mLenticularImages.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(mCellSize, mCellSize));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView)convertView;
        }

        LenticularImage lenticularImage = mLenticularImages.get(position);

        if (lenticularImage == null) return null;

        String path = lenticularImage.getTexturePath(CENTRAL_ROW, CENTRAL_COLUMN);

        if (path == null) return null;

        Bitmap thumbnailBitmap = Image.loadThumbnail(path, mCellSize, mCellSize);

        if (thumbnailBitmap != null) {
            imageView.setImageBitmap(thumbnailBitmap);
        }

        return imageView;
    }

    public LenticularImage getLenticularImage(int position) {
        return mLenticularImages.get(position);
    }

    private void loadLenticularImages() {

        mLenticularImages.clear();

        File dir = new File(LenticaConfig.getLenticularImagesPath());
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

            for (int i = 0; i < files.length - 1; i++) {

                LenticularImage lenticularImage = new LenticularImage();
                lenticularImage.loadFromFile(files[i].getAbsolutePath());
                mLenticularImages.add(lenticularImage);
            }
        }
    }
}
