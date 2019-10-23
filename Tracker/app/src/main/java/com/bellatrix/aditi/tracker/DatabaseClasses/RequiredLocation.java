package com.bellatrix.aditi.tracker.DatabaseClasses;

public class RequiredLocation {

    public double latitude, longitude;

    public RequiredLocation() {
        latitude = 0.0;
        longitude = 0.0;
    }

    public RequiredLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
