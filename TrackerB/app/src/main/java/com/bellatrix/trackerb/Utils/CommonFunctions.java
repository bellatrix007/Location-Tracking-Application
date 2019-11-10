package com.bellatrix.trackerb.Utils;

import java.util.Calendar;
import java.util.Date;

public class CommonFunctions {
    public static long getTimeDifference(Date date) {

        Date currenTime = Calendar.getInstance().getTime();

        return (date.getTime() - currenTime.getTime());
    }
}
