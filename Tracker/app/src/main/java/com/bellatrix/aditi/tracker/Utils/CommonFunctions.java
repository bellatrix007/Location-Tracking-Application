package com.bellatrix.aditi.tracker.Utils;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Aditi on 06-10-2019.
 */

public class CommonFunctions {

    public static final String MESSAGE_BODY = "Please send your location. Sent by Tracker!";

    public static long getTimeDifference(Date date) {

        Date currenTime = Calendar.getInstance().getTime();

        return (date.getTime() - currenTime.getTime());
    }

    public static String getUrl(LatLng origin, LatLng dest, String directionMode, String API_key) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + API_key;
        return url;
    }
}
