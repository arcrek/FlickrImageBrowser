package vn.edu.usth.flickrimagebrowser.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PhotosSearchResponse;

/**
 * Retrofit interface for the Flickr REST API.
 */
public interface FlickrApiService {

    String METHOD_PHOTOS_SEARCH = "flickr.photos.search";
    String DEFAULT_EXTRAS = "url_q,url_m,url_z,owner_name,description";
    String FORMAT_JSON = "json";
    int NO_JSON_CALLBACK = 1;
    int SAFE_SEARCH_SAFE = 1;

    @GET("services/rest/")
    Call<PhotosSearchResponse> searchPhotos(
            @Query("method") String method,
            @Query("api_key") String apiKey,
            @Query("text") String queryText,
            @Query("page") int page,
            @Query("per_page") int perPage,
            @Query("extras") String extras,
            @Query("safe_search") int safeSearch,
            @Query("format") String format,
            @Query("nojsoncallback") int noJsonCallback
    );
}
