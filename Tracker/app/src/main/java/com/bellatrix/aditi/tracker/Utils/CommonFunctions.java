package com.bellatrix.aditi.tracker.Utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Aditi on 06-10-2019.
 */

public class CommonFunctions {

    public static long getTimeDifference(Date date) {

        Date currenTime = Calendar.getInstance().getTime();

        return (date.getTime() - currenTime.getTime());
    }
}
