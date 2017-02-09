package ps.android.com.traffictracker.helpers;

import ps.android.com.traffictracker.network.response.MapsDirectionResponse;

/**
 * Created by satyanarayana.p on 09/02/17.
 */

public class MapsDirectionResponseHelper {


    private MapsDirectionResponse.Route.Leg.Distance distance;
    private MapsDirectionResponse.Route.Leg.Duration durationInTraffic;
    private MapsDirectionResponse.Route.Leg.Duration duration;

    public MapsDirectionResponseHelper(MapsDirectionResponse response) {
        understandResponse(response);
    }

    private void understandResponse(MapsDirectionResponse response) {
        if (response == null || response.getRoutes() == null || response.getRoutes().size() == 0 || response.getRoutes().get(0).getLegs() == null || response.getRoutes().get(0).getLegs().size() == 0) {
            return;
        }

        distance = response.getRoutes().get(0).getLegs().get(0).getDistance();
        duration = response.getRoutes().get(0).getLegs().get(0).getDuration();
        durationInTraffic = response.getRoutes().get(0).getLegs().get(0).getDurationInTraffic();
    }

    public String getDistanceInKms() {
        if (distance != null) {
            return distance.getDistance();
        }

        return null;
    }

    public long getDistanceValue() {
        if(distance != null) {
            return distance.getDistanceValue();
        }

        return -1;
    }

    public String getActualDuration() {
        if(duration != null) {
            return duration.getDuration();
        }
        return null;
    }

    public long getActualDurationInSecs() {
        if(duration != null) {
            return duration.getDurationInSecs();
        }
        return -1;
    }

    public String getDurationInTraffic() {
        if(durationInTraffic != null) {
            return durationInTraffic.getDuration();
        }
        return null;
    }

    public long getDurationInTrafficInSecs() {
        if(durationInTraffic != null) {
            return durationInTraffic.getDurationInSecs();
        }
        return -1;
    }


}
