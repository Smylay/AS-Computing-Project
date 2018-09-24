package com.smylay.hr;

import com.smylay.hr.model.Holidays;
import java.util.Date;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

// Utils is a class with various utility methods.
public class Utils {

    /**
     * This method returns the number of days between two give dates.
     *
     * @param startdate the first day of the period of days you want to count.
     * @param enddate the last day of the period of days you want to count.
     * @param dbFacade pass in the DBFacade bean.
     *
     * @return the number of days between two given dates.
     */
    public static Integer numDaysDifference(Date startdate, Date enddate, DBFacade dbFacade) {
        DateTime enddatetime = new DateTime(enddate);
        DateTime startdatetime = new DateTime(startdate);
        Integer i = 0;
        DateTime thisDateTime = startdatetime;
        // A while loop used to count the number of workdays using the counmter i.
        while (!thisDateTime.isAfter(enddatetime)) {
            if (isWorkday(thisDateTime, dbFacade)) {
                i++;
            }
            thisDateTime = thisDateTime.plusDays(1);
        }
        // returns the number of days.
        return i;
    }

    /**
     * This method checks to see if a given day is a workday or not.
     *
     * @param day the day you want to check.
     * @param dbFacade pass in the DBFacade bean.
     *
     * @return Boolean is it a work day or not?
     */
    private static Boolean isWorkday(DateTime day, DBFacade dbFacade) {

        if (day.getDayOfWeek() == DateTimeConstants.SATURDAY || day.getDayOfWeek() == DateTimeConstants.SUNDAY) {
            // if its a weekend return false
            return false;

        } else if (isBankHoliday(day, dbFacade)) {
            // if it is a holiday for all workers return false
            return false;

        } else {
            // if neither of the above then return true
            return true;
        }
    }

    /**
     * This method checks to see if a given day is a holiday for all workers or not.
     *
     * @param day the day you want to check
     * @param dbFacade pass in the DBFacade bean.
     *
     * @return Boolean is it a holiday?
     */
    private static Boolean isBankHoliday(DateTime day, DBFacade dbFacade) {
        // retrieves a public list of holidays for the forseen future.
        List<Holidays> holidates = dbFacade.getAllHolidays();
        LocalDate dayDate = new LocalDate(day);
        // checks if the day provided is a holiday using a for and an if loop.
        for (Holidays thisHoliday : holidates) {
            LocalDate thisDayDate = new LocalDate(thisHoliday.getDate());
            if (thisDayDate.equals(dayDate)) {
                //if it is a holiday return true
                return true;
            }
        }
        // if its not a holiday return false.
        return false;
    }
}
