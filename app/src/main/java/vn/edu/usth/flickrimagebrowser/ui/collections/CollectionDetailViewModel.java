package vn.edu.usth.flickrimagebrowser.ui.collections;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;

/**
 * ViewModel for the Collection Detail screen.
 * Observes the list of photos belonging to a single collection
 * and provides a method to remove a photo from that collection.
 */
public class CollectionDetailViewModel extends ViewModel {

    private final PhotoRepository repository;
    private final MutableLiveData<Long> collectionIdLiveData = new MutableLiveData<>(-1L);
    private final LiveData<List<Photo>> photos;
    private final LiveData<Collection> collection;

    public CollectionDetailViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.photos = Transformations.switchMap(
                collectionIdLiveData,
                id -> {
                    if (id == null || id <= 0) {
                        MutableLiveData<List<Photo>> empty = new MutableLiveData<>();
                        empty.setValue(Collections.emptyList());
                        return empty;
                    }
                    return this.repository.observePhotosInCollection(id);
                }
        );
        this.collection = Transformations.switchMap(
                collectionIdLiveData,
                targetId -> {
                    LiveData<List<Collection>> allCollections = repository.observeCollections();
                    if (allCollections == null) {
                        return new MutableLiveData<>();
                    }
                    return Transformations.map(
                            allCollections,
                            list -> {
                                if (list == null || targetId == null || targetId <= 0) return null;
                                for (Collection c : list) {
                                    if (c.getId() == targetId) return c;
                                }
                                return null;
                            }
                    );
                }
        );
    }

    /**
     * Sets the collection whose photos should be observed.
     *
     * @param collectionId Room primary key of the target collection.
     */
    public void setCollectionId(long collectionId) {
        if (!Objects.equals(collectionIdLiveData.getValue(), collectionId)) {
            collectionIdLiveData.setValue(collectionId);
        }
    }

    /**
     * Returns a LiveData list of photos inside the current collection.
     * Updates automatically whenever the user adds/removes photos.
     */
    @NonNull
    public LiveData<List<Photo>> getPhotos() {
        return photos;
    }

    @NonNull
    public LiveData<Collection> getCollection() {
        return collection;
    }


    public long getCollectionId() {
        Long id = collectionIdLiveData.getValue();
        return id != null ? id : -1L;
    }

    /**
     * Removes a photo from this collection.
     * If the photo is not a favorite AND not in any other collection,
     * it will also be deleted from the photos table (orphan cleanup).
     *
     * @param photo The photo to remove.
     */
    public void removePhotoFromCollection(@NonNull Photo photo) {
        Long id = collectionIdLiveData.getValue();
        if (id == null || id <= 0) {
            return;
        }
        repository.removePhotoFromCollection(id, photo.getId());
    }

    @NonNull
    public LiveData<vn.edu.usth.flickrimagebrowser.util.Resource<Void>> removePhotosFromCollection(@NonNull List<String> photoIds) {
        Long id = collectionIdLiveData.getValue();
        if (id == null || id <= 0) {
            MutableLiveData<vn.edu.usth.flickrimagebrowser.util.Resource<Void>> err = new MutableLiveData<>();
            err.setValue(vn.edu.usth.flickrimagebrowser.util.Resource.error("Invalid album", null));
            return err;
        }
        return repository.removePhotosFromCollection(id, photoIds);
    }

    @NonNull
    public LiveData<vn.edu.usth.flickrimagebrowser.util.Resource<Void>> addPhotosToCollection(@NonNull List<Photo> photosToAdd) {
        Long id = collectionIdLiveData.getValue();
        if (id == null || id <= 0) {
            MutableLiveData<vn.edu.usth.flickrimagebrowser.util.Resource<Void>> err = new MutableLiveData<>();
            err.setValue(vn.edu.usth.flickrimagebrowser.util.Resource.error("Invalid album", null));
            return err;
        }
        return repository.addPhotosToCollection(id, photosToAdd);
    }

    public void toggleFavorite(@NonNull Photo photo) {
        repository.toggleFavorite(photo);
    }
}
