package vn.edu.usth.flickrimagebrowser.data.remote;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import vn.edu.usth.flickrimagebrowser.BuildConfig;

/**
 * Factory for configuring OkHttp and Retrofit instances for the Pixabay REST API.
 */
public final class PixabayClient {

    private static final long CONNECT_TIMEOUT_SECONDS = 15L;
    private static final long READ_TIMEOUT_SECONDS = 20L;
    private static final long WRITE_TIMEOUT_SECONDS = 15L;
    private static final PixabayRateLimitInterceptor RATE_LIMITER = new PixabayRateLimitInterceptor();

    private PixabayClient() {
    }

    @NonNull
    public static PixabayApiService createService() {
        return createService(BuildConfig.PIXABAY_BASE_URL);
    }

    @NonNull
    public static PixabayApiService createService(@NonNull String baseUrl) {
        HttpLoggingInterceptor.Logger sanitizedLogger = message -> {
            String safeMessage = message.replaceAll("([?&]key=)[^&\\s]+", "$1REDACTED");
            HttpLoggingInterceptor.Logger.DEFAULT.log(safeMessage);
        };
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(sanitizedLogger);
        loggingInterceptor.setLevel(
                BuildConfig.DEBUG
                        ? HttpLoggingInterceptor.Level.BASIC
                        : HttpLoggingInterceptor.Level.NONE
        );

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addNetworkInterceptor(RATE_LIMITER)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(PixabayApiService.class);
    }
}
