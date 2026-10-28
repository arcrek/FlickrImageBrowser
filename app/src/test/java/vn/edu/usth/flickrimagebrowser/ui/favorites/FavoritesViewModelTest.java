package vn.edu.usth.flickrimagebrowser.ui.favorites;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * Unit tests for FavoritesViewModel verifying local search filtering,
 * sort order switching (Newest vs A-Z), and remove-with-undo UX behavior.
 */
public class FavoritesViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private PhotoRepository repository;

    @Mock
    private Observer<List<Photo>> observer;

    @Captor
    private ArgumentCaptor<List<Photo>> listCaptor;

    private MutableLiveData<List<Photo>> rawFavoritesLive;
    private FavoritesViewModel viewModel;

    private Photo photoForest;
    private Photo photoCoast;
    private Photo photoWaterfall;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        photoForest = new Photo(
                "1", "Rừng xanh", "An Nguyễn", "Desc 1",
                "https://example.com/1_thumb.jpg", "https://example.com/1_large.jpg",
                true, 1000L, 0, 0
        );
        photoCoast = new Photo(
                "2", "Bờ biển", "Bình Trần", "Desc 2",
                "https://example.com/2_thumb.jpg", "https://example.com/2_large.jpg",
                true, 3000L, 0, 0
        );
        photoWaterfall = new Photo(
                "3", "Thác nước", "An Nguyễn", "Desc 3",
                "https://example.com/3_thumb.jpg", "https://example.com/3_large.jpg",
                true, 2000L, 0, 0
        );

        rawFavoritesLive = new MutableLiveData<>();
        rawFavoritesLive.setValue(Arrays.asList(photoForest, photoCoast, photoWaterfall));

        when(repository.observeFavorites()).thenReturn(rawFavoritesLive);
        viewModel = new FavoritesViewModel(repository);
    }

    @Test
    public void initialState_emitsAllFavoritesSortedByNewestFirst() {
        viewModel.getFavorites().observeForever(observer);

        verify(observer, atLeastOnce()).onChanged(listCaptor.capture());
        List<Photo> emitted = listCaptor.getValue();
        assertNotNull(emitted);
        assertEquals(3, emitted.size());

        // Default sort is SAVED_DATE_DESC: Coast (3000L) -> Waterfall (2000L) -> Forest (1000L)
        assertEquals("2", emitted.get(0).getId());
        assertEquals("3", emitted.get(1).getId());
        assertEquals("1", emitted.get(2).getId());
        assertFalse(viewModel.isFilterActive());
        assertTrue(viewModel.hasSavedFavorites());
    }

    @Test
    public void filterByTitle_returnsOnlyMatchingPhotos() {
        viewModel.getFavorites().observeForever(observer);

        viewModel.setFilterQuery("biển");

        List<Photo> filtered = viewModel.getFavorites().getValue();
        assertNotNull(filtered);
        assertEquals(1, filtered.size());
        assertEquals("Bờ biển", filtered.get(0).getTitle());
        assertTrue(viewModel.isFilterActive());
    }

    @Test
    public void filterByOwner_returnsOnlyMatchingPhotos() {
        viewModel.getFavorites().observeForever(observer);

        viewModel.setFilterQuery("An Nguyễn");

        List<Photo> filtered = viewModel.getFavorites().getValue();
        assertNotNull(filtered);
        assertEquals(2, filtered.size());
        assertEquals("Thác nước", filtered.get(0).getTitle());
        assertEquals("Rừng xanh", filtered.get(1).getTitle());
    }

    @Test
    public void filterNoMatch_emitsEmptyListAndFilterActiveTrue() {
        viewModel.getFavorites().observeForever(observer);

        viewModel.setFilterQuery("nonexistent query");

        List<Photo> filtered = viewModel.getFavorites().getValue();
        assertNotNull(filtered);
        assertTrue(filtered.isEmpty());
        assertTrue(viewModel.isFilterActive());
        assertTrue(viewModel.hasSavedFavorites());
    }

    @Test
    public void changeSortOrderToTitleAsc_sortsAlphabetically() {
        viewModel.getFavorites().observeForever(observer);

        viewModel.setSortOrder(FavoritesViewModel.SortOrder.TITLE_ASC);

        List<Photo> sorted = viewModel.getFavorites().getValue();
        assertNotNull(sorted);
        assertEquals(3, sorted.size());

        // A-Z: Bờ biển -> Rừng xanh -> Thác nước
        assertEquals("Bờ biển", sorted.get(0).getTitle());
        assertEquals("Rừng xanh", sorted.get(1).getTitle());
        assertEquals("Thác nước", sorted.get(2).getTitle());
    }

    @Test
    public void removeFavorite_callsRepositorySetFavoriteFalseAndCachesTarget() {
        viewModel.removeFavorite(photoForest);

        verify(repository).setFavorite(photoForest, false);
        assertEquals(photoForest, viewModel.getRecentlyRemovedPhoto());
    }

    @Test
    public void restoreFavorite_callsRepositoryRestoreFavoritePreservingSavedAtAndClearsTarget() {
        viewModel.removeFavorite(photoForest);
        viewModel.restoreFavorite(null);

        verify(repository).restoreFavorite(photoForest);
        assertNull(viewModel.getRecentlyRemovedPhoto());
    }

    @Test
    public void toggleFavorite_whenPhotoIsFavorite_removesAndCachesForUndo() {
        viewModel.toggleFavorite(photoCoast);

        verify(repository).setFavorite(photoCoast, false);
        assertEquals(photoCoast, viewModel.getRecentlyRemovedPhoto());
    }
}
