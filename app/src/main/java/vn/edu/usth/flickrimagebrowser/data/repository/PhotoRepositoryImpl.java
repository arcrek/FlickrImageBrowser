package vn.edu.usth.flickrimagebrowser.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
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
import vn.edu.usth.flickrimagebrowser.data.remote.PixabayApiService;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PhotoMapper;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PixabayHitDto;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PixabaySearchResponse;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.AppExecutors;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Default repository implementation coordinating Pixabay REST API searches, Room persistence,
 * and a mock demo fallback mode for testing without requiring an external API key.
 */
public class PhotoRepositoryImpl implements PhotoRepository {

    public static final String PLACEHOLDER_API_KEY = "your_pixabay_api_key_here";
    public static final String DEMO_API_KEY = "demo";
    public static final String DEFAULT_IMAGE_TYPE = "photo";
    public static final String DEFAULT_FEATURED_ORDER = "popular";
    public static final boolean DEFAULT_SAFE_SEARCH = true;
    public static final int MIN_PER_PAGE = 3;
    public static final int MAX_PER_PAGE = 200;

    private final PixabayApiService apiService;
    private final PhotoDao photoDao;
    private final CollectionDao collectionDao;
    private final AppExecutors executors;
    private final String apiKey;

    public PhotoRepositoryImpl(@NonNull PixabayApiService apiService,
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

        if (apiKey.isEmpty()) {
            result.setValue(Resource.error(
                    "Pixabay API key is not configured. Add PIXABAY_API_KEY to local.properties.",
                    null
            ));
            return result;
        }

        // Demo / Mock Mode when using demo or placeholder key (allows testing without registering an API key)
        if (DEMO_API_KEY.equalsIgnoreCase(apiKey) || PLACEHOLDER_API_KEY.equalsIgnoreCase(apiKey)) {
            loadDemoPhotos(trimmedQuery, page, perPage, result);
            return result;
        }

        result.setValue(Resource.loading(null));

        int clampedPage = Math.max(1, page);
        int clampedPerPage = Math.max(MIN_PER_PAGE, Math.min(MAX_PER_PAGE, perPage));

        Call<PixabaySearchResponse> call = trimmedQuery.isEmpty()
                ? apiService.getFeaturedPhotos(
                        apiKey,
                        true,
                        DEFAULT_FEATURED_ORDER,
                        DEFAULT_IMAGE_TYPE,
                        DEFAULT_SAFE_SEARCH,
                        clampedPage,
                        clampedPerPage
                )
                : apiService.searchPhotos(
                        apiKey,
                        trimmedQuery,
                        DEFAULT_IMAGE_TYPE,
                        DEFAULT_SAFE_SEARCH,
                        clampedPage,
                        clampedPerPage
                );

        call.enqueue(new Callback<PixabaySearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<PixabaySearchResponse> call,
                                   @NonNull Response<PixabaySearchResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    int code = response.code();
                    String errorMsg;
                    if (code == 400 || code == 401 || code == 403) {
                        errorMsg = "Cannot load photos. Please verify your credentials or search parameters.";
                    } else if (code == 429) {
                        errorMsg = "Rate limit reached. Please wait a moment and try again.";
                    } else {
                        errorMsg = "Unable to load photos at this time. Please try again later.";
                    }
                    result.postValue(Resource.error(errorMsg, null));
                    return;
                }

                PixabaySearchResponse body = response.body();
                List<PixabayHitDto> dtos = body.getHits();

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
            public void onFailure(@NonNull Call<PixabaySearchResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                String message = "Unable to connect. Please check your internet connection.";
                result.postValue(Resource.error(message, null));
            }
        });

        return result;
    }

    /**
     * Generates rich, realistic demo photos supporting multi-page pagination and offline favorites
     * without requiring an active API key.
     */
    private void loadDemoPhotos(@NonNull String query, int page, int perPage,
                                @NonNull MutableLiveData<Resource<List<Photo>>> result) {
        result.setValue(Resource.loading(null));
        final String effectiveQuery = query.isEmpty() ? "Featured" : query;

        executors.networkIO().execute(() -> {
            try {
                // Simulate realistic network latency (300ms)
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            executors.diskIO().execute(() -> {
                Set<String> favoriteIds = new HashSet<>(photoDao.getFavoriteIdsSync());
                List<Photo> demoList = new ArrayList<>();
                int maxPages = 5;

                if (page <= maxPages) {
                    // Page 5 returns fewer photos to demonstrate end-of-feed pagination boundary
                    int count = (page == maxPages) ? Math.max(1, perPage / 2) : perPage;
                    int startOffset = (page - 1) * perPage + 1;

                    for (int i = 0; i < count; i++) {
                        int index = startOffset + i;
                        String photoId = "demo_" + effectiveQuery.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_") + "_" + index;
                        boolean isFav = favoriteIds.contains(photoId);

                        // High-quality CDN images with realistic varying aspect ratios for Pinterest masonry effect
                        int[] sampleHeights = new int[]{380, 260, 430, 310, 350, 280};
                        int thumbHeight = sampleHeights[index % sampleHeights.length];
                        String thumbUrl = "https://picsum.photos/seed/" + photoId + "/300/" + thumbHeight;
                        String detailUrl = "https://picsum.photos/seed/" + photoId + "/800/600";

                        demoList.add(new Photo(
                                photoId,
                                capitalize(effectiveQuery) + " #" + index,
                                "Photographer " + ((index % 10) + 1),
                                "High resolution photo of " + effectiveQuery + " (Demo Mode for testing without external API key).",
                                thumbUrl,
                                detailUrl,
                                isFav,
                                System.currentTimeMillis() - (index * 60000L),
                                300,
                                thumbHeight
                        ));
                    }
                }

                if (demoList.isEmpty()) {
                    result.postValue(Resource.empty());
                } else {
                    result.postValue(Resource.success(demoList));
                }
            });
        });
    }

    private static String capitalize(@NonNull String text) {
        if (text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
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
    public void restoreFavorite(@NonNull Photo photo) {
        long originalSavedAt = photo.getSavedAt();
        long effectiveSavedAt = originalSavedAt > 0L ? originalSavedAt : System.currentTimeMillis();
        executors.diskIO().execute(() -> {
            PhotoEntity updated = PhotoMapper.toEntity(photo, true, effectiveSavedAt);
            photoDao.upsertPhoto(updated);
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

    @NonNull
    @Override
    public LiveData<Resource<Long>> createCollection(@NonNull String name, @Nullable String description) {
        MutableLiveData<Resource<Long>> result = new MutableLiveData<>(Resource.loading(null));
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            result.setValue(Resource.error("Album name cannot be empty.", null));
            return result;
        }
        String cleanDesc = description != null ? description.trim() : "";
        executors.diskIO().execute(() -> {
            try {
                List<CollectionEntity> existingCollections = collectionDao.getCollectionsSync();
                if (existingCollections != null) {
                    for (CollectionEntity existing : existingCollections) {
                        if (existing.getName().equalsIgnoreCase(trimmedName)) {
                            result.postValue(Resource.error("An album with this name already exists.", null));
                            return;
                        }
                    }
                }
                CollectionEntity entity = new CollectionEntity(
                        0L,
                        trimmedName,
                        cleanDesc,
                        System.currentTimeMillis()
                );
                long id = collectionDao.insertCollection(entity);
                result.postValue(Resource.success(id));
            } catch (Exception e) {
                result.postValue(Resource.error("Failed to create album. Please try again.", null));
            }
        });
        return result;
    }

    @NonNull
    @Override
    public LiveData<Resource<Void>> addPhotoToCollection(long collectionId, @NonNull Photo photo) {
        return addPhotosToCollection(collectionId, java.util.Collections.singletonList(photo));
    }

    @NonNull
    @Override
    public LiveData<Resource<Void>> addPhotosToCollection(long collectionId, @NonNull List<Photo> photos) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>(Resource.loading(null));
        if (collectionId <= 0L) {
            result.setValue(Resource.error("Invalid album identifier.", null));
            return result;
        }
        if (photos.isEmpty()) {
            result.setValue(Resource.success(null));
            return result;
        }
        executors.diskIO().execute(() -> {
            try {
                long now = System.currentTimeMillis();
                List<PhotoEntity> entities = new ArrayList<>(photos.size());
                List<CollectionPhotoCrossRef> refs = new ArrayList<>(photos.size());
                for (Photo photo : photos) {
                    PhotoEntity existing = photoDao.getPhotoByIdSync(photo.getId());
                    boolean keepFavorite = existing != null && existing.isFavorite();
                    long savedAt = existing != null ? existing.getSavedAt() : now;
                    entities.add(PhotoMapper.toEntity(photo, keepFavorite, savedAt));
                    refs.add(new CollectionPhotoCrossRef(collectionId, photo.getId(), now));
                }
                collectionDao.addPhotosAndCrossRefs(photoDao, entities, refs);
                result.postValue(Resource.success(null));
            } catch (Exception e) {
                result.postValue(Resource.error("Failed to add photo(s) to album. Please try again.", null));
            }
        });
        return result;
    }

    @NonNull
    @Override
    public LiveData<Resource<Void>> deleteCollection(long collectionId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>(Resource.loading(null));
        if (collectionId <= 0L) {
            result.setValue(Resource.error("Invalid album identifier.", null));
            return result;
        }
        executors.diskIO().execute(() -> {
            try {
                collectionDao.deleteCollectionAndCleanOrphans(photoDao, collectionId);
                result.postValue(Resource.success(null));
            } catch (Exception e) {
                result.postValue(Resource.error("Failed to delete album. Please try again.", null));
            }
        });
        return result;
    }

    @NonNull
    @Override
    public LiveData<Resource<Void>> updateCollection(long collectionId, @NonNull String newName, @Nullable String newDescription) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>(Resource.loading(null));
        String trimmedName = newName.trim();
        if (trimmedName.isEmpty()) {
            result.setValue(Resource.error("Album name cannot be empty.", null));
            return result;
        }
        executors.diskIO().execute(() -> {
            try {
                List<CollectionEntity> existingCollections = collectionDao.getCollectionsSync();
                CollectionEntity target = null;
                if (existingCollections != null) {
                    for (CollectionEntity existing : existingCollections) {
                        if (existing.getId() == collectionId) {
                            target = existing;
                        } else if (existing.getName().equalsIgnoreCase(trimmedName)) {
                            result.postValue(Resource.error("An album with this name already exists.", null));
                            return;
                        }
                    }
                }
                String finalDesc = newDescription != null ? newDescription.trim() : (target != null ? target.getDescription() : "");
                int updatedRows = collectionDao.updateCollection(collectionId, trimmedName, finalDesc);
                if (updatedRows > 0) {
                    result.postValue(Resource.success(null));
                } else {
                    result.postValue(Resource.error("Album not found.", null));
                }
            } catch (Exception e) {
                result.postValue(Resource.error("Failed to update album. Please try again.", null));
            }
        });
        return result;
    }

    @NonNull
    @Override
    public LiveData<Resource<Void>> removePhotoFromCollection(long collectionId, @NonNull String photoId) {
        return removePhotosFromCollection(collectionId, java.util.Collections.singletonList(photoId));
    }
    @NonNull
    @Override
    public LiveData<Resource<Void>> removePhotosFromCollection(long collectionId, @NonNull List<String> photoIds) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>(Resource.loading(null));
        if (collectionId <= 0L || photoIds.isEmpty()) {
            result.setValue(Resource.success(null));
            return result;
        }
        executors.diskIO().execute(() -> {
            try {
                collectionDao.removePhotosAndCleanOrphans(photoDao, collectionId, photoIds);
                result.postValue(Resource.success(null));
            } catch (Exception e) {
                result.postValue(Resource.error("Failed to remove photo(s) from album: " + e.getMessage(), null));
            }
        });
        return result;
    }

    @NonNull
    @Override
    public LiveData<List<Photo>> observePhotosInCollection(long collectionId) {
        return Transformations.map(
                collectionDao.observeCollectionWithPhotosById(collectionId),
                relation -> {
                    if (relation == null || relation.photos == null) {
                        return java.util.Collections.emptyList();
                    }
                    return PhotoMapper.fromEntityList(relation.photos);
                }
        );
    }
}