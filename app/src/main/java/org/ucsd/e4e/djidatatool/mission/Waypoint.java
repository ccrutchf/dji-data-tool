package org.ucsd.e4e.djidatatool.mission;

public class Waypoint {
    private final double latitude;
    private final double longitude;
    private final double altitude;

    private WaypointAction action;
    private int actionValue;

    public Waypoint(double latitude, double longitude, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public WaypointAction getAction() {
        return action;
    }

    public int getActionValue() {
        return actionValue;
    }

    public void setAction(WaypointAction action, int actionValue) {
        this.action = action;
        this.actionValue = actionValue;
    }
}
