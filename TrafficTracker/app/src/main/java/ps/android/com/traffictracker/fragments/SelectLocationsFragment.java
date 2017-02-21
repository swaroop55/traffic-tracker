package ps.android.com.traffictracker.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import okhttp3.HttpUrl;
import ps.android.com.traffictracker.R;
import ps.android.com.traffictracker.helpers.DateTimeUtil;
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
    private static final String PESSIMISTIC = "pessimistic";

    private static final long MILLIS_IN_A_MINUTE = 60 * 1000;
    private static final long MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000;
    // Interval with which duration has to be calculated
    private static final int FRACTION_IN_MINUTES = 30;
    // No. of times the durations has to be calculated. Always keep it odd to get equal partitions before and after the time selected.
    private static final int MULTIPLIER_IN_ODD = 5;
    private static final int DAYS_IN_WEEK_PLUS_ONE = 8;
    private static final int DAYS_IN_WEEK = 7;


    private int SOURCE_PLACE_PICKER_REQUEST = 1;
    private int DESTINATION_PLACE_PICKER_REQUEST = 2;
    private PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    private GoogleApiClient mGoogleApiClient;

    private EditText sourceEditText;
    private EditText destinationEditText;
    private Button searchButton;
    private Spinner timeSpinner;
    private BarChart chart;
    private FrameLayout chartLayout;
    private ProgressBar chartProgressBar;

    private Place source;
    private Place destination;
    private int time = -1;

    private boolean findDurationInitiated = false;

    private int dayOfWeek = 0; //start with 0 i.e Sunday and go till 6 i.e Saturday
    private List<Long> departures = new ArrayList<>();


    @UIMode
    private int currentUIMode = INITIAL;

    private List<List<MapsDirectionResponseHelper>> durationsList = new ArrayList<>();

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

        initSpinners();

        chartLayout = (FrameLayout) getView().findViewById(R.id.layout_chart);
        chart = (BarChart) getView().findViewById(R.id.chart);
        chartProgressBar = (ProgressBar) getView().findViewById(R.id.progress_bar);
        adjustUI(INITIAL);

    }

    private void initSpinners() {
        timeSpinner = (Spinner) getView().findViewById(R.id.spinner_from_time);
        List<Integer> hours = new ArrayList<>();
        for (int i = 0; i <= 24; i++) {
            hours.add(i);
        }
        ArrayAdapter<Integer> fromAdapter = new ArrayAdapter<Integer>(getContext(), android.R.layout.simple_dropdown_item_1line, hours);
        timeSpinner.setAdapter(fromAdapter);
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                time = (int) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
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
                if (!findDurationInitiated)
                    analyseTimings();
                break;
            default:
                break;
        }
    }

    private void refreshSearchButton() {
        if (source != null && destination != null && time != -1 && !findDurationInitiated) {
            searchButton.setEnabled(true);
        } else {
            searchButton.setEnabled(false);
        }
    }

    private void analyseTimings() {
        if (dayOfWeek >= DAYS_IN_WEEK) {
            return;
        }
        if (dayOfWeek == 0) {
            durationsList.clear();
        }
        findDurationInitiated = true;
        adjustUI(FETCHING);
        long millis = getNextWeekInMillis() + dayOfWeek * MILLIS_IN_A_DAY;
        TrafficTrackerAPI api = TrafficTrackerApiClient.getClient().create(TrafficTrackerAPI.class);

        for (int i = 0; i < MULTIPLIER_IN_ODD; i++) {
            int minuteMultiplier = time * 2 - (MULTIPLIER_IN_ODD / 2 - MULTIPLIER_IN_ODD + i + 1); // Todo: Too complex to understand. Rewrite for your own sake.
            long addMillis = minuteMultiplier * FRACTION_IN_MINUTES * MILLIS_IN_A_MINUTE;
            long reqMillis = millis + addMillis;
            retrofit2.Call<MapsDirectionResponse> call = api.getDuration(source.getAddress().toString(), destination.getAddress().toString(), reqMillis, BEST_GUESS, getResources().getString(R.string.maps_api_key));
            call.enqueue(new retrofit2.Callback<MapsDirectionResponse>() {
                @Override
                public void onResponse(retrofit2.Call<MapsDirectionResponse> call, retrofit2.Response<MapsDirectionResponse> response) {
                    HttpUrl s = call.request().url();
                    handleResponse(response.body(), s);
                }

                @Override
                public void onFailure(retrofit2.Call<MapsDirectionResponse> call, Throwable t) {
                    adjustUI(ERROR_FETCHING);
                }
            });
        }
    }

    /*
        Calculate millis for starting of next week i.e 12:00 AM next Sunday in IST
     */
    private long getNextWeekInMillis() {
        long currentSeconds = System.currentTimeMillis();
        long secsAtZeroZero = currentSeconds - (currentSeconds % MILLIS_IN_A_DAY); // Millis for 12:00 AM
        return secsAtZeroZero + ((DAYS_IN_WEEK_PLUS_ONE - DateTimeUtil.getDayOfWeek(currentSeconds)) * MILLIS_IN_A_DAY) - (330 * MILLIS_IN_A_MINUTE); // Manually subtracted for IST
    }

    private void handleResponse(MapsDirectionResponse response, HttpUrl url) {
        MapsDirectionResponseHelper helper = new MapsDirectionResponseHelper(response);
        if (durationsList.size() == dayOfWeek || durationsList.get(dayOfWeek) == null) {
            durationsList.add(dayOfWeek, new ArrayList<MapsDirectionResponseHelper>());
        }
        helper.setDepartureTime(url.queryParameter("departure_time"));
        durationsList.get(dayOfWeek).add(helper);
        if (durationsList.get(dayOfWeek).size() == MULTIPLIER_IN_ODD) {
            if (durationsList.size() == DAYS_IN_WEEK) {
                loadChart();
                findDurationInitiated = false;
                dayOfWeek = 0;
                adjustUI(FETCHED);
            } else {
                dayOfWeek++;
                analyseTimings();
            }
        }
    }

    private void loadChart() {
        departures.clear();

        List<IBarDataSet> dataSets = new ArrayList<>();
        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {

                long v = departures.get((int) (value-1));

                String time = DateTimeUtil.getTimeFromMillis(v * MILLIS_IN_A_MINUTE);

                return time;
            }
        };

        for (int i = 0; i < MULTIPLIER_IN_ODD; i++) {
            long departure = (Long.parseLong(durationsList.get(0).get(i).getDepartureTime()) - getNextWeekInMillis()) / MILLIS_IN_A_MINUTE;
            departures.add(departure);
        }

        Collections.sort(departures);

        for (int i = 0; i < durationsList.size(); i++) {
            List<BarEntry> entries = new ArrayList<>();
            Collections.sort(durationsList.get(i));
            Iterator<MapsDirectionResponseHelper> iterator = durationsList.get(i).iterator();
            int j=0;
            while (iterator.hasNext()) {
                MapsDirectionResponseHelper duration = iterator.next();
                long departure = (Long.parseLong(duration.getDepartureTime()) - getNextWeekInMillis()) / MILLIS_IN_A_MINUTE;
                entries.add(new BarEntry((float) ++j, (duration.getDurationInTrafficInMins())));
            }
            BarDataSet dataSet = new BarDataSet(entries, DateTimeUtil.dayOfWeek(i));
            switch (i) {
                case 0:
                    dataSet.setColor(ColorTemplate.getHoloBlue());
                    break;
                case 1:
                    dataSet.setColor(ColorTemplate.rgb("ff1122"));
                    break;
                case 2:
                    dataSet.setColor(ColorTemplate.rgb("ffff22"));
                    break;
                case 3:
                    dataSet.setColor(ColorTemplate.rgb("001122"));
                    break;
                case 4:
                    dataSet.setColor(ColorTemplate.rgb("ff1122"));
                    break;
                case 5:
                    dataSet.setColor(ColorTemplate.rgb("ff11ff"));
                    break;
                case 6:
                    dataSet.setColor(ColorTemplate.rgb("ff55ff"));
                    break;
            }
            //dataSet.setColor(ColorTemplate.getHoloBlue());
            dataSets.add(dataSet);
        }

        XAxis xAxis = chart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(formatter);

        BarData data = new BarData(dataSets);
        data.setBarWidth(.1f);
        chart.setData(data);
        chart.groupBars(1f, .06f, .02f);
        chart.invalidate();
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

    /* Helper UI function to set the UI according to different modes */
    private void adjustUI(@UIMode int mode) {
        switch (mode) {
            case INITIAL:
                chartLayout.setVisibility(View.GONE);
                break;
            case FETCHING:
                chartLayout.setVisibility(View.VISIBLE);
                chart.setVisibility(View.GONE);
                chartProgressBar.setVisibility(View.VISIBLE);
                refreshSearchButton();
                break;
            case FETCHED:
                chartLayout.setVisibility(View.VISIBLE);
                chart.setVisibility(View.VISIBLE);
                chartProgressBar.setVisibility(View.GONE);
                refreshSearchButton();
                break;
            case ERROR_FETCHING:
                //Do something
                break;
        }
        currentUIMode = mode;
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
