package vn.edu.usth.flickrimagebrowser.data.remote;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import vn.edu.usth.flickrimagebrowser.BuildConfig;

/**
 * Factory for configuring OkHttp and Retrofit instances for the Flickr REST API.
 */
public final class FlickrClient {

    private static final long CONNECT_TIMEOUT_SECONDS = 15L;
    private static final long READ_TIMEOUT_SECONDS = 20L;
    private static final long WRITE_TIMEOUT_SECONDS = 15L;

    private FlickrClient() {
    }

    @NonNull
    public static FlickrApiService createService() {
        return createService(BuildConfig.FLICKR_BASE_URL);
    }

    @NonNull
    public static FlickrApiService createService(@NonNull String baseUrl) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        // Flickr credentials are query parameters; logging request lines would expose api_key in Logcat.
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(FlickrApiService.class);
    }
}
