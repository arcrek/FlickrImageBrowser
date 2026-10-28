package vn.edu.usth.flickrimagebrowser.ui.favorites;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * ViewModel exposing locally persisted favorite photos from Room with local filtering,
 * sort order switching, and remove-with-undo UX support.
 */
public class FavoritesViewModel extends ViewModel {

    public enum SortOrder {
        SAVED_DATE_DESC,
        TITLE_ASC
    }

    private final PhotoRepository repository;
    private final LiveData<List<Photo>> rawFavorites;
    private final MutableLiveData<String> filterQuery = new MutableLiveData<>("");
    private final MutableLiveData<SortOrder> sortOrder = new MutableLiveData<>(SortOrder.SAVED_DATE_DESC);
    private final MediatorLiveData<List<Photo>> displayedFavorites = new MediatorLiveData<>();

    @Nullable
    private Photo recentlyRemovedPhoto;

    public FavoritesViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.rawFavorites = repository.observeFavorites();

        displayedFavorites.addSource(rawFavorites, photos -> updateDisplayedFavorites());
        displayedFavorites.addSource(filterQuery, query -> updateDisplayedFavorites());
        displayedFavorites.addSource(sortOrder, order -> updateDisplayedFavorites());
    }

    @NonNull
    public LiveData<List<Photo>> getFavorites() {
        return displayedFavorites;
    }

    @NonNull
    public LiveData<List<Photo>> getRawFavorites() {
        return rawFavorites;
    }

    @NonNull
    public LiveData<String> getFilterQuery() {
        return filterQuery;
    }

    @NonNull
    public LiveData<SortOrder> getSortOrder() {
        return sortOrder;
    }

    public void setFilterQuery(@Nullable String query) {
        String normalized = query != null ? query : "";
        if (!Objects.equals(filterQuery.getValue(), normalized)) {
            filterQuery.setValue(normalized);
        }
    }

    public void setSortOrder(@NonNull SortOrder order) {
        Objects.requireNonNull(order, "order must not be null");
        if (sortOrder.getValue() != order) {
            sortOrder.setValue(order);
        }
    }

    public boolean isFilterActive() {
        String current = filterQuery.getValue();
        return current != null && !current.trim().isEmpty();
    }

    public boolean hasSavedFavorites() {
        List<Photo> list = rawFavorites.getValue();
        return list != null && !list.isEmpty();
    }

    public void removeFavorite(@NonNull Photo photo) {
        Objects.requireNonNull(photo, "photo must not be null");
        this.recentlyRemovedPhoto = photo;
        repository.setFavorite(photo, false);
    }

    public void restoreFavorite(@Nullable Photo photo) {
        Photo target = photo != null ? photo : recentlyRemovedPhoto;
        if (target != null) {
            repository.restoreFavorite(target);
            if (target == recentlyRemovedPhoto) {
                recentlyRemovedPhoto = null;
            }
        }
    }

    public void toggleFavorite(@NonNull Photo photo) {
        if (photo.isFavorite()) {
            removeFavorite(photo);
        } else {
            repository.toggleFavorite(photo);
        }
    }

    @Nullable
    public Photo getRecentlyRemovedPhoto() {
        return recentlyRemovedPhoto;
    }

    private void updateDisplayedFavorites() {
        List<Photo> source = rawFavorites.getValue();
        if (source == null || source.isEmpty()) {
            displayedFavorites.setValue(Collections.emptyList());
            return;
        }

        String query = filterQuery.getValue();
        String normalizedQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        SortOrder order = sortOrder.getValue() != null ? sortOrder.getValue() : SortOrder.SAVED_DATE_DESC;

        List<Photo> filtered = new ArrayList<>();
        for (Photo photo : source) {
            if (normalizedQuery.isEmpty()) {
                filtered.add(photo);
            } else {
                boolean matchesTitle = photo.getTitle().toLowerCase(Locale.ROOT).contains(normalizedQuery);
                boolean matchesOwner = photo.getOwnerName().toLowerCase(Locale.ROOT).contains(normalizedQuery);
                if (matchesTitle || matchesOwner) {
                    filtered.add(photo);
                }
            }
        }

        if (order == SortOrder.TITLE_ASC) {
            filtered.sort(Comparator.comparing(Photo::getTitle, String.CASE_INSENSITIVE_ORDER));
        } else {
            filtered.sort((p1, p2) -> Long.compare(p2.getSavedAt(), p1.getSavedAt()));
        }

        displayedFavorites.setValue(filtered);
    }
}
