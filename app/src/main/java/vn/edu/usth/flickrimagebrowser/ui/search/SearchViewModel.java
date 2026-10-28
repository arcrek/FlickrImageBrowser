package vn.edu.usth.flickrimagebrowser.ui.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * ViewModel managing photo search queries, multi-page pagination accumulation,
 * and retaining results across configuration changes.
 */
public class SearchViewModel extends ViewModel {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PER_PAGE = 30;

    private final PhotoRepository repository;
    private final MediatorLiveData<Resource<List<Photo>>> searchResults = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);
    private final List<Photo> accumulatedPhotos = new ArrayList<>();

    @Nullable
    private LiveData<Resource<List<Photo>>> activePageSource;

    @Nullable
    private LiveData<List<Photo>> favoritesSource;

    @Nullable
    private String lastQuery;

    private int currentPage = DEFAULT_PAGE;
    private boolean isLoadingNextPage = false;
    private boolean hasMorePages = true;
    private boolean lastFailedOnNextPage = false;

    public SearchViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        favoritesSource = repository.observeFavorites();
        if (favoritesSource != null) {
            searchResults.addSource(favoritesSource, this::onFavoritesChanged);
        }
    }

    private void onFavoritesChanged(@Nullable List<Photo> favoritePhotos) {
        if (accumulatedPhotos.isEmpty()) {
            return;
        }
        Set<String> favIds = new HashSet<>();
        if (favoritePhotos != null) {
            for (Photo p : favoritePhotos) {
                favIds.add(p.getId());
            }
        }
        boolean changed = false;
        List<Photo> updated = new ArrayList<>(accumulatedPhotos.size());
        for (Photo p : accumulatedPhotos) {
            boolean isFav = favIds.contains(p.getId());
            if (p.isFavorite() != isFav) {
                changed = true;
                updated.add(p.withFavorite(isFav, isFav ? System.currentTimeMillis() : 0));
            } else {
                updated.add(p);
            }
        }
        if (changed) {
            accumulatedPhotos.clear();
            accumulatedPhotos.addAll(updated);
            Resource<List<Photo>> current = searchResults.getValue();
            if (current != null && current.getStatus() == Resource.Status.SUCCESS) {
                searchResults.setValue(Resource.success(new ArrayList<>(accumulatedPhotos)));
            }
        }
    }

    @NonNull
    public LiveData<Resource<List<Photo>>> getSearchResults() {
        return searchResults;
    }

    @NonNull
    public LiveData<Boolean> getIsLoadingMore() {
        return isLoadingMore;
    }

    @Nullable
    public String getLastQuery() {
        return lastQuery;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public boolean isLoadingNextPage() {
        return isLoadingNextPage;
    }

    public boolean hasMorePages() {
        return hasMorePages;
    }

    /**
     * Loads the initial featured photos feed when no query or featured feed has been loaded yet.
     */
    public void loadInitialFeaturedIfNeeded() {
        if (lastQuery == null && searchResults.getValue() == null) {
            submitQuery("", false);
        }
    }

    /**
     * Submits a keyword search query (or empty string for the featured photos feed).
     * Avoids repeating identical requests on configuration changes unless forceRefresh is true.
     * Resets pagination to page 1.
     */
    public void submitQuery(@NonNull String query, boolean forceRefresh) {
        String normalized = query.trim();
        if (!forceRefresh && normalized.equals(lastQuery) && searchResults.getValue() != null) {
            return;
        }

        lastQuery = normalized;
        currentPage = DEFAULT_PAGE;
        hasMorePages = true;
        isLoadingNextPage = false;
        lastFailedOnNextPage = false;
        isLoadingMore.setValue(false);
        accumulatedPhotos.clear();

        if (activePageSource != null) {
            searchResults.removeSource(activePageSource);
            activePageSource = null;
        }

        searchResults.setValue(Resource.loading(null));
        activePageSource = repository.searchPhotos(normalized, DEFAULT_PAGE, DEFAULT_PER_PAGE);

        searchResults.addSource(activePageSource, resource -> {
            if (resource == null) {
                return;
            }

            switch (resource.getStatus()) {
                case LOADING:
                    searchResults.setValue(Resource.loading(null));
                    break;

                case SUCCESS:
                    if (activePageSource != null) {
                        searchResults.removeSource(activePageSource);
                        activePageSource = null;
                    }
                    List<Photo> data = resource.getData();
                    if (data == null || data.isEmpty()) {
                        hasMorePages = false;
                        searchResults.setValue(Resource.empty());
                    } else {
                        accumulatedPhotos.clear();
                        accumulatedPhotos.addAll(data);
                        if (data.size() < DEFAULT_PER_PAGE) {
                            hasMorePages = false;
                        }
                        searchResults.setValue(Resource.success(new ArrayList<>(accumulatedPhotos)));
                    }
                    break;

                case EMPTY:
                    if (activePageSource != null) {
                        searchResults.removeSource(activePageSource);
                        activePageSource = null;
                    }
                    hasMorePages = false;
                    searchResults.setValue(Resource.empty());
                    break;

                case ERROR:
                    if (activePageSource != null) {
                        searchResults.removeSource(activePageSource);
                        activePageSource = null;
                    }
                    String errorMessage = resource.getMessage() != null
                            ? resource.getMessage()
                            : "Error loading photos";
                    searchResults.setValue(Resource.error(errorMessage, null));
                    break;
            }
        });
    }

    /**
     * Loads the next page of results for the active query and appends new photos to the
     * existing accumulated list.
     */
    public void loadNextPage() {
        if (isLoadingNextPage || lastFailedOnNextPage || !hasMorePages
                || lastQuery == null || accumulatedPhotos.isEmpty()) {
            return;
        }

        isLoadingNextPage = true;
        isLoadingMore.setValue(true);
        int nextPage = currentPage + 1;

        if (activePageSource != null) {
            searchResults.removeSource(activePageSource);
            activePageSource = null;
        }

        activePageSource = repository.searchPhotos(lastQuery, nextPage, DEFAULT_PER_PAGE);
        searchResults.addSource(activePageSource, resource -> {
            if (resource == null) {
                return;
            }

            switch (resource.getStatus()) {
                case LOADING:
                    searchResults.setValue(Resource.loading(new ArrayList<>(accumulatedPhotos)));
                    break;

                case SUCCESS:
                    if (activePageSource != null) {
                        searchResults.removeSource(activePageSource);
                        activePageSource = null;
                    }
                    isLoadingNextPage = false;
                    lastFailedOnNextPage = false;
                    isLoadingMore.setValue(false);

                    List<Photo> newPhotos = resource.getData();
                    if (newPhotos != null && !newPhotos.isEmpty()) {
                        accumulatedPhotos.addAll(newPhotos);
                        currentPage = nextPage;
                        if (newPhotos.size() < DEFAULT_PER_PAGE) {
                            hasMorePages = false;
                        }
                    } else {
                        hasMorePages = false;
                    }
                    searchResults.setValue(Resource.success(new ArrayList<>(accumulatedPhotos)));
                    break;

                case EMPTY:
                    if (activePageSource != null) {
                        searchResults.removeSource(activePageSource);
                        activePageSource = null;
                    }
                    isLoadingNextPage = false;
                    lastFailedOnNextPage = false;
                    isLoadingMore.setValue(false);
                    hasMorePages = false;
                    searchResults.setValue(Resource.success(new ArrayList<>(accumulatedPhotos)));
                    break;

                case ERROR:
                    if (activePageSource != null) {
                        searchResults.removeSource(activePageSource);
                        activePageSource = null;
                    }
                    isLoadingNextPage = false;
                    lastFailedOnNextPage = true;
                    isLoadingMore.setValue(false);
                    String message = resource.getMessage() != null
                            ? resource.getMessage()
                            : "Error loading more photos";
                    searchResults.setValue(Resource.error(message, new ArrayList<>(accumulatedPhotos)));
                    break;
            }
        });
    }

    public void retryLastQuery() {
        if (lastQuery == null) {
            return;
        }
        if (lastFailedOnNextPage && !accumulatedPhotos.isEmpty()) {
            lastFailedOnNextPage = false;
            loadNextPage();
        } else {
            submitQuery(lastQuery, true);
        }
    }

    public void toggleFavorite(@NonNull Photo photo) {
        repository.toggleFavorite(photo);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (activePageSource != null) {
            searchResults.removeSource(activePageSource);
            activePageSource = null;
        }
        if (favoritesSource != null) {
            searchResults.removeSource(favoritesSource);
            favoritesSource = null;
        }
    }
}
