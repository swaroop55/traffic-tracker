package ps.android.com.traffictracker.network.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by satyanarayana.p on 09/02/17.
 */

public class MapsDirectionResponse {

    @SerializedName("routes")
    private List<Route> routes;

    public List<Route> getRoutes() {
        return routes;
    }

    public class Route {

        @SerializedName("legs")
        private List<Leg> legs;

        public List<Leg> getLegs() {
            return legs;
        }

        public class Leg {

            @SerializedName("distance")
            private Distance distance;

            @SerializedName("duration")
            private Duration duration;

            @SerializedName("duration_in_traffic")
            private Duration durationInTraffic;

            public Distance getDistance() {
                return distance;
            }

            public Duration getDuration() {
                return duration;
            }

            public Duration getDurationInTraffic() {
                return durationInTraffic;
            }

            public class Distance {
                public String getDistance() {
                    return distance;
                }

                public long getDistanceValue() {
                    return distanceValue;
                }

                @SerializedName("text")
                String distance;

                @SerializedName("value")
                long distanceValue;
            }

            public class Duration {
                public String getDuration() {
                    return duration;
                }

                public long getDurationInSecs() {
                    return durationInSecs;
                }

                @SerializedName("text")
                String duration;

                @SerializedName("value")
                long durationInSecs;
            }

        }

    }
}
