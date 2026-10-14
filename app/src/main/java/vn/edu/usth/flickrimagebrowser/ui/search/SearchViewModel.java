package vn.edu.usth.flickrimagebrowser.ui.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.List;
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
    private final MutableLiveData<SearchTrigger> searchTrigger = new MutableLiveData<>();
    private final LiveData<Resource<List<Photo>>> searchResults;

    @Nullable
    private String lastQuery;

    public SearchViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.searchResults = Transformations.switchMap(
                searchTrigger,
                trigger -> repository.searchPhotos(trigger.query, trigger.page, trigger.perPage)
        );
    }

    @NonNull
    public LiveData<Resource<List<Photo>>> getSearchResults() {
        return searchResults;
    }

    @Nullable
    public String getLastQuery() {
        return lastQuery;
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
        if (!forceRefresh && normalized.equals(lastQuery) && searchTrigger.getValue() != null) {
            return;
        }
        lastQuery = normalized;
        searchTrigger.setValue(new SearchTrigger(normalized, DEFAULT_PAGE, DEFAULT_PER_PAGE));
    }

    public void retryLastQuery() {
        if (lastQuery != null && !lastQuery.isEmpty()) {
            submitQuery(lastQuery, true);
        }
    }

    public void toggleFavorite(@NonNull Photo photo) {
        repository.toggleFavorite(photo);
    }

    private static final class SearchTrigger {
        final String query;
        final int page;
        final int perPage;

        SearchTrigger(String query, int page, int perPage) {
            this.query = query;
            this.page = page;
            this.perPage = perPage;
        }
    }
}
