package vn.edu.usth.flickrimagebrowser.util;

import android.content.Context;

import androidx.annotation.NonNull;

import vn.edu.usth.flickrimagebrowser.BuildConfig;
import vn.edu.usth.flickrimagebrowser.data.local.AppDatabase;
import vn.edu.usth.flickrimagebrowser.data.remote.FlickrApiService;
import vn.edu.usth.flickrimagebrowser.data.remote.FlickrClient;
import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepositoryImpl;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;

/**
 * Lightweight application-scoped dependency container avoiding heavy DI frameworks.
 */
public class AppContainer {

    private final AppExecutors executors;
    private final AppDatabase database;
    private final FlickrApiService apiService;
    private final PhotoRepository photoRepository;
    private final AppViewModelFactory viewModelFactory;

    public AppContainer(@NonNull Context applicationContext) {
        this.executors = AppExecutors.createDefault();
        this.database = AppDatabase.getInstance(applicationContext);
        this.apiService = FlickrClient.createService();
        this.photoRepository = new PhotoRepositoryImpl(
                apiService,
                database.photoDao(),
                database.collectionDao(),
                executors,
                BuildConfig.FLICKR_API_KEY
        );
        this.viewModelFactory = new AppViewModelFactory(photoRepository);
    }

    @NonNull
    public PhotoRepository getPhotoRepository() {
        return photoRepository;
    }

    @NonNull
    public AppViewModelFactory getViewModelFactory() {
        return viewModelFactory;
    }
}
