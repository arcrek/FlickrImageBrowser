package vn.edu.usth.flickrimagebrowser.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.Objects;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.ui.collections.CollectionsViewModel;
import vn.edu.usth.flickrimagebrowser.ui.detail.DetailViewModel;
import vn.edu.usth.flickrimagebrowser.ui.favorites.FavoritesViewModel;
import vn.edu.usth.flickrimagebrowser.ui.search.SearchViewModel;

/**
 * Factory providing ViewModels with constructor-injected PhotoRepository.
 */
public class AppViewModelFactory implements ViewModelProvider.Factory {

    private final PhotoRepository repository;

    public AppViewModelFactory(@NonNull PhotoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SearchViewModel.class)) {
            return (T) new SearchViewModel(repository);
        }
        if (modelClass.isAssignableFrom(FavoritesViewModel.class)) {
            return (T) new FavoritesViewModel(repository);
        }
        if (modelClass.isAssignableFrom(CollectionsViewModel.class)) {
            return (T) new CollectionsViewModel(repository);
        }
        if (modelClass.isAssignableFrom(DetailViewModel.class)) {
            return (T) new DetailViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
