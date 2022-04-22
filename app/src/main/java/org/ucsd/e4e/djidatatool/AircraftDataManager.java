package org.ucsd.e4e.djidatatool;

import androidx.annotation.NonNull;

import dji.common.flightcontroller.CompassState;
import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class AircraftDataManager {
    private final Aircraft aircraft;
    private AircraftDataChanged aircraftDataChanged;

    public AircraftDataManager() {
        aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();

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
                System.nanoTime());
    }

    private void aircraftDataChanged() {
        AircraftData aircraftData = getAircraftData();

        if (aircraftDataChanged != null) {
            aircraftDataChanged.AircraftDataChanged(aircraftData);
        }
    }
}
