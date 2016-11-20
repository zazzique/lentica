package com.example.zazzique.lentica;

import android.view.View;

/**
 * Created by zazzique on 17.11.2015.
 */
public interface ComposeViewInterface {
    View getSelectedCell();
    void setSelectedCell(View v);
    void updateCells(ComposeCellView cell);
    LenticularImage getLenticularImage();
    void saveLenticularImage();
    int[] getSelectedCellPos();
}
