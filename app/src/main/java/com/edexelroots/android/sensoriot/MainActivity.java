package com.edexelroots.android.sensoriot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
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
    public boolean mPublishToAWSIoT = false;
    MqttPublishManager mPublishManager = null;
    AWSIoTConnectionStatus mConnectionStatus = null;
    SensingKitLibInterface mSensingKitLib;

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

        CheckBox checkPublishToAWS = findViewById(R.id.check_publish_iot);
        this.mPublishToAWSIoT = checkPublishToAWS.isChecked();

        checkPublishToAWS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mPublishToAWSIoT = b;
            }
        });

        mConnectionStatus = new AWSIoTConnectionStatus(this, (TextView) findViewById(R.id.label_aws_connect));
        mPublishManager =  new MqttPublishManager(this, mConnectionStatus);
        mPublishManager.connectToAWS();

        checkAccelero.setOnCheckedChangeListener(this);
        checkGyro.setOnCheckedChangeListener(this);
        checkGPS.setOnCheckedChangeListener(this);
        checkRotation.setOnCheckedChangeListener(this);
        checkOrientation.setOnCheckedChangeListener(this);

        try {
            mSensingKitLib = SensingKitLib.getSensingKitLib(this);
            for(SKSensorModuleType smt: sensors) {
                registerSensor(smt);
            }

        } catch (SKException e) {
            e.printStackTrace();
        }
    }

    protected void registerSensor(SKSensorModuleType sensorModuleType) {
        try {
            if(!mSensingKitLib.isSensorModuleRegistered(sensorModuleType)) {
                mSensingKitLib.registerSensorModule(sensorModuleType);
                mSensingKitLib.subscribeSensorDataListener(sensorModuleType, this);
            }
        } catch (SKException e) {
            e.printStackTrace();
        } catch (NoClassDefFoundError ncdf) {
            Toast.makeText(this, sensorNames.get(sensorModuleType) + ": Not available!", Toast.LENGTH_SHORT).show();
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
    protected boolean subscribeToSensor(SKSensorModuleType sensorModuleType) {
        try {
            if(!mSensingKitLib.isSensorModuleSensing(sensorModuleType)) {
                mSensingKitLib.startContinuousSensingWithSensor(sensorModuleType);
            }
            return true;
        } catch (SKException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
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
        mPublishToAWSIoT = false;
        // unsub all checked sensors
        for(SKSensorModuleType smt: sensors) {
            unsubscribeSensor(smt);
            unregisterSensor(smt);
        }
        if(mPublishManager != null) {
            mPublishManager.disconnectMqtt();
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
                if(!subscribeToSensor(moduleType)) {
                    compoundButton.setChecked(false);
                }
            } else {
                unsubscribeSensor(moduleType);
            }
        }
    }

    @Override
    public void onDataReceived(final SKSensorModuleType moduleType, final SKSensorData sensorData) {

        String msg = "";
        double x = 0, y, z;
        switch (moduleType) {
            case ACCELEROMETER:
                SKAccelerometerData accelerometerData = (SKAccelerometerData) sensorData;
//                Utils.logE(getClass().getName(), "Accelerometer: X = " + accelerometerData.getX() + "; Y = " + accelerometerData.getY());
                ((TextView) findViewById(R.id.value_x_accelerometer)).setText(String.format("X  = %.20f", (x = accelerometerData.getX())));
                ((TextView) findViewById(R.id.value_y_accelerometer)).setText(String.format("Y  = %.20f", (y = accelerometerData.getY())));
                ((TextView) findViewById(R.id.value_z_accelerometer)).setText(String.format("Z  = %.20f", ((z = accelerometerData.getZ()))));
                msg = sensorNames.get(moduleType) + ": X = " + x + ", Y = " + y + ", Z = " + z;

                break;

            case GYROSCOPE:
                SKGyroscopeData gyroData = (SKGyroscopeData) sensorData;
//                Utils.logE(getClass().getName(), "Gyroscope: X = " + gyroData.getX() + "; Y = " + gyroData.getY());
                ((TextView) findViewById(R.id.value_x_gyro)).setText(String.format("X  = %.20f", (x = gyroData.getX())));
                ((TextView) findViewById(R.id.value_y_gyro)).setText(String.format("Y  = %.20f", (y = gyroData.getY())));
                ((TextView) findViewById(R.id.value_z_gyro)).setText(String.format("Z  = %.20f", (z = gyroData.getZ())));
                msg = sensorNames.get(moduleType) + ": X = " + x + ", Y = " + y + ", Z = " + z;

                break;

            case LOCATION:
                SKLocationData gpsData = (SKLocationData) sensorData;
//                Utils.logE(getClass().getName(), "GPS: X = " + gpsData.getLocation().getLatitude() + "; Y = " + gpsData.getLocation().getLongitude());
                ((TextView) findViewById(R.id.value_x_gps)).setText(String.format("Lon  = %.20f", (x = gpsData.getLocation().getLongitude())));
                ((TextView) findViewById(R.id.value_y_gps)).setText(String.format("Lat  = %.20f", (y = gpsData.getLocation().getLatitude())));
                ((TextView) findViewById(R.id.value_z_gps)).setText(String.format("Alt  = %.20f", (z = gpsData.getLocation().getAltitude())));
                msg = sensorNames.get(moduleType) + ": X = " + x + ", Y = " + y + ", Z = " + z;

                break;

            case ROTATION:
                SKRotationData rotationData = (SKRotationData) sensorData;
//                Utils.logE(getClass().getName(), "Rotation: X = " + rotationData.getX() + "; Y = " + rotationData.getY());
                ((TextView) findViewById(R.id.value_x_rotation)).setText(String.format("X  = %.20f", (x = rotationData.getX())));
                ((TextView) findViewById(R.id.value_y_rotation)).setText(String.format("Y  = %.20f", (y = rotationData.getY())));
                ((TextView) findViewById(R.id.value_z_rotation)).setText(String.format("Z  = %.20f", (z = rotationData.getZ())));
                msg = sensorNames.get(moduleType) + ": X = " + x + ", Y = " + y + ", Z = " + z;

                break;

            case MAGNETOMETER:
                SKMagnetometerData orientationData = (SKMagnetometerData) sensorData;
//                Utils.logE(getClass().getName(), "Orientation: X = " + orientationData.getX() + "; Y = " + orientationData.getY());
                ((TextView) findViewById(R.id.value_x_orientation)).setText(String.format("X  = %.20f", (x = orientationData.getX())));
                ((TextView) findViewById(R.id.value_y_orientation)).setText(String.format("Y  = %.20f", (y = orientationData.getY())));
                ((TextView) findViewById(R.id.value_z_orientation)).setText(String.format("Z  = %.20f", (z = orientationData.getZ())));
                msg = sensorNames.get(moduleType) + ": X = " + x + ", Y = " + y + ", Z = " + z;

                break;
        }


        if(mPublishToAWSIoT && !TextUtils.isEmpty(msg)) {
            // publish data to AWS IoT for sensors
            Utils.logE(getClass().getName(), msg);
            mPublishManager.publishToAwsIoT(msg);
        }
    }
}
