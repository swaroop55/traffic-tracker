package ps.android.com.traffictracker.intentServices;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Created by satyanarayana.p on 22/02/17.
 */

public class GeoTrackingIntentService extends IntentService {

    private static final String TAG = "GeoTrackingIntentService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public GeoTrackingIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        GeofencingEvent event = GeofencingEvent.fromIntent(intent);


        if (event.hasError()) {
            // TODO: Handle error
        } else {
            handleGeofences(event.getTriggeringLocation(), event.getTriggeringGeofences(), event.getGeofenceTransition());
            //TODO mute and unmute device
        }

    }

    private void handleGeofences(Location location, List<Geofence> geoFences, int transition) {

    }

}
