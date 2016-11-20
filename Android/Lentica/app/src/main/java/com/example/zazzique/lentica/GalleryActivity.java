package com.example.zazzique.lentica;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

public class GalleryActivity extends Activity {

    private LenticularView mLenticularView;
    private LenticularImageAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        mLenticularView = (LenticularView)findViewById(R.id.lenticularView);

        mAdapter = new LenticularImageAdapter(this);

        // TODO: separate class
        GridView lenticularGalleryView = (GridView)findViewById(R.id.lenticularGalleryView);
        lenticularGalleryView.setAdapter(mAdapter);

        lenticularGalleryView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                mLenticularView.setLenticularImage(mAdapter.getLenticularImage(position));
            }
        });
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

    public void onCameraButton(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        finish();
    }

    public void onComposeButton(View view) {
        Intent intent = new Intent(this, ComposeActivity.class);
        startActivity(intent);
        finish();
    }
}
