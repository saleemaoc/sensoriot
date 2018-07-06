package com.edexelroots.android.sensoriot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

/*
import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.ESSensorManager;
import com.ubhave.sensormanager.SensorDataListener;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.sensors.SensorInterface;
import com.ubhave.sensormanager.sensors.SensorUtils;
*/

import org.sensingkit.sensingkitlib.SKException;
import org.sensingkit.sensingkitlib.SKSensorDataListener;
import org.sensingkit.sensingkitlib.SKSensorModuleType;
import org.sensingkit.sensingkitlib.SensingKitLib;
import org.sensingkit.sensingkitlib.SensingKitLibInterface;
import org.sensingkit.sensingkitlib.data.SKAccelerometerData;
import org.sensingkit.sensingkitlib.data.SKGyroscopeData;
import org.sensingkit.sensingkitlib.data.SKLocationData;
import org.sensingkit.sensingkitlib.data.SKMagnetometerData;
import org.sensingkit.sensingkitlib.data.SKRotationData;
import org.sensingkit.sensingkitlib.data.SKSensorData;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements
        CompoundButton.OnCheckedChangeListener,
        SKSensorDataListener {

    SKSensorModuleType[] sensors = {
            SKSensorModuleType.ACCELEROMETER,
            SKSensorModuleType.GYROSCOPE,
            SKSensorModuleType.LOCATION,
            SKSensorModuleType.MAGNETOMETER,
            SKSensorModuleType.ROTATION
    };

    HashMap<SKSensorModuleType, String> sensorNames = new HashMap<>(5);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorNames.put(SKSensorModuleType.ACCELEROMETER, "ACCELEROMETER");
        sensorNames.put(SKSensorModuleType.GYROSCOPE, "GYROSCOPE");
        sensorNames.put(SKSensorModuleType.LOCATION, "LOCATION");
        sensorNames.put(SKSensorModuleType.MAGNETOMETER, "MAGNETOMETER");
        sensorNames.put(SKSensorModuleType.ROTATION, "ROTATION");

        CheckBox checkAccelero = findViewById(R.id.check_accel);
        CheckBox checkGyro = findViewById(R.id.check_gyro);
        CheckBox checkGPS = findViewById(R.id.check_gps);
        CheckBox checkRotation = findViewById(R.id.check_rotation);
        CheckBox checkOrientation = findViewById(R.id.check_orientation);

        checkAccelero.setOnCheckedChangeListener(this);
        checkGyro.setOnCheckedChangeListener(this);
        checkGPS.setOnCheckedChangeListener(this);
        checkRotation.setOnCheckedChangeListener(this);
        checkOrientation.setOnCheckedChangeListener(this);

        try {
            mSensingKitLib = SensingKitLib.getSensingKitLib(this);
            for(SKSensorModuleType smt: sensors) {
                try {
                    mSensingKitLib.registerSensorModule(smt);
                    mSensingKitLib.subscribeSensorDataListener(smt, this);
                } catch (java.lang.NoClassDefFoundError ncdf) {
                    ncdf.printStackTrace();
                    Toast.makeText(this, sensorNames.get(smt) + " is not available!", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (SKException e) {
            e.printStackTrace();
        }
    }

    SensingKitLibInterface mSensingKitLib;

    public void checkSelected(View v) {
        // TODO - Add implementation for reading respective sensors data

        boolean isChecked = ((CheckBox) v).isChecked();
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
                Utils.logE(getClass().getName(), "switch default - shouldn't have occurred!!");
                break;
        }
    }


    protected void registerSensor(SKSensorModuleType sensorModuleType) {
        try {
            if(!mSensingKitLib.isSensorModuleRegistered(sensorModuleType)) {
                mSensingKitLib.registerSensorModule(sensorModuleType);
            }
        } catch (SKException e) {
            e.printStackTrace();
        }
    }

    protected void unregisterSensor(SKSensorModuleType sensorModuleType) {
        try {
            if(mSensingKitLib.isSensorModuleRegistered(sensorModuleType)) {
                mSensingKitLib.deregisterSensorModule(sensorModuleType);
            }
        } catch (SKException e) {
            e.printStackTrace();
        }
    }
    protected void subscribeToSensor(SKSensorModuleType sensorModuleType) {
        try {
            if(!mSensingKitLib.isSensorModuleSensing(sensorModuleType)) {
                mSensingKitLib.startContinuousSensingWithSensor(sensorModuleType);
            }
        } catch (SKException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    protected void unsubscribeSensor(SKSensorModuleType sensorModuleType) {
        try {
            if(mSensingKitLib.isSensorModuleSensing(sensorModuleType)) {
                mSensingKitLib.stopContinuousSensingWithSensor(sensorModuleType);
            }
        } catch (SKException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        // unsub all checked sensors
        for(SKSensorModuleType smt: sensors) {
            unsubscribeSensor(smt);
            unregisterSensor(smt);
        }
        super.onDestroy();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        SKSensorModuleType moduleType = null;
        switch (compoundButton.getId()) {
            case R.id.check_accel:
                moduleType = SKSensorModuleType.ACCELEROMETER;
                break;
            case R.id.check_gyro:
                moduleType = SKSensorModuleType.GYROSCOPE;
                break;
            case R.id.check_gps:
                moduleType = SKSensorModuleType.LOCATION;
                break;
            case R.id.check_orientation:
                moduleType = SKSensorModuleType.MAGNETOMETER;
                break;
            case R.id.check_rotation:
                moduleType = SKSensorModuleType.ROTATION;
                break;
            default:
                Utils.logE(getClass().getName(), "switch default - shouldn't have occurred");
                break;
        }

        if(moduleType != null) {
            if(b) {
                subscribeToSensor(moduleType);
            } else {
                unsubscribeSensor(moduleType);
            }
        }
    }

    @Override
    public void onDataReceived(final SKSensorModuleType moduleType, final SKSensorData sensorData) {
        switch (moduleType) {
            case ACCELEROMETER:
                SKAccelerometerData accelerometerData = (SKAccelerometerData) sensorData;
                Utils.logE(getClass().getName(), "Accelerometer: X = " + accelerometerData.getX() + "; Y = " + accelerometerData.getY());
                break;
            case GYROSCOPE:
                SKGyroscopeData gyroData = (SKGyroscopeData) sensorData;
                Utils.logE(getClass().getName(), "Gyroscope: X = " + gyroData.getX() + "; Y = " + gyroData.getY());
                break;
            case LOCATION:
                SKLocationData gpsData = (SKLocationData) sensorData;
                Utils.logE(getClass().getName(), "GPS: X = " + gpsData.getLocation().getLatitude() + "; Y = " + gpsData.getLocation().getLongitude());
                break;
            case MAGNETOMETER:
                SKMagnetometerData orientationData = (SKMagnetometerData) sensorData;
                Utils.logE(getClass().getName(), "Orientation: X = " + orientationData.getX() + "; Y = " + orientationData.getY());
                break;
            case ROTATION:
                SKRotationData rotationData = (SKRotationData) sensorData;
                Utils.logE(getClass().getName(), "Rotation: X = " + rotationData.getX() + "; Y = " + rotationData.getY());
                break;
        }
    }
}
