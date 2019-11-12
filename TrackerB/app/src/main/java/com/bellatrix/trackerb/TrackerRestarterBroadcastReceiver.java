package com.bellatrix.trackerb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class TrackerRestarterBroadcastReceiver   extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Service", "Service Stops!");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, TrackerService.class));
        } else {
            context.startService(new Intent(context, TrackerService.class));
        }
    }
}