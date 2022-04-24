package org.ucsd.e4e.djidatatool;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.os.Environment;

import androidx.annotation.NonNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import dji.common.flightcontroller.CompassState;
import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class AircraftDataManager {
    private final Aircraft aircraft;
    private final ThreadPoolExecutor threadPoolExecutor;
    private SQLiteDatabase database;
    private AircraftDataChanged aircraftDataChanged;

    public AircraftDataManager() {
        aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        // We don't know how many threads we need.  This threadpool will create them lazily.
        threadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        // These callbacks are called 10 times a second.
        aircraft.getFlightController().setStateCallback(new FlightControllerState.Callback() {
            @Override
            public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                aircraftDataChanged();
            }
        });
        aircraft.getFlightController().getCompass().setCompassStateCallback(new CompassState.Callback() {
            @Override
            public void onUpdate(@NonNull CompassState compassState) {
                aircraftDataChanged();
            }
        });
    }

    public void startDataCollection(int minAltitude, int maxAltitude, int altitudeInterval) {
        if (database != null) {
            return;
        }

        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File dbPath = new File(root, String.format("%s_%s_%s_%s_%s.db",
                aircraft.getModel().getDisplayName().toLowerCase(Locale.ROOT).replace(' ', '-'),
                minAltitude,
                maxAltitude,
                altitudeInterval,
                new SimpleDateFormat("yyyyMMddHHmm").format(new Date())));
        database = SQLiteDatabase.openOrCreateDatabase(dbPath.getPath(), null);

        database.beginTransaction();

        database.execSQL("CREATE TABLE Metadata (name TEXT, value TEXT)");
        database.execSQL("CREATE TABLE FlightData (" +
                "roll REAL, pitch REAL, yaw REAL," +
                "latitude REAL, longitude REAL, altitude REAL," +
                "velocityX REAL, velocityY REAL, velocityZ REAL," +
                "heading REAL, timestamp INTEGER)");

        // Save all the metadata so that we know how this was generated.
        insertMetadata("aircraft_model", getAircraftData().getModel());
        insertMetadata("min_altitude", Integer.toString(minAltitude));
        insertMetadata("max_altitude", Integer.toString(maxAltitude));
        insertMetadata("altitude_interval", Integer.toString(altitudeInterval));
        insertMetadata("date", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public void stopDataCollection() {
        if (database == null) {
            return;
        }

        database.close();
        database = null;
    }

    public void setAircraftDataChanged(AircraftDataChanged aircraftDataChanged) {
        this.aircraftDataChanged = aircraftDataChanged;
    }

    public AircraftData getAircraftData() {
        FlightControllerState flightControllerState = aircraft.getFlightController().getState();
        Compass compass = aircraft.getFlightController().getCompass();

        return new AircraftData(
                aircraft.getModel().getDisplayName(),
                flightControllerState.getAttitude().roll,
                flightControllerState.getAttitude().pitch,
                flightControllerState.getAttitude().yaw,
                flightControllerState.getAircraftLocation().getLatitude(),
                flightControllerState.getAircraftLocation().getLongitude(),
                flightControllerState.getAircraftLocation().getAltitude(),
                flightControllerState.getVelocityX(),
                flightControllerState.getVelocityY(),
                flightControllerState.getVelocityZ(),
                compass.getHeading(),
                System.nanoTime()); // Benchmarks done indicate this is the most efficient/accurate way of getting time.
    }

    private void insertAircraftData(AircraftData aircraftData) {
        ContentValues values = new ContentValues();
        values.put("roll", aircraftData.getRoll());
        values.put("pitch", aircraftData.getPitch());
        values.put("yaw", aircraftData.getYaw());
        values.put("latitude", aircraftData.getLatitude());
        values.put("longitude", aircraftData.getLongitude());
        values.put("altitude", aircraftData.getAltitude());
        values.put("velocityX", aircraftData.getVelocityX());
        values.put("velocityY", aircraftData.getVelocityY());
        values.put("velocityZ", aircraftData.getVelocityZ());
        values.put("heading", aircraftData.getHeading());
        values.put("timestamp", aircraftData.getTimestamp());

        database.insert("FlightData", null, values);
    }

    private void insertMetadata(String name, String value) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("value", value);
        database.insert("Metadata", null, values);
    }

    private void aircraftDataChanged() {
        AircraftData aircraftData = getAircraftData();

        if (aircraftDataChanged != null) {
            aircraftDataChanged.AircraftDataChanged(aircraftData);
        }

        if (database != null) {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    insertAircraftData(aircraftData);
                }
            });
        }
    }
}
