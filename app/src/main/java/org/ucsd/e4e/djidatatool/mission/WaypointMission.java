package org.ucsd.e4e.djidatatool.mission;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class WaypointMission {
    private final double flightSpeed;
    private final FinishAction finishAction;
    private final List<Waypoint> waypoints;
    private final ThreadPoolExecutor threadPoolExecutor;

    private int waypointIndex;

    public WaypointMission(double flightSpeed, FinishAction finishAction, List<Waypoint> waypoints) {
        this.flightSpeed = flightSpeed;
        this.finishAction = finishAction;
        this.waypoints = waypoints;

        threadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    public void start(Runnable runnable) {
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
                FlightController flightController = aircraft.getFlightController();

                // Support velocity with respect to the ground.
                flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.GROUND);

                // In order to send controller input, you must enable virtual stick mode.
                // Switching modes disables virtual stick mode.
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        waypointIndex = 0;

                        // Called 10 times a second.
                        // No point to do this if we don't have new info.
                        aircraft.getFlightController().setStateCallback(new FlightControllerState.Callback() {
                            @Override
                            public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                                Waypoint waypoint = waypoints.get(waypointIndex);
                                LocationCoordinate3D location = flightController.getState().getAircraftLocation();

                                double x = (waypoint.getLatitude() - location.getLatitude())*distPerLat(location.getLatitude());
                                double y = (waypoint.getLongitude() - location.getLongitude())*distPerLng(location.getLatitude());
                                double z = waypoint.getAltitude() - location.getAltitude();

                                double distance = Math.sqrt(x*x + y*y + z*z);

                                if (distance <= 0.005) { // 5cm accuracy
                                    waypointIndex++;
                                }

                                // Check for last waypoint
                                if (waypointIndex < waypoints.size()) {
                                    goToWaypoint(aircraft, waypoint);
                                } else {
                                    // Done
                                    finishMission(aircraft, runnable);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void finishMission(Aircraft aircraft, Runnable runnable) {
        FlightController flightController = aircraft.getFlightController();
        switch (finishAction) {
            case GO_HOME:
                flightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        runnable.run();
                    }
                });
                break;

            case STAY:
            default:
                if (runnable != null) {
                    runnable.run();
                }
                break;
        }
    }

    private void goToWaypoint(Aircraft aircraft, Waypoint waypoint) {
        FlightController flightController = aircraft.getFlightController();
        LocationCoordinate3D location = flightController.getState().getAircraftLocation();

        // https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude
        double x = (waypoint.getLatitude() - location.getLatitude())*distPerLat(location.getLatitude());
        double y = (waypoint.getLongitude() - location.getLongitude())*distPerLng(location.getLatitude());
        double z = waypoint.getAltitude() - location.getAltitude();

        double distance = Math.sqrt(x*x + y*y + z*z);
        double alpha = Math.atan(z / x);
        double beta = Math.atan(z / y);

        // https://math.stackexchange.com/questions/3097506/components-of-a-3d-vector-given-specific-angles
        double speed = Math.min(flightSpeed * (distance / flightSpeed), flightSpeed);
        double denom = Math.sqrt(Math.pow(Math.cos(beta), 2) * Math.pow(Math.sin(alpha), 2) + Math.pow(Math.sin(beta), 2));
        float velocityX = (float)Math.min(speed * Math.cos(alpha) * Math.sin(beta) / denom, speed);
        float velocityY = (float)Math.min(speed * Math.cos(beta) * Math.sin(alpha) / denom, speed);
        float velocityZ = (float)Math.min(speed * Math.sin(alpha) * Math.sin(beta) / denom, speed);

        flightController.sendVirtualStickFlightControlData(new FlightControlData(velocityX, velocityY, 0, velocityZ), new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });
    }

    private static double distPerLng(double lat){
        return 0.0003121092*Math.pow(lat, 4)
                +0.0101182384*Math.pow(lat, 3)
                -17.2385140059*lat*lat
                +5.5485277537*lat+111301.967182595;
    }

    private static double distPerLat(double lat){
        return -0.000000487305676*Math.pow(lat, 4)
                -0.0033668574*Math.pow(lat, 3)
                +0.4601181791*lat*lat
                -1.4558127346*lat+110579.25662316;
    }
}
