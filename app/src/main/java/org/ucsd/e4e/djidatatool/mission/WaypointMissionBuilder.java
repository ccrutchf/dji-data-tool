package org.ucsd.e4e.djidatatool.mission;

import java.util.ArrayList;
import java.util.List;

public class WaypointMissionBuilder {
    private double flightSpeed;
    private FinishAction finishAction;
    private List<Waypoint> waypoints = new ArrayList<>();

    public WaypointMissionBuilder flightSpeed(double flightSpeed) {
        this.flightSpeed = flightSpeed;

        return this;
    }

    public WaypointMissionBuilder finishAction(FinishAction finishAction) {
        this.finishAction = finishAction;

        return this;
    }

    public WaypointMissionBuilder addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);

        return this;
    }

    public WaypointMission build() {
        return null;
    }
}
