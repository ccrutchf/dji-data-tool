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
    private final double velocityX;
    private final double velocityY;
    private final double velocityZ;
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
            double velocityX,
            double velocityY,
            double velocityZ,
            double heading,
            long timestamp) {
        this.model = model;
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.heading = heading;
        this.timestamp = timestamp;
    }

    public String getModel() {
        return model;
    }

    public double getRoll() {
        return roll;
    }

    public double getPitch() {
        return pitch;
    }

    public double getYaw() {
        return yaw;
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

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getVelocityZ() {
        return velocityZ;
    }

    public double getHeading() {
        return heading;
    }

    public long getTimestamp() {
        return timestamp;
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
        builder.append(velocityX);
        builder.append(", ");
        builder.append(velocityY);
        builder.append(", ");
        builder.append(velocityZ);
        builder.append(")\n");

        builder.append("Heading :");
        builder.append(heading);
        builder.append("\n");

        return builder.toString();
    }
}
