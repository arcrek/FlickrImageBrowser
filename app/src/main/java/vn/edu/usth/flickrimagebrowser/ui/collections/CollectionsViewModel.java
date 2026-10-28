package vn.edu.usth.flickrimagebrowser.ui.collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Objects;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * ViewModel managing user photo collections stored in Room.
 */
public class CollectionsViewModel extends ViewModel {

    private final PhotoRepository repository;
    private final LiveData<List<Collection>> collections;

    public CollectionsViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.collections = repository.observeCollections();
    }

    @NonNull
    public LiveData<List<Collection>> getCollections() {
        return collections;
    }

    @NonNull
    public LiveData<Resource<Long>> createCollection(@NonNull String name, @Nullable String description) {
        String trimmed = Objects.requireNonNull(name, "name must not be null").trim();
        if (trimmed.isEmpty() || !isCollectionNameUnique(trimmed, -1L)) {
            androidx.lifecycle.MutableLiveData<Resource<Long>> err = new androidx.lifecycle.MutableLiveData<>();
            err.setValue(Resource.error("Invalid or duplicate album name", null));
            return err;
        }
        return repository.createCollection(trimmed, description);
    }

    @NonNull
    public LiveData<Resource<Void>> deleteCollection(long collectionId) {
        return repository.deleteCollection(collectionId);
    }

    @NonNull
    public LiveData<Resource<Void>> deleteCollection(@NonNull Collection collection) {
        Objects.requireNonNull(collection, "collection must not be null");
        return repository.deleteCollection(collection.getId());
    }

    @NonNull
    public LiveData<Resource<Void>> updateCollection(long collectionId, @NonNull String newName, @Nullable String newDescription) {
        String trimmed = Objects.requireNonNull(newName, "newName must not be null").trim();
        if (trimmed.isEmpty() || !isCollectionNameUnique(trimmed, collectionId)) {
            androidx.lifecycle.MutableLiveData<Resource<Void>> err = new androidx.lifecycle.MutableLiveData<>();
            err.setValue(Resource.error("Invalid or duplicate album name", null));
            return err;
        }
        return repository.updateCollection(collectionId, trimmed, newDescription);
    }

    /**
     * Checks if a collection name is unique among existing collections (case-insensitive).
     *
     * @param name Name to validate.
     * @param excludeCollectionId Collection ID to exclude (e.g. current collection ID when renaming), or -1 if new.
     * @return true if unique, false if already taken.
     */
    public boolean isCollectionNameUnique(@NonNull String name, long excludeCollectionId) {
        String trimmed = Objects.requireNonNull(name, "name must not be null").trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        List<Collection> current = collections.getValue();
        if (current == null) {
            // Repository performs the authoritative check on diskIO before writing.
            return true;
        }
        if (current.isEmpty()) {
            return true;
        }
        for (Collection c : current) {
            if (c.getId() != excludeCollectionId && c.getName().equalsIgnoreCase(trimmed)) {
                return false;
            }
        }
        return true;
    }
}
