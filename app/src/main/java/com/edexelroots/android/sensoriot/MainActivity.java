package com.edexelroots.android.sensoriot;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.amazonaws.mobile.auth.core.DefaultSignInResultHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.ui.AuthUIConfiguration;
import com.amazonaws.mobile.auth.ui.SignInActivity;
import com.edexelroots.android.sensoriot.kinesis.KinesisActivity;
import com.edexelroots.android.sensoriot.kinesis.fragments.SensorFragment;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        SensorFragment.OnFragmentInteractionListener
    {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
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


        @Override
        public void onFragmentInteraction(Uri uri) {
            // Todo -- anything ?
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