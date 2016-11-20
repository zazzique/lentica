package com.example.zazzique.lentica;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by zazzique on 07.12.2015.
 */
public class LookSelectionAdapter extends BaseAdapter {

    private Context mContext;

    private int mCellSize;
    private int mCellFrameSize;

    private LenticularImage mLenticularImage = null;

    public LookSelectionAdapter(Context c) {
        mContext = c;

        mCellFrameSize = c.getResources().getDisplayMetrics().widthPixels / 4;
        mCellSize = mCellFrameSize - (int)(4.0f * c.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getCount() {
        return LenticularImage.LOOKS_ARRAY.length;
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

        Button button;

        if (convertView == null) {
            button = new Button(mContext);
            button.setLayoutParams(new GridView.LayoutParams(mCellSize, mCellSize));
            button.setText(getLook(position));
            button.setTag(position);
            button.setOnClickListener(lookButtonOnClickListener);

        } else {
            button = (Button)convertView;
        }

        return button;
    }

    public String getLook(int position) { // TODO: meybe don't need
        return LenticularImage.LOOKS_ARRAY[position];
    }

    private View.OnClickListener lookButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            GridView gridView = (GridView)v.getParent();
            final int position = gridView.getPositionForView(v);

            if (mLenticularImage != null) {
                mLenticularImage.setLook(getLook(position));
            }
        }
    };

    public void setLenticularImage(LenticularImage lenticularImage) {
        mLenticularImage = lenticularImage;
    }
}
