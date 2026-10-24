package vn.edu.usth.flickrimagebrowser.data.local;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

final class LiveDataTestUtil {

    private LiveDataTestUtil() {
    }

    static <T> T awaitValue(LiveData<T> source) throws InterruptedException {
        return awaitValue(source, value -> true);
    }

    static <T> T awaitValue(LiveData<T> source, Predicate<T> condition)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        Observer<T> observer = value -> {
            if (condition.test(value)) {
                result.set(value);
                latch.countDown();
            }
        };
        source.observeForever(observer);
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for Room LiveData");
            }
            return result.get();
        } finally {
            source.removeObserver(observer);
        }
    }
}
