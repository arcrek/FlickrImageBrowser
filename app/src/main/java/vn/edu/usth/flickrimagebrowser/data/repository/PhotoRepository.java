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

    @NonNull
    LiveData<List<Collection>> observeCollections();

    void createCollection(@NonNull String name, @Nullable String description);

    void deleteCollection(long collectionId);

    void renameCollection(long collectionId, @NonNull String newName);

    void addPhotoToCollection(long collectionId, @NonNull Photo photo);

    void removePhotoFromCollection(long collectionId, @NonNull String photoId);

    @NonNull
    LiveData<List<Photo>> observePhotosInCollection(long collectionId);
}
