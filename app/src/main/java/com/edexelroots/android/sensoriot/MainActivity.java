package com.edexelroots.android.sensoriot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.ESSensorManager;
import com.ubhave.sensormanager.SensorDataListener;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.sensors.SensorUtils;

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


    public void checkSelected(View v) {
        // TODO - Add implementation for reading respective sensors data

        switch (v.getId()) {
            case R.id.check_accel:
                // read accelerometer data
                subscribeToAccelerometer(v.isSelected());
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

//    ArrayList<Integer> subIDs = new ArrayList<>();

    int accelerometerId = -1;
    protected void subscribeToAccelerometer(boolean start) {
        try {
            ESSensorManager sm = ESSensorManager.getSensorManager(this);
            if(start) {
                // start sensing
                accelerometerId = sm.subscribeToSensorData(SensorUtils.SENSOR_TYPE_ACCELEROMETER, new AccelerometerListener());
            } else if(accelerometerId != -1){
                // stop sensing
                sm.unsubscribeFromSensorData(accelerometerId);
            }
        } catch (ESException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            ESSensorManager sm = ESSensorManager.getSensorManager(this);
            Utils.logE(getClass().getName(), "Unsubing sensors");
            sm.unsubscribeFromSensorData(accelerometerId);
        } catch (ESException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private class AccelerometerListener implements SensorDataListener {

        @Override
        public void onDataSensed(SensorData sensorData) {
            String str = sensorData.getSensorType() + " " + sensorData.getTimestamp();
            Utils.logE(getClass().getName(), str);
        }

        @Override
        public void onCrossingLowBatteryThreshold(boolean b) {

        }
    }
}
