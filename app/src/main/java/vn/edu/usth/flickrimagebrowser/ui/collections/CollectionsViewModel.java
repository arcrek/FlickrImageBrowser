package vn.edu.usth.flickrimagebrowser.ui.collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Objects;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;

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

    public void createCollection(@NonNull String name, @Nullable String description) {
        repository.createCollection(name, description);
    }
}
