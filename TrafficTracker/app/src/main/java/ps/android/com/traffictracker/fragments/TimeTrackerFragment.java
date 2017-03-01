package ps.android.com.traffictracker.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import ps.android.com.traffictracker.R;

import static android.app.Activity.RESULT_OK;

/**
 * Created by satyanarayana.p on 22/02/17.
 */

public class TimeTrackerFragment extends Fragment implements View.OnClickListener {

    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 200.0f; // in meters

    private int SOURCE_PLACE_PICKER_REQUEST = 1;
    private int DESTINATION_PLACE_PICKER_REQUEST = 2;
    private PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

    private EditText sourceEditText;
    private EditText destinationEditText;
    private Button searchButton;

    private Place source;
    private Place destination;

    public interface GoogleAPIClientListener {
        void addGeofence(GeofencingRequest request);
    }

    private GoogleAPIClientListener googleAPIClientListener;

    @UIMode
    private int currentUIMode = INITIAL;

    public static TimeTrackerFragment newInstance() {
        return new TimeTrackerFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_time_tracker, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        googleAPIClientListener = (GoogleAPIClientListener) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        initViews();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SOURCE_PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                source = PlacePicker.getPlace(data, getActivity());
                if (source != null) {
                    sourceEditText.setText(source.getAddress());
                    refreshSearchButton();
                }
            }
        } else if (requestCode == DESTINATION_PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                destination = PlacePicker.getPlace(data, getActivity());
                if (destination != null) {
                    destinationEditText.setText(destination.getAddress());
                    refreshSearchButton();
                }
            }
        }
    }

    private void initViews() {

        sourceEditText = (EditText) getView().findViewById(R.id.time_tracker_source_address);
        sourceEditText.setInputType(InputType.TYPE_NULL);
        sourceEditText.setOnClickListener(this);

        destinationEditText = (EditText) getView().findViewById(R.id.time_tracker_destination_address);
        destinationEditText.setInputType(InputType.TYPE_NULL);
        destinationEditText.setOnClickListener(this);

        searchButton = (Button) getView().findViewById(R.id.time_tracker_search_button);
        searchButton.setOnClickListener(this);

        adjustUI(INITIAL);

    }


    private void refreshSearchButton() {
        if (source != null && destination != null) {
            searchButton.setEnabled(true);
        } else {
            searchButton.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.time_tracker_source_address:
                try {
                    startActivityForResult(builder.build(getActivity()), SOURCE_PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.time_tracker_destination_address:
                try {
                    startActivityForResult(builder.build(getActivity()), DESTINATION_PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.time_tracker_search_button:
                addGeofences();
                //if (!findDurationInitiated)
                //  analyseTimings();
                break;
            default:
                break;
        }
    }

    /* Helper UI function to set the UI according to different modes */
    private void adjustUI(@UIMode int mode) {
        switch (mode) {
            case INITIAL:
                break;
            case FETCHING:
                break;
            case FETCHED:
                break;
            case ERROR_FETCHING:
                //Do something
                break;
        }
        currentUIMode = mode;
    }


    private void addGeofences() {
        List<Geofence> geoFences = new ArrayList<>();
        geoFences.add(createGeoFence(source.getLatLng(), GEOFENCE_RADIUS));
        geoFences.add(createGeoFence(destination.getLatLng(), GEOFENCE_RADIUS));

        GeofencingRequest request = createGeofenceRequest(geoFences);
        if (googleAPIClientListener != null) {
            googleAPIClientListener.addGeofence(request);
        }
    }

    private Geofence createGeoFence(LatLng latLng, float radius) {
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setExpirationDuration(-1)
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    private GeofencingRequest createGeofenceRequest(List<Geofence> geofences) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build();
    }

    //*********************************************************************
    // Inner Classes/Enums/Interfaces
    //*********************************************************************

    /**
     * Annotation Interface to take care of present UI state
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({INITIAL, FETCHING, FETCHED,
            ERROR_FETCHING})
    private @interface UIMode {
    }

    private static final int INITIAL = 0;
    private static final int FETCHING = 1;
    private static final int FETCHED = 2;
    private static final int ERROR_FETCHING = 3;
}
