package org.ucsd.e4e.djidatatool;

import androidx.annotation.NonNull;

public class AircraftData {
    private final String model;
    private final double roll;
    private final double pitch;
    private final double yaw;
    private final double latitude;
    private final double longitude;
    private final double altitude;
    private final double xVelocity;
    private final double yVelocity;
    private final double zVelocity;
    private final double heading;
    private final long timestamp;

    public AircraftData(
            String model,
            double roll,
            double pitch,
            double yaw,
            double latitude,
            double longitude,
            double altitude,
            double xVelocity,
            double yVelocity,
            double zVelocity,
            double heading,
            long timestamp) {
        this.model = model;
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.xVelocity = xVelocity;
        this.yVelocity = yVelocity;
        this.zVelocity = zVelocity;
        this.heading = heading;
        this.timestamp = timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Model: ");
        builder.append(model);
        builder.append("\n");

        builder.append("Roll: ");
        builder.append(roll);
        builder.append("\n");

        builder.append("Pitch: ");
        builder.append(pitch);
        builder.append("\n");

        builder.append("Yaw: ");
        builder.append(yaw);
        builder.append("\n");

        builder.append("Latitude: ");
        builder.append(latitude);
        builder.append("\n");

        builder.append("Longitude: ");
        builder.append(longitude);
        builder.append("\n");

        builder.append("Altitude: ");
        builder.append(altitude);
        builder.append("m\n");

        builder.append("Velocity (x, y, z) m/s: (");
        builder.append(xVelocity);
        builder.append(", ");
        builder.append(yVelocity);
        builder.append(", ");
        builder.append(zVelocity);
        builder.append(")\n");

        builder.append("Heading :");
        builder.append(heading);
        builder.append("\n");

        return builder.toString();
    }
}
