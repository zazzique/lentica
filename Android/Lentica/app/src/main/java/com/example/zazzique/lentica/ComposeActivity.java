package com.example.zazzique.lentica;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TabHost;

public class ComposeActivity extends Activity implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = ComposeActivity.class.getName();

    private ComposeViewInterface mComposeView = null;
    private LenticularView mLenticularView = null;
    private LenticularImageTransformView mLenticularImageTransformView = null;

    private ImageView mPositionLockButton = null;

    private static final String TAB_TAG_COMPOSE = "tagCompose";
    private static final String TAB_TAG_COLOURS = "tagColours";
    private static final String TAB_TAG_FILTERS = "tagFilters";
    private static final String TAB_TAG_EFFECTS = "tagEffects";

    private TabHost mToolsTabHost = null;

    private SeekBar mBrightnessSeekBar = null;
    private SeekBar mContrastSeekBar = null;
    private SeekBar mSaturationSeekBar = null;

    private LookSelectionAdapter mLookAdapter = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        mToolsTabHost = (TabHost)findViewById(R.id.toolsTabHost);
        mToolsTabHost.setup();

        TabHost.TabSpec tabSpec;
        tabSpec = mToolsTabHost.newTabSpec(TAB_TAG_COMPOSE);
        tabSpec.setIndicator("Compose"); // TODO: to strings file
        tabSpec.setContent(R.id.composeTabLayout);
        mToolsTabHost.addTab(tabSpec);

        tabSpec = mToolsTabHost.newTabSpec(TAB_TAG_COLOURS);
        tabSpec.setIndicator("Colours"); // TODO: to strings file
        tabSpec.setContent(R.id.coloursTabLayout);
        mToolsTabHost.addTab(tabSpec);

        tabSpec = mToolsTabHost.newTabSpec(TAB_TAG_FILTERS);
        tabSpec.setIndicator("Filters"); // TODO: to strings file
        tabSpec.setContent(R.id.filtersTabLayout);
        mToolsTabHost.addTab(tabSpec);

        tabSpec = mToolsTabHost.newTabSpec(TAB_TAG_EFFECTS);
        tabSpec.setIndicator("Effects"); // TODO: to strings file
        tabSpec.setContent(R.id.effectsTabLayout);
        mToolsTabHost.addTab(tabSpec);

        mToolsTabHost.setCurrentTabByTag(TAB_TAG_COMPOSE);

        mComposeView = (ComposeViewInterface)findViewById(R.id.composeViewCompact);

        mLenticularView = (LenticularView)findViewById(R.id.lenticularView);
        mLenticularView.setLenticularImage(mComposeView.getLenticularImage());

        mLenticularImageTransformView = (LenticularImageTransformView)findViewById(R.id.lenticularImageTransform);
        mLenticularImageTransformView.setComposeView(mComposeView);

        //mToggleComposeButton = (ImageView)findViewById(R.id.toggleComposeButton);
        mPositionLockButton = (ImageView)findViewById(R.id.posLockButton);

        mBrightnessSeekBar = (SeekBar)findViewById(R.id.brightnessSeekBar);
        mBrightnessSeekBar.setOnSeekBarChangeListener(this);

        mContrastSeekBar = (SeekBar)findViewById(R.id.contrastSeekBar);
        mContrastSeekBar.setOnSeekBarChangeListener(this);

        mSaturationSeekBar = (SeekBar)findViewById(R.id.saturationSeekBar);
        mSaturationSeekBar.setOnSeekBarChangeListener(this);


        mLookAdapter = new LookSelectionAdapter(this);

        GridView lookSelectionView = (GridView)findViewById(R.id.lookSelectionView);
        lookSelectionView.setAdapter(mLookAdapter);

        /*lookSelectionView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                mLenticularView.getLenticularImage().setLook(mLookAdapter.getLook(position));
            }
        });*/

        mLookAdapter.setLenticularImage(mLenticularView.getLenticularImage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLenticularView.startSensors();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLenticularView.stopSensors();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //
    }

    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
        switch (bar.getId()) {

            case R.id.brightnessSeekBar:
                mLenticularView.getLenticularImage().setBrightness(((float)(progress - 100)) / 100.0f);
                break;

            case R.id.contrastSeekBar:
                mLenticularView.getLenticularImage().setContrast(((float)progress) / 100.0f);
                break;

            case R.id.saturationSeekBar:
                mLenticularView.getLenticularImage().setSaturation(((float)progress) / 100.0f);
                break;
        }
    }

    public void onCameraButton(View view) {

        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        finish();
    }

    public void onSaveButton(View view) {

        mComposeView.saveLenticularImage();

        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
        finish();
    }

    public void onGalleryButton(View view) {

        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
        finish();
    }

    /*public void onToggleComposeButton(View view) {

        if (mComposeActive == true) {
            mComposeActive = false;
            mToggleComposeButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_visibility_off_white_24dp));
        } else {
            mComposeActive = true;
            mToggleComposeButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_visibility_white_24dp));
        }

        if (mComposeActive) {
            mComposeView.setVisibility(View.VISIBLE);
            mComposeView.setEnabled(true);
        } else {
            mComposeView.setVisibility(View.INVISIBLE);
            mComposeView.setEnabled(false);
        }
    }*/

    public void onPositionLockButton(View view) {
        if (mLenticularView.getLock() == true) {
            mLenticularView.setLock(false);
            mPositionLockButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_lock_open_white_24dp));
        } else {
            mLenticularView.setLock(true);
            mPositionLockButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_lock_white_24dp));
        }
    }
}
