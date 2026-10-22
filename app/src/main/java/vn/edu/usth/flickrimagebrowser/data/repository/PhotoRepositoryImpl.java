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
import vn.edu.usth.flickrimagebrowser.data.remote.FlickrApiService;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PhotoDto;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PhotoMapper;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PhotosSearchResponse;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.AppExecutors;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Default repository implementation coordinating Flickr REST API searches, Room persistence,
 * and a mock demo fallback mode for testing without requiring a paid Flickr Pro account.
 */
public class PhotoRepositoryImpl implements PhotoRepository {

    public static final String PLACEHOLDER_API_KEY = "your_flickr_api_key_here";
    public static final String DEMO_API_KEY = "demo";

    private final FlickrApiService apiService;
    private final PhotoDao photoDao;
    private final CollectionDao collectionDao;
    private final AppExecutors executors;
    private final String apiKey;

    public PhotoRepositoryImpl(@NonNull FlickrApiService apiService,
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

        if (apiKey.isEmpty()) {
            result.setValue(Resource.error(
                    "Flickr API key is not configured. Add FLICKR_API_KEY to local.properties.",
                    null
            ));
            return result;
        }

        // Demo / Mock Mode when using demo or placeholder key (bypasses Flickr Pro requirements for university testing)
        if (DEMO_API_KEY.equalsIgnoreCase(apiKey) || PLACEHOLDER_API_KEY.equalsIgnoreCase(apiKey)) {
            loadDemoPhotos(trimmedQuery, page, perPage, result);
            return result;
        }

        result.setValue(Resource.loading(null));

        Call<PhotosSearchResponse> call = apiService.searchPhotos(
                FlickrApiService.METHOD_PHOTOS_SEARCH,
                apiKey,
                trimmedQuery,
                Math.max(1, page),
                Math.max(1, perPage),
                FlickrApiService.DEFAULT_EXTRAS,
                FlickrApiService.SAFE_SEARCH_SAFE,
                FlickrApiService.FORMAT_JSON,
                FlickrApiService.NO_JSON_CALLBACK
        );

        call.enqueue(new Callback<PhotosSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<PhotosSearchResponse> call,
                                   @NonNull Response<PhotosSearchResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    result.postValue(Resource.error(
                            "Server returned HTTP " + response.code(),
                            null
                    ));
                    return;
                }

                PhotosSearchResponse body = response.body();
                if (!body.isOk()) {
                    String apiErrorMessage = body.getMessage() != null && !body.getMessage().trim().isEmpty()
                            ? body.getMessage().trim()
                            : "Flickr API error (code " + body.getCode() + ")";
                    result.postValue(Resource.error(apiErrorMessage, null));
                    return;
                }

                List<PhotoDto> dtos = body.getPhotos() != null
                        ? body.getPhotos().getPhotoList()
                        : null;

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
            public void onFailure(@NonNull Call<PhotosSearchResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                String message = t.getMessage() != null && !t.getMessage().trim().isEmpty()
                        ? "Network error: " + t.getMessage().trim()
                        : "Network error: Unable to reach Flickr.";
                result.postValue(Resource.error(message, null));
            }
        });

        return result;
    }

    /**
     * Generates rich, realistic demo photos supporting multi-page pagination and offline favorites
     * without requiring an active Flickr Pro API key.
     */
    private void loadDemoPhotos(@NonNull String query, int page, int perPage,
                                @NonNull MutableLiveData<Resource<List<Photo>>> result) {
        result.setValue(Resource.loading(null));

        executors.networkIO().execute(() -> {
            try {
                // Simulate realistic network latency (300ms)
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
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
                        String photoId = "demo_" + query.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_") + "_" + index;
                        boolean isFav = favoriteIds.contains(photoId);

                        // High-quality CDN images with realistic varying aspect ratios for Pinterest masonry effect
                        int[] sampleHeights = new int[]{380, 260, 430, 310, 350, 280};
                        int thumbHeight = sampleHeights[index % sampleHeights.length];
                        String thumbUrl = "https://picsum.photos/seed/" + photoId + "/300/" + thumbHeight;
                        String detailUrl = "https://picsum.photos/seed/" + photoId + "/800/600";

                        demoList.add(new Photo(
                                photoId,
                                capitalize(query) + " #" + index,
                                "Photographer " + ((index % 10) + 1),
                                "High resolution photo of " + query + " (Demo Mode for testing without Flickr Pro).",
                                thumbUrl,
                                detailUrl,
                                isFav,
                                System.currentTimeMillis() - (index * 60000L)
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
        String cleanDescription = description != null ? description.trim() : "";
        executors.diskIO().execute(() -> {
            CollectionEntity entity = new CollectionEntity(
                    0L,
                    trimmedName,
                    cleanDescription,
                    System.currentTimeMillis()
            );
            try {
                collectionDao.insertCollection(entity);
            } catch (RuntimeException ignored) {
                // Ignore duplicate collection name constraint violations in boilerplate
            }
        });
    }

    @Override
    public void addPhotoToCollection(long collectionId, @NonNull Photo photo) {
        executors.diskIO().execute(() -> {
            PhotoEntity existing = photoDao.getPhotoByIdSync(photo.getId());
            boolean isFav = existing != null && existing.isFavorite();
            long savedAt = existing != null ? existing.getSavedAt() : System.currentTimeMillis();
            photoDao.upsertPhoto(PhotoMapper.toEntity(photo, isFav, savedAt));
            collectionDao.addPhotoToCollection(
                    new CollectionPhotoCrossRef(collectionId, photo.getId(), System.currentTimeMillis())
            );
        });
    }
}
