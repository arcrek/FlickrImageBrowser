package vn.edu.usth.flickrimagebrowser.ui.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private String lastQuery;

    private int currentPage = DEFAULT_PAGE;
    private boolean isLoadingNextPage = false;
    private boolean hasMorePages = true;

    public SearchViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
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
     * Submits a keyword search query. Avoids repeating identical requests on configuration changes
     * unless forceRefresh is true. Resets pagination to page 1.
     */
    public void submitQuery(@NonNull String query, boolean forceRefresh) {
        String normalized = query.trim();
        if (normalized.isEmpty()) {
            return;
        }
        if (!forceRefresh && normalized.equals(lastQuery) && searchResults.getValue() != null) {
            return;
        }

        lastQuery = normalized;
        currentPage = DEFAULT_PAGE;
        hasMorePages = true;
        isLoadingNextPage = false;
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
        if (isLoadingNextPage || !hasMorePages || lastQuery == null || lastQuery.isEmpty() || accumulatedPhotos.isEmpty()) {
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
        if (lastQuery != null && !lastQuery.isEmpty()) {
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
    }
}
