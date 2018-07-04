package com.edexelroots.android.sensoriot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        CheckBox checkAccelero = findViewById(R.id.check_accel);
        CheckBox checkGyro = findViewById(R.id.check_gyro);
        CheckBox checkGPS = findViewById(R.id.check_gps);
        CheckBox checkRotation = findViewById(R.id.check_rotation);
        CheckBox checkOrientation = findViewById(R.id.check_orientation);
    }


    protected void checkSelected(View v) {
        // TODO - Add implementation for reading respective sensors data

        switch (v.getId()) {
            case R.id.check_accel:
                // read accelerometer data
                break;
            case R.id.check_gyro:
                // read gyro data
                break;
            case R.id.check_gps:
                // read gps data
                break;
            case R.id.check_orientation:
                break;
            case R.id.check_rotation:
                // get rotation vector
                break;
            default:
                Utils.logE(getClass().getName(), "switch default shouldn't have occurred!!");
                break;
        }
    }
}
