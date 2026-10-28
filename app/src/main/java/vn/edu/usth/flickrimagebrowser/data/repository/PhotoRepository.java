package vn.edu.usth.flickrimagebrowser.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.util.List;

import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Repository abstraction decoupling ViewModels from Retrofit and Room implementation details.
 */
public interface PhotoRepository {

    @NonNull
    LiveData<Resource<List<Photo>>> searchPhotos(@NonNull String query, int page, int perPage);

    @NonNull
    LiveData<List<Photo>> observeFavorites();

    @NonNull
    LiveData<Boolean> observeIsFavorite(@NonNull String photoId);

    void toggleFavorite(@NonNull Photo photo);

    void setFavorite(@NonNull Photo photo, boolean isFavorite);

    void restoreFavorite(@NonNull Photo photo);

    @NonNull
    LiveData<List<Collection>> observeCollections();

    @NonNull
    LiveData<Resource<Long>> createCollection(@NonNull String name, @Nullable String description);

    @NonNull
    LiveData<Resource<Void>> deleteCollection(long collectionId);

    @NonNull
    LiveData<Resource<Void>> updateCollection(long collectionId, @NonNull String newName, @Nullable String newDescription);

    @NonNull
    LiveData<Resource<Void>> addPhotoToCollection(long collectionId, @NonNull Photo photo);

    @NonNull
    LiveData<Resource<Void>> addPhotosToCollection(long collectionId, @NonNull List<Photo> photos);

    @NonNull
    LiveData<Resource<Void>> removePhotoFromCollection(long collectionId, @NonNull String photoId);

    @NonNull
    LiveData<Resource<Void>> removePhotosFromCollection(long collectionId, @NonNull List<String> photoIds);
    @NonNull
    LiveData<List<Photo>> observePhotosInCollection(long collectionId);
}
