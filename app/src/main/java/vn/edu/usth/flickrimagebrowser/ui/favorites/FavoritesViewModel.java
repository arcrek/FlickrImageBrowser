package vn.edu.usth.flickrimagebrowser.ui.favorites;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Objects;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * ViewModel exposing locally persisted favorite photos from Room.
 */
public class FavoritesViewModel extends ViewModel {

    private final PhotoRepository repository;
    private final LiveData<List<Photo>> favorites;

    public FavoritesViewModel(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.favorites = repository.observeFavorites();
    }

    @NonNull
    public LiveData<List<Photo>> getFavorites() {
        return favorites;
    }

    public void toggleFavorite(@NonNull Photo photo) {
        repository.toggleFavorite(photo);
    }
}
