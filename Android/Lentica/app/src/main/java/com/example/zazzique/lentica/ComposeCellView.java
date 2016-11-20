package com.example.zazzique.lentica;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by zazzique on 17.11.2015.
 */
public class ComposeCellView extends ImageView {

    private static final float TRANSPARENT = 0.5f;
    private static final float OPAQUE = 1.0f;

    private String mImagePath = null;
    private boolean mEmpty = true;
    private ComposeViewInterface mParent = null;

    private ComposeCellDragEventListener mDragListener = null;

    public ComposeCellView(Context context, ComposeViewInterface parent) {
        super(context);

        mParent = parent;

        setScaleType(ImageView.ScaleType.CENTER_CROP);
        setAlpha(TRANSPARENT);

        mDragListener = new ComposeCellDragEventListener();
        setOnDragListener(mDragListener);
        setOnClickListener(onSelectListener);
    }

    public boolean isEmpty() {
        return mEmpty;
    }

    public void setEmpty(boolean empty) {
        mEmpty = empty;
    }

    public void setSelected(boolean selected) {
        if (selected) {
            setAlpha(OPAQUE);
        } else {
            setAlpha(TRANSPARENT);
        }
    }

    public String getImagePath() {
        return mImagePath;
    }

    View.OnClickListener onSelectListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            if (mParent == null) return;

            View selectedCell = mParent.getSelectedCell();
            ComposeCellView cellView = (ComposeCellView)v;

            if (cellView.isEmpty()) return;

            if (selectedCell == v) {
                mParent.setSelectedCell(null);
            } else {
                mParent.setSelectedCell(v);
            }
        }
    };

    protected class ComposeCellDragEventListener implements View.OnDragListener {

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
                        cellView.setAlpha(OPAQUE);
                        cellView.invalidate();
                        return true;
                    }

                    return false;

                case DragEvent.ACTION_DRAG_ENTERED:

                    cellView.setAlpha(OPAQUE);
                    cellView.setColorFilter(Color.YELLOW);
                    cellView.invalidate();

                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:

                    return true;

                case DragEvent.ACTION_DRAG_EXITED:

                    cellView.setAlpha(OPAQUE);
                    cellView.clearColorFilter();
                    cellView.invalidate();

                    return true;

                case DragEvent.ACTION_DROP:

                    ClipData.Item item = event.getClipData().getItemAt(0);

                    mImagePath = item.getText().toString();

                    // TODO: set new image and delete old one
                    // TODO: in separate procedure

                    Bitmap thumbnailBitmap = Image.loadThumbnail(mImagePath, cellView.getWidth(), cellView.getHeight());
                    cellView.setImageBitmap(thumbnailBitmap);
                    cellView.setEmpty(false);

                    mParent.updateCells(cellView);

                    cellView.setAlpha(TRANSPARENT);
                    cellView.clearColorFilter();
                    cellView.invalidate();

                    return true;

                case DragEvent.ACTION_DRAG_ENDED:

                    cellView.setAlpha(TRANSPARENT);
                    cellView.clearColorFilter();
                    cellView.invalidate();

                    return true;
            }

            return false;
        }
    };
}