package org.ucsd.e4e.djidatatool;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.battery.BatteryState;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = MainActivity.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static BaseProduct mProduct;
    private Handler mHandler;

    private AircraftDataManager aircraftDataManager;
    private BatteryState batteryState;

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }

        setContentView(R.layout.activity_main);

        //Initialize DJI SDK Manager
        mHandler = new Handler(Looper.getMainLooper());

        // Get the controls necessary to handle the starting of data collection.
        EditText minAltitudeEditText = (EditText)findViewById(R.id.minAltitude);
        EditText maxAltitudeEditText = (EditText)findViewById(R.id.maxAltitude);
        EditText altitudeIntervalEditText = (EditText)findViewById(R.id.altitudeInterval);
        Button uploadButton = (Button)findViewById(R.id.uploadButton);
        Button startButton = (Button)findViewById(R.id.startButton);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int minAltitude = Integer.parseInt(minAltitudeEditText.getText().toString());
                int maxAltitude = Integer.parseInt(maxAltitudeEditText.getText().toString());
                int altitudeInterval = Integer.parseInt(altitudeIntervalEditText.getText().toString());

                AircraftData aircraftData = aircraftDataManager.getAircraftData();
                double latitude = aircraftData.getLatitude();
                double longitude = aircraftData.getLongitude();

                // Create a waypoint mission that will start at minAltitude and go up by altitudeInterval,
                // until it reaches the maxAltitude.  It will use the aircraft's current lat/long for safety reasons.
                // It will attempt to maintain a vertical column.

                WaypointMission.Builder builder = new WaypointMission.Builder();
                builder.autoFlightSpeed(5f); // in m/s
                builder.maxFlightSpeed(10f); // in m/s
                builder.setExitMissionOnRCSignalLostEnabled(true);
                builder.finishedAction(WaypointMissionFinishedAction.GO_HOME);
                builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
                builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);

                for (int i = minAltitude; i <= maxAltitude; i += altitudeInterval) {
                    Waypoint waypoint = new Waypoint();
                    waypoint.altitude = i;
                    waypoint.coordinate = new LocationCoordinate2D(latitude, longitude);
                    waypoint.addAction(new WaypointAction(WaypointActionType.STAY, 60 * 1000)); // Stay at the waypoint for 60 seconds.

                    builder.addWaypoint(waypoint);
                }

                WaypointMission mission = builder.build();
                MissionControl.getInstance().getWaypointMissionOperator().loadMission(mission); // Blocking

                // By using a separate button, we ensure that we can't accidentally start the mission.
                startButton.setEnabled(true);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int minAltitude = Integer.parseInt(minAltitudeEditText.getText().toString());
                int maxAltitude = Integer.parseInt(maxAltitudeEditText.getText().toString());
                int altitudeInterval = Integer.parseInt(altitudeIntervalEditText.getText().toString());

                aircraftDataManager.startDataCollection(minAltitude, maxAltitude, altitudeInterval);
                MissionControl.getInstance().getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        aircraftDataManager.stopDataCollection();
                    }
                });
            }
        });
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast("Need to grant the permissions!");
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast("Missing permissions!!!");
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast("registering, pls wait...");
                    DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                showToast("Register Success");
                                DJISDKManager.getInstance().startConnectionToProduct();
                            } else {
                                showToast("Register sdk fails, please check the bundle id and network connection!");
                            }
                            Log.v(TAG, djiError.getDescription());
                        }

                        @Override
                        public void onProductDisconnect() {
                            Log.d(TAG, "onProductDisconnect");
                            showToast("Product Disconnected");
                            notifyStatusChange();

                        }
                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
                            showToast("Product Connected");
                            notifyStatusChange();

                        }

                        @Override
                        public void onProductChanged(BaseProduct baseProduct) {

                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                      BaseComponent newComponent) {

                            if (newComponent != null) {
                                newComponent.setComponentListener(new BaseComponent.ComponentListener() {

                                    @Override
                                    public void onConnectivityChange(boolean isConnected) {
                                        Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                        notifyStatusChange();
                                    }
                                });
                            }
                            Log.d(TAG,
                                    String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                            componentKey,
                                            oldComponent,
                                            newComponent));

                        }
                        @Override
                        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                        }

                        @Override
                        public void onDatabaseDownloadProgress(long l, long l1) {

                        }
                    });
                }
            });
        }
    }

    // Called when product state changes.
    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);

        MainActivity that = this;

        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        if (aircraft != null) {
            aircraftDataManager = new AircraftDataManager();

            aircraft.getBattery().setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(BatteryState batteryState) {
                    that.batteryState = batteryState;

                    updateAircraftStatus();
                }
            });

            aircraftDataManager.setAircraftDataChanged(new AircraftDataChanged() {
                @Override
                public void AircraftDataChanged(AircraftData aircraftData) {
                    updateAircraftStatus();
                }
            });
        }
    }

    private void updateAircraftStatus() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView aircraftStatus = (TextView)findViewById(R.id.aircraftStatus);
                aircraftStatus.setText(getAircraftStatus());
            }
        });
    }

    private String getAircraftStatus() {
        if (batteryState == null) {
            return "";
        }

        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        FlightControllerState flightControllerState = aircraft.getFlightController().getState();

        StringBuilder builder = new StringBuilder();

        builder.append("Battery: ");
        builder.append(batteryState.getChargeRemainingInPercent());
        builder.append("%\n");

        builder.append("GPS Signal Strength: ");
        builder.append(flightControllerState.getSatelliteCount());
        builder.append("\n\n");

        builder.append(aircraftDataManager.getAircraftData());

        return builder.toString();
    }

    private final Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };

    private void showToast(final String toastMsg) {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
            }
        });

    }
}