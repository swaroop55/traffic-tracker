package ps.android.com.traffictracker.helpers;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by satyanarayana.p on 20/02/17.
 */

public class DateTimeUtil {

    private static final String DATE_TIME_FORMAT = "hh:mm";

    public static String getTimeFromMillis(long millis) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_FORMAT);
        format.setTimeZone(TimeZone.getTimeZone("IST"));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return format.format(calendar.getTime());
    }
}
