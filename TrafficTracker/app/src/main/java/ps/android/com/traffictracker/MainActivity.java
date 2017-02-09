package ps.android.com.traffictracker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import ps.android.com.traffictracker.fragments.SelectLocationsFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportFragmentManager().findFragmentByTag(SelectLocationsFragment.TAG) == null) {
            attachFragment(SelectLocationsFragment.newInstance(), SelectLocationsFragment.TAG);
        }
    }

    private void attachFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction().replace(R.id.select_locations_fragment_container, fragment, tag).commitAllowingStateLoss();
    }
}
