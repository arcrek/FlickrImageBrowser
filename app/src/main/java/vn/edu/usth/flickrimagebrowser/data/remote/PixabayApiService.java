package vn.edu.usth.flickrimagebrowser.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PixabaySearchResponse;

/**
 * Retrofit interface for the Pixabay REST API (https://pixabay.com/api/).
 */
public interface PixabayApiService {

    @GET(".")
    Call<PixabaySearchResponse> searchPhotos(
            @Query("key") String apiKey,
            @Query("q") String query,
            @Query("image_type") String imageType,
            @Query("safesearch") boolean safeSearch,
            @Query("page") int page,
            @Query("per_page") int perPage
    );

    @GET(".")
    Call<PixabaySearchResponse> getPhotoById(
            @Query("key") String apiKey,
            @Query("id") String photoId
    );
}
