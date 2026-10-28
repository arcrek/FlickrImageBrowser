package vn.edu.usth.flickrimagebrowser.ui.collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.Objects;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * ViewModel retaining in-flight album mutation state across device rotations,
 * ensuring dialog configuration changes never fire duplicate create or update requests.
 */
public class CollectionFormViewModel extends ViewModel {

    private final PhotoRepository repository;
    private LiveData<Resource<Long>> createResultLiveData;
    private LiveData<Resource<Void>> updateResultLiveData;
    private LiveData<Resource<Void>> attachPhotoResultLiveData;
    private boolean isSubmitting = false;
    private long createdCollectionId = -1L;

    public CollectionFormViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public boolean isSubmitting() {
        return isSubmitting;
    }

    public void setSubmitting(boolean submitting) {
        this.isSubmitting = submitting;
    }

    @Nullable
    public LiveData<Resource<Long>> getCreateResultLiveData() {
        return createResultLiveData;
    }

    @Nullable
    public LiveData<Resource<Void>> getUpdateResultLiveData() {
        return updateResultLiveData;
    }

    @Nullable
    public LiveData<Resource<Void>> getAttachPhotoResultLiveData() {
        return attachPhotoResultLiveData;
    }

    public long getCreatedCollectionId() {
        return createdCollectionId;
    }

    public void setCreatedCollectionId(long id) {
        this.createdCollectionId = id;
    }

    @NonNull
    public LiveData<Resource<Long>> submitCreate(@NonNull String name, @Nullable String description) {
        if (createResultLiveData != null && isSubmitting) {
            return createResultLiveData;
        }
        isSubmitting = true;
        createResultLiveData = repository.createCollection(name, description);
        return createResultLiveData;
    }

    @NonNull
    public LiveData<Resource<Void>> submitUpdate(long id, @NonNull String name, @Nullable String description) {
        if (updateResultLiveData != null && isSubmitting) {
            return updateResultLiveData;
        }
        isSubmitting = true;
        updateResultLiveData = repository.updateCollection(id, name, description);
        return updateResultLiveData;
    }

    @NonNull
    public LiveData<Resource<Void>> attachPhoto(long collectionId, @NonNull Photo photo) {
        if (attachPhotoResultLiveData != null && isSubmitting) {
            return attachPhotoResultLiveData;
        }
        attachPhotoResultLiveData = repository.addPhotoToCollection(collectionId, photo);
        return attachPhotoResultLiveData;
    }

    public void reset() {
        isSubmitting = false;
        createdCollectionId = -1L;
        createResultLiveData = null;
        updateResultLiveData = null;
        attachPhotoResultLiveData = null;
    }
}
