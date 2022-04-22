package org.ucsd.e4e.djidatatool;

import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class AircraftDataManager {
    private final Aircraft aircraft;

    public AircraftDataManager() {
        aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
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
}
