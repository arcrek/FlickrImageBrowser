package vn.edu.usth.flickrimagebrowser.ui.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * ViewModel managing photo search queries and retaining results across configuration changes.
 */
public class SearchViewModel extends ViewModel {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PER_PAGE = 30;

    private final PhotoRepository repository;
    private final MediatorLiveData<Resource<List<Photo>>> searchResults = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private LiveData<Resource<List<Photo>>> activeRequest;

    @Nullable
    private String lastQuery;
    private int currentPage;
    private int requestGeneration;
    private boolean hasMorePages;
    private boolean loadingPage;

    public SearchViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @NonNull
    public LiveData<Resource<List<Photo>>> getSearchResults() {
        return searchResults;
    }

    @Nullable
    public String getLastQuery() {
        return lastQuery;
    }

    @NonNull
    public LiveData<Boolean> isLoadingMore() {
        return loadingMore;
    }

    /**
     * Submits a keyword search query. Avoids repeating identical requests on configuration changes
     * unless forceRefresh is true.
     */
    public void submitQuery(@NonNull String query, boolean forceRefresh) {
        String normalized = query.trim();
        if (normalized.isEmpty()) {
            return;
        }
        if (!forceRefresh && normalized.equals(lastQuery) && (currentPage > 0 || loadingPage)) {
            return;
        }
        lastQuery = normalized;
        currentPage = 0;
        hasMorePages = true;
        loadingPage = false;
        requestPage(DEFAULT_PAGE, false);
    }

    public void retryLastQuery() {
        if (lastQuery != null && !lastQuery.isEmpty()) {
            submitQuery(lastQuery, true);
        }
    }

    /** Loads and appends the next Flickr page if one is not already in flight. */
    public void loadNextPage() {
        if (lastQuery == null || loadingPage || !hasMorePages) {
            return;
        }
        requestPage(currentPage + 1, true);
    }

    public void toggleFavorite(@NonNull Photo photo) {
        boolean newState = !photo.isFavorite();
        Photo updated = photo.withFavorite(newState,
                newState ? System.currentTimeMillis() : photo.getSavedAt());
        repository.setFavorite(updated, newState);

        Resource<List<Photo>> current = searchResults.getValue();
        if (current == null || current.getData() == null) {
            return;
        }
        List<Photo> updatedPhotos = new ArrayList<>(current.getData().size());
        for (Photo item : current.getData()) {
            updatedPhotos.add(item.getId().equals(updated.getId()) ? updated : item);
        }
        searchResults.setValue(Resource.success(updatedPhotos));
    }

    private void requestPage(int page, boolean append) {
        loadingPage = true;
        loadingMore.setValue(append);
        if (!append) {
            searchResults.setValue(Resource.loading(null));
        }
        if (activeRequest != null) {
            searchResults.removeSource(activeRequest);
        }

        int generation = ++requestGeneration;
        LiveData<Resource<List<Photo>>> request = repository.searchPhotos(
                lastQuery, page, DEFAULT_PER_PAGE);
        activeRequest = request;
        searchResults.addSource(request, resource -> {
            if (generation != requestGeneration || resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.LOADING) {
                if (!append) {
                    searchResults.setValue(Resource.loading(null));
                }
                return;
            }

            loadingPage = false;
            loadingMore.setValue(false);
            List<Photo> pagePhotos = resource.getData();
            if (resource.getStatus() == Resource.Status.ERROR) {
                List<Photo> previous = currentPhotos();
                searchResults.setValue(Resource.error(resource.getMessage(),
                        append && !previous.isEmpty() ? previous : null));
            } else if (resource.getStatus() == Resource.Status.EMPTY
                    || pagePhotos == null || pagePhotos.isEmpty()) {
                hasMorePages = false;
                if (append) {
                    List<Photo> previous = currentPhotos();
                    searchResults.setValue(previous.isEmpty()
                            ? Resource.empty() : Resource.success(previous));
                } else {
                    searchResults.setValue(Resource.empty());
                }
                currentPage = page;
            } else {
                Map<String, Photo> deduplicated = new LinkedHashMap<>();
                if (append) {
                    for (Photo photo : currentPhotos()) {
                        deduplicated.put(photo.getId(), photo);
                    }
                }
                for (Photo photo : pagePhotos) {
                    deduplicated.put(photo.getId(), photo);
                }
                hasMorePages = pagePhotos.size() >= DEFAULT_PER_PAGE;
                currentPage = page;
                searchResults.setValue(Resource.success(new ArrayList<>(deduplicated.values())));
            }

            searchResults.removeSource(request);
            if (activeRequest == request) {
                activeRequest = null;
            }
        });
    }

    @NonNull
    private List<Photo> currentPhotos() {
        Resource<List<Photo>> current = searchResults.getValue();
        return current != null && current.getData() != null
                ? new ArrayList<>(current.getData()) : new ArrayList<>();
    }
}
