package vn.edu.usth.flickrimagebrowser.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

public class PixabayRateLimitInterceptorTest {
    private final AtomicLong now = new AtomicLong();
    private PixabayRateLimitInterceptor limiter;
    private Interceptor.Chain chain;
    private Request request;

    @Before
    public void setUp() throws IOException {
        limiter = new PixabayRateLimitInterceptor(now::get);
        chain = mock(Interceptor.Chain.class);
        request = new Request.Builder().url("https://pixabay.com/api/?editors_choice=true").build();
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any())).thenReturn(response(200).build());
    }

    @Test
    public void budgetBlocksAttempt101AndRecoversAtRollingBoundary() throws IOException {
        for (int i = 0; i < 100; i++) {
            limiter.intercept(chain);
        }
        now.set(TimeUnit.SECONDS.toNanos(60) - 1);
        IOException error = assertThrows(IOException.class, () -> limiter.intercept(chain));
        assertTrue(error.getMessage().contains("wait 1 seconds"));
        verify(chain, times(100)).proceed(any());
        now.incrementAndGet();
        assertEquals(200, limiter.intercept(chain).code());
        verify(chain, times(101)).proceed(any());
    }

    @Test
    public void rollingWindowDoesNotResetAllCapacityAtMinuteBoundary() throws IOException {
        for (int i = 0; i < 50; i++) {
            limiter.intercept(chain);
        }
        now.set(TimeUnit.SECONDS.toNanos(30));
        for (int i = 0; i < 50; i++) {
            limiter.intercept(chain);
        }
        now.set(TimeUnit.SECONDS.toNanos(60));
        for (int i = 0; i < 50; i++) {
            limiter.intercept(chain);
        }
        assertThrows(IOException.class, () -> limiter.intercept(chain));
        verify(chain, times(150)).proceed(any());
    }

    @Test
    public void concurrentRequestsShareOneBudget() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> attempts = new ArrayList<>();
            for (int i = 0; i < 150; i++) {
                attempts.add(() -> {
                    try {
                        limiter.intercept(chain);
                        return true;
                    } catch (IOException expected) {
                        return false;
                    }
                });
            }
            int allowed = 0;
            for (Future<Boolean> result : workers.invokeAll(attempts)) {
                if (result.get()) {
                    allowed++;
                }
            }
            assertEquals(100, allowed);
            verify(chain, times(100)).proceed(any());
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    public void server429StopsRequestsForAtLeastOneMinuteWithInvalidHeaders() throws IOException {
        when(chain.proceed(any())).thenReturn(response(429)
                .header("Retry-After", "invalid").header("X-RateLimit-Reset", "-1").build());
        assertEquals(429, limiter.intercept(chain).code());
        now.set(TimeUnit.SECONDS.toNanos(59));
        assertThrows(IOException.class, () -> limiter.intercept(chain));
        verify(chain, times(1)).proceed(any());
        when(chain.proceed(any())).thenReturn(response(200).build());
        now.set(TimeUnit.SECONDS.toNanos(60));
        assertEquals(200, limiter.intercept(chain).code());
    }

    @Test
    public void serverCooldownHonorsLongerRetryAndResetHeaders() throws IOException {
        when(chain.proceed(any())).thenReturn(response(429)
                .header("Retry-After", "90").header("X-RateLimit-Reset", "120").build());
        limiter.intercept(chain);
        now.set(TimeUnit.SECONDS.toNanos(119));
        assertThrows(IOException.class, () -> limiter.intercept(chain));
        verify(chain, times(1)).proceed(any());
        when(chain.proceed(any())).thenReturn(response(200).build());
        now.set(TimeUnit.SECONDS.toNanos(120));
        assertEquals(200, limiter.intercept(chain).code());
    }

    @Test
    public void failedNetworkAttemptsStillConsumeBudget() throws IOException {
        when(chain.proceed(any())).thenThrow(new IOException("Disconnected"));
        for (int i = 0; i < 100; i++) {
            assertThrows(IOException.class, () -> limiter.intercept(chain));
        }
        IOException error = assertThrows(IOException.class, () -> limiter.intercept(chain));
        assertTrue(error.getMessage().contains("request limit"));
        verify(chain, times(100)).proceed(any());
    }

    @Test
    public void monotonicClockMayStartNegative() throws IOException {
        now.set(-TimeUnit.SECONDS.toNanos(100));
        assertEquals(200, limiter.intercept(chain).code());
    }

    private Response.Builder response(int code) {
        return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(code).message("Test response");
    }
}
