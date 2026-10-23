package vn.edu.usth.flickrimagebrowser.ui.detail;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.Objects;
import java.util.List;
import androidx.annotation.Nullable;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;

/**
 * ViewModel for the photo detail screen, observing live favorite status and toggling persistence.
 */
public class DetailViewModel extends ViewModel {

    private final PhotoRepository repository;
    private final LiveData<List<Collection>> collections;
    private final MutableLiveData<Photo> currentPhoto = new MutableLiveData<>();
    private final LiveData<Boolean> isFavorite;

    public DetailViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.collections = repository.observeCollections();
        this.isFavorite = Transformations.switchMap(
                currentPhoto,
                photo -> repository.observeIsFavorite(photo.getId())
        );
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

    public void toggleFavorite() {
        Photo photo = currentPhoto.getValue();
        if (photo != null) {
            repository.toggleFavorite(photo);
        }
    }

    @NonNull
    public LiveData<List<Collection>> getCollections() {
        return collections;
    }

    public void addPhotoToCollection(long collectionId) {
        Photo photo = currentPhoto.getValue();
        if (photo != null && collectionId > 0) {
            repository.addPhotoToCollection(collectionId, photo);
        }
    }

    @Nullable
    public Photo getCurrentPhoto() {
        return currentPhoto.getValue();
    }
}
