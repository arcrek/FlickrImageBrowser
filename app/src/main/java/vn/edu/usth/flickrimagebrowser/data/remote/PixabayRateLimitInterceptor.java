package vn.edu.usth.flickrimagebrowser.data.remote;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import okhttp3.Interceptor;
import okhttp3.Response;

/** Shares one rolling request budget across Featured, search, and detail requests. */
final class PixabayRateLimitInterceptor implements Interceptor {
    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_NANOS = TimeUnit.SECONDS.toNanos(60);

    private final LongSupplier clock;
    private final Deque<Long> requestTimes = new ArrayDeque<>();
    private long cooldownStarted;
    private long cooldownNanos;

    PixabayRateLimitInterceptor() {
        this(System::nanoTime);
    }

    PixabayRateLimitInterceptor(LongSupplier clock) {
        this.clock = clock;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        acquire();
        Response response = chain.proceed(chain.request());
        String remaining = response.header("X-RateLimit-Remaining");
        if (response.code() == 429 || (remaining != null && "0".equals(remaining.trim()))) {
            startCooldown(response);
        }
        return response;
    }

    private synchronized void acquire() throws IOException {
        long now = clock.getAsLong();
        while (!requestTimes.isEmpty() && now - requestTimes.peekFirst() >= WINDOW_NANOS) {
            requestTimes.removeFirst();
        }
        long waitNanos = cooldownNanos > 0 ? cooldownNanos - (now - cooldownStarted) : 0;
        if (requestTimes.size() >= MAX_REQUESTS) {
            waitNanos = Math.max(waitNanos, WINDOW_NANOS - (now - requestTimes.peekFirst()));
        }
        if (waitNanos > 0) {
            long seconds = TimeUnit.NANOSECONDS.toSeconds(waitNanos);
            if (waitNanos % TimeUnit.SECONDS.toNanos(1) != 0) {
                seconds++;
            }
            throw new IOException("Pixabay API request limit reached. Please wait "
                    + seconds + " seconds before retrying.");
        }
        requestTimes.addLast(now);
    }

    private synchronized void startCooldown(Response response) {
        long now = clock.getAsLong();
        long remaining = cooldownNanos > 0 ? cooldownNanos - (now - cooldownStarted) : 0;
        long resetNanos = headerDelayNanos(response.header("X-RateLimit-Reset"));
        if (response.code() == 429 || resetNanos == 0) {
            resetNanos = Math.max(WINDOW_NANOS, resetNanos);
        }
        cooldownStarted = now;
        cooldownNanos = Math.max(remaining, Math.max(
                headerDelayNanos(response.header("Retry-After")),
                resetNanos
        ));
    }

    private static long headerDelayNanos(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return TimeUnit.SECONDS.toNanos(Math.max(0, Long.parseLong(value.trim())));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
