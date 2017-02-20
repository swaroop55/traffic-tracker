package ps.android.com.traffictracker.helpers;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by satyanarayana.p on 20/02/17.
 */

public class DateTimeUtil {

    private static final String DATE_TIME_FORMAT = "dd/MM/yyyy hh:mm";

    public String getTimeFromMillis(long millis) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_FORMAT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return format.format(calendar.getTime());
    }
}
