package com.edexelroots.android.sensoriot.kinesis;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Size;
import android.widget.Toast;

import com.edexelroots.android.sensoriot.R;
import com.edexelroots.android.sensoriot.Utils;
import com.edexelroots.android.sensoriot.kinesis.fragments.Camera2BasicFragment;
import com.edexelroots.android.sensoriot.kinesis.fragments.StreamConfigurationFragment;
import com.edexelroots.android.sensoriot.kinesis.fragments.StreamingFragment;

public class KinesisActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kinesis);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(null == savedInstanceState) {
            startConfigFragment();
        }
    }

    public void startFragment(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if(null == tag) {
            fragmentManager.beginTransaction().replace(R.id.content_simple, fragment).commit();
        } else {
            fragmentManager.beginTransaction().replace(R.id.content_simple, fragment, tag).commit();
        }
    }

    private final String STREAMING_FRAGMENT_TAG = "preview_fragment";

    public void indicateFace(String label, float rTop, float rLeft, float rHeight, float rWidth) {
        Fragment f = getSupportFragmentManager().findFragmentByTag(STREAMING_FRAGMENT_TAG);
//        Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
        // Todo
        assert f != null && f instanceof Camera2BasicFragment;
        Size previewSize = ((Camera2BasicFragment) f).getPreviewSize();
        Utils.logE(getClass().getName(), "Preview: " + previewSize.getWidth() + ", " + previewSize.getHeight());
        int w = previewSize.getHeight();
        int h = previewSize.getWidth();
        float top = h * rTop;
        float height = h * rHeight;
        float left = w * rLeft;
        float width = w * rWidth;

        ((Camera2BasicFragment) f).drawRectangle(left, top, width, height);
    }

    public void startCamera2Fragment(Bundle extras) {
        try {
            Camera2BasicFragment f = Camera2BasicFragment.newInstance(this);
            f.setArguments(extras);
            startFragment(f, STREAMING_FRAGMENT_TAG);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not start streaming", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public  void startStreamingFragment(Bundle extras) {
        try {
            StreamingFragment f = StreamingFragment.newInstance(this);
            f.setArguments(extras);
            startFragment(f, null);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not start streaming", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void startConfigFragment() {
        Utils.logE(getClass().getName(), "Start Config Fragment");
        try {
            StreamConfigurationFragment f = StreamConfigurationFragment.newInstance(this);
            startFragment(f, null);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not config", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
