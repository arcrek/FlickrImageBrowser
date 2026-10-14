package vn.edu.usth.flickrimagebrowser;

import android.app.Application;

import androidx.annotation.NonNull;

import vn.edu.usth.flickrimagebrowser.util.AppContainer;

/**
 * Custom Application class initializing the shared AppContainer.
 */
public class FlickrApplication extends Application {

    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
    }

    @NonNull
    public AppContainer getAppContainer() {
        return appContainer;
    }
}
