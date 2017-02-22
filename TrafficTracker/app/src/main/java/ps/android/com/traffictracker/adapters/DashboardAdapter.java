package ps.android.com.traffictracker.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import ps.android.com.traffictracker.fragments.SelectLocationsFragment;
import ps.android.com.traffictracker.fragments.TimeTrackerFragment;

/**
 * Created by satyanarayana.p on 22/02/17.
 */

public class DashboardAdapter extends FragmentPagerAdapter {
    public DashboardAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return SelectLocationsFragment.newInstance();
            case 1:
                return TimeTrackerFragment.newInstance();

        }
        return null;
    }

    @Override
    public int getCount() {
        return 2;
    }
}
