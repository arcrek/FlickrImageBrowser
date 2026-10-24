package vn.edu.usth.flickrimagebrowser.ui.detail;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Objects;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * ViewModel for the photo detail screen, observing live favorite status,
 * toggling persistence, and assigning photos to collections.
 */
public class DetailViewModel extends ViewModel {

    private final PhotoRepository repository;
    private final MutableLiveData<Photo> currentPhoto = new MutableLiveData<>();
    private final LiveData<Boolean> isFavorite;
    private final LiveData<List<Collection>> collections;

    public DetailViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.isFavorite = Transformations.switchMap(
                currentPhoto,
                photo -> repository.observeIsFavorite(photo.getId())
        );
        this.collections = repository.observeCollections();
    }

    public void setPhoto(@NonNull Photo photo) {
        if (currentPhoto.getValue() == null || !currentPhoto.getValue().getId().equals(photo.getId())) {
            currentPhoto.setValue(photo);
        }
    }

    @NonNull
    public LiveData<Photo> getPhoto() {
        return currentPhoto;
    }

    @NonNull
    public LiveData<Boolean> getIsFavorite() {
        return isFavorite;
    }

    @NonNull
    public LiveData<List<Collection>> getCollections() {
        return collections;
    }

    public void toggleFavorite() {
        Photo photo = currentPhoto.getValue();
        if (photo != null) {
            repository.toggleFavorite(photo);
        }
    }

    public void addPhotoToCollection(long collectionId) {
        Photo photo = currentPhoto.getValue();
        if (photo != null) {
            repository.addPhotoToCollection(collectionId, photo);
        }
    }

    public void addToCollection(long collectionId) {
        addPhotoToCollection(collectionId);
    }
}
