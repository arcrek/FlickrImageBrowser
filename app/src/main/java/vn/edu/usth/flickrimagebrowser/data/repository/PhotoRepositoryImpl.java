package vn.edu.usth.flickrimagebrowser.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.usth.flickrimagebrowser.data.local.CollectionDao;
import vn.edu.usth.flickrimagebrowser.data.local.PhotoDao;
import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionEntity;
import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionPhotoCrossRef;
import vn.edu.usth.flickrimagebrowser.data.local.entity.PhotoEntity;
import vn.edu.usth.flickrimagebrowser.data.remote.PexelsApiService;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PexelsPhotoDto;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PexelsSearchResponse;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PhotoMapper;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.AppExecutors;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Default repository implementation coordinating Pexels REST API searches and Room persistence.
 */
public class PhotoRepositoryImpl implements PhotoRepository {

    public static final String PLACEHOLDER_API_KEY = "your_pexels_api_key_here";

    private final PexelsApiService apiService;
    private final PhotoDao photoDao;
    private final CollectionDao collectionDao;
    private final AppExecutors executors;
    private final String apiKey;

    public PhotoRepositoryImpl(@NonNull PexelsApiService apiService,
                               @NonNull PhotoDao photoDao,
                               @NonNull CollectionDao collectionDao,
                               @NonNull AppExecutors executors,
                               @Nullable String apiKey) {
        this.apiService = Objects.requireNonNull(apiService, "apiService must not be null");
        this.photoDao = Objects.requireNonNull(photoDao, "photoDao must not be null");
        this.collectionDao = Objects.requireNonNull(collectionDao, "collectionDao must not be null");
        this.executors = Objects.requireNonNull(executors, "executors must not be null");
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    @NonNull
    @Override
    public LiveData<Resource<List<Photo>>> searchPhotos(@NonNull String query, int page, int perPage) {
        MutableLiveData<Resource<List<Photo>>> result = new MutableLiveData<>();
        String trimmedQuery = query.trim();
        if (trimmedQuery.isEmpty()) {
            result.setValue(Resource.empty());
            return result;
        }

        if (apiKey.isEmpty() || PLACEHOLDER_API_KEY.equalsIgnoreCase(apiKey)) {
            result.setValue(Resource.error(
                    "Pexels API key is not configured. Add PEXELS_API_KEY to local.properties.",
                    null
            ));
            return result;
        }

        result.setValue(Resource.loading(null));

        Call<PexelsSearchResponse> call = apiService.searchPhotos(
                apiKey,
                trimmedQuery,
                Math.max(1, page),
                Math.max(1, perPage)
        );

        call.enqueue(new Callback<PexelsSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<PexelsSearchResponse> call,
                                   @NonNull Response<PexelsSearchResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    int code = response.code();
                    String errorMsg;
                    if (code == 401 || code == 403) {
                        errorMsg = "Invalid or unauthorized Pexels API key (HTTP " + code + ").";
                    } else if (code == 429) {
                        errorMsg = "Pexels API rate limit exceeded (HTTP 429). Please wait before retrying.";
                    } else {
                        errorMsg = "Server returned HTTP " + code;
                    }
                    result.postValue(Resource.error(errorMsg, null));
                    return;
                }

                PexelsSearchResponse body = response.body();
                if (body.getError() != null && !body.getError().trim().isEmpty()) {
                    result.postValue(Resource.error(body.getError().trim(), null));
                    return;
                }

                List<PexelsPhotoDto> dtos = body.getPhotos();

                executors.diskIO().execute(() -> {
                    Set<String> favoriteIds = new HashSet<>(photoDao.getFavoriteIdsSync());
                    List<Photo> mapped = PhotoMapper.fromDtoList(dtos, favoriteIds);
                    if (mapped.isEmpty()) {
                        result.postValue(Resource.empty());
                    } else {
                        result.postValue(Resource.success(mapped));
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<PexelsSearchResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                String message = t.getMessage() != null && !t.getMessage().trim().isEmpty()
                        ? "Network error: " + t.getMessage().trim()
                        : "Network error: Unable to reach Pexels.";
                result.postValue(Resource.error(message, null));
            }
        });

        return result;
    }

    @NonNull
    @Override
    public LiveData<List<Photo>> observeFavorites() {
        return Transformations.map(photoDao.observeFavoritePhotos(), PhotoMapper::fromEntityList);
    }

    @NonNull
    @Override
    public LiveData<Boolean> observeIsFavorite(@NonNull String photoId) {
        return photoDao.observeIsFavorite(photoId);
    }

    @Override
    public void toggleFavorite(@NonNull Photo photo) {
        executors.diskIO().execute(() -> {
            PhotoEntity existing = photoDao.getPhotoByIdSync(photo.getId());
            boolean nextFavoriteState = existing == null || !existing.isFavorite();
            long now = System.currentTimeMillis();
            PhotoEntity updated = PhotoMapper.toEntity(photo, nextFavoriteState, now);
            photoDao.upsertPhoto(updated);
            if (!nextFavoriteState) {
                photoDao.deleteOrphanIfUnreferenced(photo.getId());
            }
        });
    }

    @Override
    public void setFavorite(@NonNull Photo photo, boolean isFavorite) {
        executors.diskIO().execute(() -> {
            long now = System.currentTimeMillis();
            PhotoEntity updated = PhotoMapper.toEntity(photo, isFavorite, now);
            photoDao.upsertPhoto(updated);
            if (!isFavorite) {
                photoDao.deleteOrphanIfUnreferenced(photo.getId());
            }
        });
    }

    @NonNull
    @Override
    public LiveData<List<Collection>> observeCollections() {
        return Transformations.map(
                collectionDao.observeCollectionsWithPhotos(),
                PhotoMapper::fromCollectionRelations
        );
    }

    @Override
    public void createCollection(@NonNull String name, @Nullable String description) {
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            return;
        }
        String cleanDesc = description != null ? description.trim() : "";
        executors.diskIO().execute(() -> {
            CollectionEntity entity = new CollectionEntity(
                    0L,
                    trimmedName,
                    cleanDesc,
                    System.currentTimeMillis()
            );
            collectionDao.insertCollection(entity);
        });
    }

    @Override
    public void addPhotoToCollection(long collectionId, @NonNull Photo photo) {
        if (collectionId <= 0L) {
            return;
        }
        executors.diskIO().execute(() -> {
            PhotoEntity existing = photoDao.getPhotoByIdSync(photo.getId());
            boolean keepFavorite = existing != null && existing.isFavorite();
            long savedAt = existing != null ? existing.getSavedAt() : System.currentTimeMillis();
            PhotoEntity entity = PhotoMapper.toEntity(photo, keepFavorite, savedAt);
            photoDao.upsertPhoto(entity);
            collectionDao.addPhotoToCollection(
                    new CollectionPhotoCrossRef(collectionId, photo.getId(), System.currentTimeMillis())
            );
        });
    }
}
