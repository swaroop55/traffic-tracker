package ps.android.com.traffictracker.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.Calendar;

import ps.android.com.traffictracker.R;
import ps.android.com.traffictracker.helpers.MapsDirectionResponseHelper;
import ps.android.com.traffictracker.network.TrafficTrackerAPI;
import ps.android.com.traffictracker.network.TrafficTrackerApiClient;
import ps.android.com.traffictracker.network.response.MapsDirectionResponse;

import static android.app.Activity.RESULT_OK;

/**
 * Created by satyanarayana.p on 06/02/17.
 */

public class SelectLocationsFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    public static String TAG = "SelectLocationsFrag";
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    private static final String BEST_GUESS = "best_guess";

    private static final int MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000;
    private static final int FRACTION = 12;

    private int SOURCE_PLACE_PICKER_REQUEST = 1;
    private int DESTINATION_PLACE_PICKER_REQUEST = 2;
    private PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    private GoogleApiClient mGoogleApiClient;

    private EditText sourceEditText;
    private EditText destinationEditText;
    private Button searchButton;

    private Place source;
    private Place destination;


    public static SelectLocationsFragment newInstance() {
        return new SelectLocationsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
        initGoogleApiClient();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_locations, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        initViews();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        } else if (requestCode == SOURCE_PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                source = PlacePicker.getPlace(data, getActivity());
                if (source != null) {
                    sourceEditText.setText(source.getAddress());
                    locationsSelected();
                }
            }
        } else if (requestCode == DESTINATION_PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                destination = PlacePicker.getPlace(data, getActivity());
                if (destination != null) {
                    destinationEditText.setText(destination.getAddress());
                    locationsSelected();
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    private void initGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient
                .Builder(getActivity())
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(getActivity(), this)
                .build();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(getActivity(), REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    private void initViews() {

        sourceEditText = (EditText) getView().findViewById(R.id.source_address);
        sourceEditText.setInputType(InputType.TYPE_NULL);
        sourceEditText.setOnClickListener(this);

        destinationEditText = (EditText) getView().findViewById(R.id.destination_address);
        destinationEditText.setInputType(InputType.TYPE_NULL);
        destinationEditText.setOnClickListener(this);

        searchButton = (Button) getView().findViewById(R.id.search_button);
        searchButton.setOnClickListener(this);

    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getActivity().getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.source_address:
                try {
                    startActivityForResult(builder.build(getActivity()), SOURCE_PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.destination_address:
                try {
                    startActivityForResult(builder.build(getActivity()), DESTINATION_PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.search_button:
                analyseTimings();
                break;
            default:
                break;
        }
    }

    private void locationsSelected() {
        if (source != null && destination != null) {
            searchButton.setEnabled(true);
        } else {
            searchButton.setEnabled(false);
        }
    }

    private void analyseTimings() {
        Calendar c = Calendar.getInstance();
        long seconds = System.currentTimeMillis() + 100000l;
        TrafficTrackerAPI api = TrafficTrackerApiClient.getClient().create(TrafficTrackerAPI.class);

        retrofit2.Call<MapsDirectionResponse> call = api.getDuration(source.getAddress().toString(), destination.getAddress().toString(), seconds, BEST_GUESS, getResources().getString(R.string.maps_api_key));
        call.enqueue(new retrofit2.Callback<MapsDirectionResponse>() {
            @Override
            public void onResponse(retrofit2.Call<MapsDirectionResponse> call, retrofit2.Response<MapsDirectionResponse> response) {
                handleResponse(response.body());
            }

            @Override
            public void onFailure(retrofit2.Call<MapsDirectionResponse> call, Throwable t) {

            }
        });
    }

    private void handleResponse(MapsDirectionResponse response) {
        MapsDirectionResponseHelper helper = new MapsDirectionResponseHelper(response);
        /*final TextView responseView = (TextView) getView().findViewById(R.id.response);

        responseView.setText(helper.getDurationInTraffic());*/

    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            //(SelectLocationsFragment)getContext().onDialogDismissed();
        }
    }
}
