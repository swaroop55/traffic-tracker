package ps.android.com.traffictracker.network;

import ps.android.com.traffictracker.network.response.MapsDirectionResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by satyanarayana.p on 09/02/17.
 */

public interface TrafficTrackerAPI {

    @GET("directions/json?")
    Call<MapsDirectionResponse> getDuration(@Query("origin") String origin, @Query("destination") String destination, @Query("departure_time") long departureTime, @Query("traffic_model") String trafficModel, @Query("key") String key );
}
