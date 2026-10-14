package vn.edu.usth.flickrimagebrowser.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Global executor pools for running database and network tasks off the main UI thread.
 */
public class AppExecutors {

    private final Executor diskIO;
    private final Executor networkIO;
    private final Executor mainThread;

    public AppExecutors(@NonNull Executor diskIO,
                        @NonNull Executor networkIO,
                        @NonNull Executor mainThread) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
    }

    public static AppExecutors createDefault() {
        return new AppExecutors(
                Executors.newSingleThreadExecutor(),
                Executors.newFixedThreadPool(3),
                new MainThreadExecutor()
        );
    }

    @NonNull
    public Executor diskIO() {
        return diskIO;
    }

    @NonNull
    public Executor networkIO() {
        return networkIO;
    }

    @NonNull
    public Executor mainThread() {
        return mainThread;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
