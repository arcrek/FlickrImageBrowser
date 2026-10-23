package vn.edu.usth.flickrimagebrowser.ui.favorites;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * ViewModel exposing locally persisted favorite photos from Room.
 */
public class FavoritesViewModel extends ViewModel {

    private final PhotoRepository repository;
    private final MediatorLiveData<List<Photo>> favorites = new MediatorLiveData<>();
    private List<Photo> allFavorites = new ArrayList<>();
    private String filterQuery = "";
    private boolean titleAscending;

    public FavoritesViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        favorites.addSource(repository.observeFavorites(), photos -> {
            allFavorites = photos != null ? new ArrayList<>(photos) : new ArrayList<>();
            publishFavorites();
        });
    }

    @NonNull
    public LiveData<List<Photo>> getFavorites() {
        return favorites;
    }

    public void toggleFavorite(@NonNull Photo photo) {
        repository.toggleFavorite(photo);
    }

    public void setFilterQuery(@NonNull String query) {
        filterQuery = query.trim().toLowerCase(Locale.ROOT);
        publishFavorites();
    }

    public void toggleSortOrder() {
        titleAscending = !titleAscending;
        publishFavorites();
    }

    public boolean isTitleAscending() {
        return titleAscending;
    }

    public void removeFavorite(@NonNull Photo photo) {
        repository.setFavorite(photo, false);
    }

    public void restoreFavorite(@NonNull Photo photo) {
        repository.setFavorite(photo, true);
    }

    private void publishFavorites() {
        List<Photo> filtered = new ArrayList<>();
        for (Photo photo : allFavorites) {
            if (filterQuery.isEmpty()
                    || photo.getTitle().toLowerCase(Locale.ROOT).contains(filterQuery)
                    || photo.getOwnerName().toLowerCase(Locale.ROOT).contains(filterQuery)) {
                filtered.add(photo);
            }
        }
        if (titleAscending) {
            filtered.sort(Comparator.comparing((Photo photo) -> photo.getTitle().toLowerCase(Locale.ROOT)));
        } else {
            filtered.sort(Comparator.comparingLong(Photo::getSavedAt).reversed());
        }
        favorites.setValue(filtered);
    }
}
