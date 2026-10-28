package vn.edu.usth.flickrimagebrowser.ui.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;

/**
 * Unit tests for CollectionsViewModel testing collection creation, deletion, renaming, and name validation.
 */
public class CollectionsViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private PhotoRepository repository;

    private MutableLiveData<List<Collection>> collectionsLiveData;
    private CollectionsViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        collectionsLiveData = new MutableLiveData<>();
        when(repository.observeCollections()).thenReturn(collectionsLiveData);
        viewModel = new CollectionsViewModel(repository);
    }

    @Test
    public void getCollections_observesRepositoryLiveData() {
        List<Collection> sample = Arrays.asList(
                new Collection(1L, "Nature", "Outdoor photos", 1000L, 5, ""),
                new Collection(2L, "Travel", "Trips", 2000L, 12, "")
        );
        collectionsLiveData.setValue(sample);

        List<Collection> result = viewModel.getCollections().getValue();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Nature", result.get(0).getName());
        assertEquals("Travel", result.get(1).getName());
    }

    @Test
    public void createCollection_delegatesToRepository() {
        viewModel.createCollection("Architecture", "Buildings");
        verify(repository).createCollection("Architecture", "Buildings");
    }

    @Test
    public void createCollection_withWhitespaceOrEmpty_doesNotCallRepository() {
        viewModel.createCollection("   ", "Empty name");
        verify(repository, never()).createCollection(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void createCollection_withDuplicateName_doesNotCallRepository() {
        collectionsLiveData.setValue(Collections.singletonList(
                new Collection(1L, "Nature", "", 1000L, 0, "")
        ));

        viewModel.createCollection(" nature ", "Duplicate");

        verify(repository, never()).createCollection(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void deleteCollection_byCollectionObject_delegatesToRepository() {
        Collection collection = new Collection(10L, "Portrait", "People", 5000L, 3, "");
        viewModel.deleteCollection(collection);
        verify(repository).deleteCollection(10L);
    }

    @Test
    public void deleteCollection_byId_delegatesToRepository() {
        viewModel.deleteCollection(15L);
        verify(repository).deleteCollection(15L);
    }

    @Test
    public void updateCollection_delegatesToRepository() {
        viewModel.updateCollection(10L, "Urban Architecture", "Buildings");
        verify(repository).updateCollection(10L, "Urban Architecture", "Buildings");
    }

    @Test
    public void updateCollection_withWhitespaceOrEmpty_doesNotCallRepository() {
        viewModel.updateCollection(10L, "  ", null);
        verify(repository, never()).updateCollection(Mockito.anyLong(), Mockito.anyString(), Mockito.any());
    }

    @Test
    public void updateCollection_withDuplicateName_doesNotCallRepository() {
        collectionsLiveData.setValue(Arrays.asList(
                new Collection(10L, "Nature", "", 1000L, 0, ""),
                new Collection(11L, "Travel", "", 2000L, 0, "")
        ));

        viewModel.updateCollection(10L, " travel ", null);

        verify(repository, never()).updateCollection(Mockito.anyLong(), Mockito.anyString(), Mockito.any());
    }

    @Test
    public void isCollectionNameUnique_whenListEmpty_returnsTrue() {
        collectionsLiveData.setValue(Collections.emptyList());
        assertTrue(viewModel.isCollectionNameUnique("Nature", -1));
    }

    @Test
    public void isCollectionNameUnique_whenEmptyOrWhitespace_returnsFalse() {
        assertFalse(viewModel.isCollectionNameUnique("", -1));
        assertFalse(viewModel.isCollectionNameUnique("   ", -1));
    }

    @Test
    public void isCollectionNameUnique_whenDuplicateName_returnsFalse() {
        collectionsLiveData.setValue(Collections.singletonList(
                new Collection(1L, "Nature", "", 1000L, 0, "")
        ));

        // Exact match
        assertFalse(viewModel.isCollectionNameUnique("Nature", -1));
        // Case-insensitive match
        assertFalse(viewModel.isCollectionNameUnique("nature", -1));
        assertFalse(viewModel.isCollectionNameUnique(" NATURE ", -1));
    }

    @Test
    public void isCollectionNameUnique_whenExcludingCurrentId_returnsTrue() {
        collectionsLiveData.setValue(Collections.singletonList(
                new Collection(1L, "Nature", "", 1000L, 0, "")
        ));

        // When renaming the same collection without changing name, exclude its own ID
        assertTrue(viewModel.isCollectionNameUnique("Nature", 1L));
        assertTrue(viewModel.isCollectionNameUnique("nature", 1L));
    }

    @Test
    public void isCollectionNameUnique_whenDistinctName_returnsTrue() {
        collectionsLiveData.setValue(Collections.singletonList(
                new Collection(1L, "Nature", "", 1000L, 0, "")
        ));

        assertTrue(viewModel.isCollectionNameUnique("Urban", -1));
    }
}
