package com.edexelroots.android.sensoriot;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.auth.core.DefaultSignInResultHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.ui.AuthUIConfiguration;
import com.amazonaws.mobile.auth.ui.SignInActivity;
import com.amazonaws.mobile.auth.ui.SignInUI;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.edexelroots.android.sensoriot.kinesis.KinesisActivity;
import com.edexelroots.android.sensoriot.kinesis.fragments.StreamConfigurationFragment;
import com.edexelroots.android.sensoriot.kinesis.fragments.StreamingFragment;

import org.json.JSONObject;
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
        NavigationView.OnNavigationItemSelectedListener,
        SKSensorDataListener {

    SKSensorModuleType[] sensors = {
            SKSensorModuleType.ACCELEROMETER,
            SKSensorModuleType.GYROSCOPE,
            SKSensorModuleType.LOCATION,
            SKSensorModuleType.MAGNETOMETER,
            SKSensorModuleType.ROTATION
    };

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 111;

    HashMap<SKSensorModuleType, String> sensorNames = new HashMap<>(5);
    public boolean mPublishToAWSIoT = false;
    MqttPublishManager mPublishManager = null;
    AWSIoTConnectionStatus mConnectionStatus = null;
    SensingKitLibInterface mSensingKitLib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        sensorNames.put(SKSensorModuleType.ACCELEROMETER, "Accelerometer");
        sensorNames.put(SKSensorModuleType.GYROSCOPE, "Gyroscope");
        sensorNames.put(SKSensorModuleType.LOCATION, "Location");
        sensorNames.put(SKSensorModuleType.MAGNETOMETER, "Orientation");
        sensorNames.put(SKSensorModuleType.ROTATION, "Rotation");

        CheckBox checkAccelero = findViewById(R.id.check_accel);
        CheckBox checkGyro = findViewById(R.id.check_gyro);
        CheckBox checkGPS = findViewById(R.id.check_gps);
        CheckBox checkRotation = findViewById(R.id.check_rotation);
        CheckBox checkOrientation = findViewById(R.id.check_orientation);

        CheckBox checkPublishToAWS = findViewById(R.id.check_publish_iot);
        if(checkPublishToAWS != null) {
            this.mPublishToAWSIoT = checkPublishToAWS.isChecked();

            checkPublishToAWS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    mPublishToAWSIoT = b;
                }
            });
        }

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

        checkPerms();
        signInAWSCognito();
    }

    public void checkPerms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,  Manifest.permission.ACCESS_FINE_LOCATION)) {
                /* Show an explanation to the user *asynchronously* -- don't block this thread waiting for the user's response! After the user
                 sees the explanation, try again to request the permission. */
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            }
        } else {
            // Permission has already been granted
        }
    }
    protected void signInAWSCognito() {
        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {

                final IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
                if(!identityManager.isUserSignedIn()) {
                    AuthUIConfiguration config =
                            new AuthUIConfiguration.Builder()
                                    .userPools(true)  // true? show the Email and Password UI
                                    .backgroundColor(Color.BLUE) // Change the backgroundColor
                                    .isBackgroundColorFullScreen(true) // Full screen backgroundColor the backgroundColor full screenff
                                    .fontFamily("sans-serif-light") // Apply sans-serif-light as the global font
                                    .canCancel(true)
                                    .build();

                    SignInUI signInUI = (SignInUI) AWSMobileClient.getInstance().getClient(MainActivity.this, SignInUI.class);
                    signInUI.login(MainActivity.this, MainActivity.class).authUIConfiguration(config).execute();
                }
                mConnectionStatus = new AWSIoTConnectionStatus(MainActivity.this, (TextView) findViewById(R.id.label_aws_connect));
                mPublishManager =  new MqttPublishManager(MainActivity.this, mConnectionStatus);
                mPublishManager.setupSession();
            }
        }).execute();
    }
    protected void registerSensor(SKSensorModuleType sensorModuleType) {
        try {
//            if(!mSensingKitLib.isSensorModuleRegistered(sensorModuleType)) {
                mSensingKitLib.registerSensorModule(sensorModuleType);
                mSensingKitLib.subscribeSensorDataListener(sensorModuleType, this);
//            }
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
    protected void onPause() {
        mPublishToAWSIoT = false;
        super.onPause();
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
        double x = 0, y, z, w;
        switch (moduleType) {
            case ACCELEROMETER:
                SKAccelerometerData accelerometerData = (SKAccelerometerData) sensorData;
                ((TextView) findViewById(R.id.value_x_accelerometer)).setText(String.format("X  = %.20f", (x = accelerometerData.getX())));
                ((TextView) findViewById(R.id.value_y_accelerometer)).setText(String.format("Y  = %.20f", (y = accelerometerData.getY())));
                ((TextView) findViewById(R.id.value_z_accelerometer)).setText(String.format("Z  = %.20f", ((z = accelerometerData.getZ()))));
                msg = sensorNames.get(moduleType) + ": X = " + x + ", Y = " + y + ", Z = " + z;
                try {
                    JSONObject readings = new JSONObject();
                    readings.put("x", x);
                    readings.put("y", y);
                    readings.put("z", z);

                    JSONObject jo = new JSONObject();
                    jo.put(sensorNames.get(moduleType), readings);

                    JSONObject reported = new JSONObject();
                    reported.put("reported", jo);

                    JSONObject state = new JSONObject();
                    state.put("state", reported);

                    msg = state.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case GYROSCOPE:
                SKGyroscopeData gyroData = (SKGyroscopeData) sensorData;
                ((TextView) findViewById(R.id.value_x_gyro)).setText(String.format("X  = %.20f", (x = gyroData.getX())));
                ((TextView) findViewById(R.id.value_y_gyro)).setText(String.format("Y  = %.20f", (y = gyroData.getY())));
                ((TextView) findViewById(R.id.value_z_gyro)).setText(String.format("Z  = %.20f", (z = gyroData.getZ())));
                try {
                    JSONObject readings = new JSONObject();
                    readings.put("x", x);
                    readings.put("y", y);
                    readings.put("z", z);

                    JSONObject jo = new JSONObject();
                    jo.put(sensorNames.get(moduleType), readings);

                    JSONObject reported = new JSONObject();
                    reported.put("reported", jo);

                    JSONObject state = new JSONObject();
                    state.put("state", reported);

                    msg = state.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case LOCATION:
                SKLocationData gpsData = (SKLocationData) sensorData;
                ((TextView) findViewById(R.id.value_x_gps)).setText(String.format("Lon  = %.20f", (x = gpsData.getLocation().getLongitude())));
                ((TextView) findViewById(R.id.value_y_gps)).setText(String.format("Lat  = %.20f", (y = gpsData.getLocation().getLatitude())));
                ((TextView) findViewById(R.id.value_z_gps)).setText(String.format("Alt  = %.20f", (z = gpsData.getLocation().getAltitude())));
                try {
                    JSONObject readings = new JSONObject();
                    readings.put("longitude", x);
                    readings.put("latitude", y);
                    readings.put("altitude", z);

                    JSONObject jo = new JSONObject();
                    jo.put(sensorNames.get(moduleType), readings);

                    JSONObject reported = new JSONObject();
                    reported.put("reported", jo);

                    JSONObject state = new JSONObject();
                    state.put("state", reported);

                    msg = state.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case ROTATION:
                SKRotationData rotationData = (SKRotationData) sensorData;
                ((TextView) findViewById(R.id.value_x_rotation)).setText(String.format("X  = %.20f", (x = rotationData.getX())));
                ((TextView) findViewById(R.id.value_y_rotation)).setText(String.format("Y  = %.20f", (y = rotationData.getY())));
                ((TextView) findViewById(R.id.value_z_rotation)).setText(String.format("Z  = %.20f", (z = rotationData.getZ())));

                // float[] result = rotationVectorAction(new float[]{(float) x, (float) y, (float) z});

                w = rotationData.getCos();
                if(w == 0) {
                    w = getRotationScalarComponent(x,y,z);
                }
                ((TextView) findViewById(R.id.value_w_rotation)).setText(String.format("W  = %.20f", w));

                try {
                    JSONObject readings = new JSONObject();
                    readings.put("x", x);
                    readings.put("y", y);
                    readings.put("z", z);
                    readings.put("w", w);

                    JSONObject jo = new JSONObject();
                    jo.put(sensorNames.get(moduleType), readings);

                    JSONObject reported = new JSONObject();
                    reported.put("reported", jo);

                    JSONObject state = new JSONObject();
                    state.put("state", reported);

                    msg = state.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case MAGNETOMETER:
                SKMagnetometerData orientationData = (SKMagnetometerData) sensorData;
                ((TextView) findViewById(R.id.value_x_orientation)).setText(String.format("X  = %.20f", (x = orientationData.getX())));
                ((TextView) findViewById(R.id.value_y_orientation)).setText(String.format("Y  = %.20f", (y = orientationData.getY())));
                ((TextView) findViewById(R.id.value_z_orientation)).setText(String.format("Z  = %.20f", (z = orientationData.getZ())));
                try {
                    JSONObject readings = new JSONObject();
                    readings.put("x", x);
                    readings.put("y", y);
                    readings.put("z", z);

                    JSONObject jo = new JSONObject();
                    jo.put(sensorNames.get(moduleType), readings);

                    JSONObject reported = new JSONObject();
                    reported.put("reported", jo);

                    JSONObject state = new JSONObject();
                    state.put("state", reported);

                    msg = state.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

        if(mPublishToAWSIoT && !TextUtils.isEmpty(msg)) {
            // publish data to AWS IoT for sensors
            Utils.logE(getClass().getName(), msg);
            if(mPublishManager != null) {
                mPublishManager.publishToAwsIoT(msg);
            }
        }
    }

    private float[] rotationVectorAction(float[] values) {
        float[] result = new float[3];
        float vec[] = values;
        float[] orientation = new float[3];
        float[] rotMat = new float[9];
        SensorManager.getRotationMatrixFromVector(rotMat, vec);
        SensorManager.getOrientation(rotMat, orientation);
        result[0] = (float) orientation[0]; // Yaw
        result[1] = (float) orientation[1]; // Pitch
        result[2] = (float) orientation[2]; // Roll
        return result;
    }

    private double getRotationScalarComponent(double x, double y, double z) {
        //return Math.acos(Math.sqrt(x*x + y*y + z*z)) * 2;
        return Math.sqrt(x*x + y*y + z*z);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        int id = item.getItemId();
        if(id == R.id.nav_camera) {
            // show record screen
            Intent i = new Intent(this, KinesisActivity.class);
            startActivity(i);
        } else if(id == R.id.nav_logout) {
            // logout the user
            IdentityManager.getDefaultIdentityManager().signOut();
            IdentityManager.getDefaultIdentityManager().login(this, new DefaultSignInResultHandler() {
                @Override
                public void onSuccess(Activity callingActivity, IdentityProvider provider) {
                    // startConfigFragment();
                    // Toast.makeText(callingActivity.getApplicationContext(), "", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(MainActivity.this, KinesisActivity.class);
                    startActivity(i);
                }

                @Override
                public boolean onCancel(Activity callingActivity) {
                    return false;
                }
            });
            SignInActivity.startSignInActivity(this, new AuthUIConfiguration.Builder().userPools(true).build());
        }

        return true;
    }


}

/*
{
    "state":{
        "reported":{
            "rotation": {
                "x":1,
                "y":1,
                "z":0,
                "w":0
            },
            "accelerometer":{
                "x":1,
                "y":1,
                "z":0
            },
            "gyroscope":{
                "x":1,
                "y":1,
                "z":0
            }
        }
    }
}
*/