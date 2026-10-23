package vn.edu.usth.flickrimagebrowser.ui.favorites;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

public class FavoritesViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private PhotoRepository repository;

    private MutableLiveData<List<Photo>> source;
    private FavoritesViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        source = new MutableLiveData<>();
        when(repository.observeFavorites()).thenReturn(source);
        viewModel = new FavoritesViewModel(repository);
        viewModel.getFavorites().observeForever(photos -> { });
    }

    @Test
    public void filter_matchesTitleAndOwner_caseInsensitive() {
        source.setValue(Arrays.asList(photo("1", "Sea coast", "Alice", 10L),
                photo("2", "City", "Seaside Studio", 20L)));

        viewModel.setFilterQuery("sea");

        assertEquals(2, viewModel.getFavorites().getValue().size());
        viewModel.setFilterQuery("coast");
        assertEquals(1, viewModel.getFavorites().getValue().size());
        assertEquals("1", viewModel.getFavorites().getValue().get(0).getId());
    }

    @Test
    public void toggleSortOrder_switchesFromRecentToTitle() {
        source.setValue(Arrays.asList(photo("1", "Zulu", "A", 20L),
                photo("2", "Alpha", "B", 10L)));

        assertFalse(viewModel.isTitleAscending());
        assertEquals("1", viewModel.getFavorites().getValue().get(0).getId());
        viewModel.toggleSortOrder();
        assertTrue(viewModel.isTitleAscending());
        assertEquals("2", viewModel.getFavorites().getValue().get(0).getId());
    }

    @Test
    public void removeAndRestore_delegateToRepository() {
        Photo photo = photo("1", "Title", "Owner", 10L);

        viewModel.removeFavorite(photo);
        viewModel.restoreFavorite(photo);

        verify(repository).setFavorite(photo, false);
        verify(repository).setFavorite(photo, true);
    }

    private Photo photo(String id, String title, String owner, long savedAt) {
        return new Photo(id, title, owner, "", "https://example.com/thumb.jpg",
                "https://example.com/detail.jpg", true, savedAt);
    }
}
