package vn.edu.usth.flickrimagebrowser.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PexelsSearchResponse;

/**
 * Retrofit interface for the Pexels REST API (https://api.pexels.com/v1/).
 */
public interface PexelsApiService {

    @GET("search")
    Call<PexelsSearchResponse> searchPhotos(
            @Header("Authorization") String apiKey,
            @Query("query") String query,
            @Query("page") int page,
            @Query("per_page") int perPage
    );
}
