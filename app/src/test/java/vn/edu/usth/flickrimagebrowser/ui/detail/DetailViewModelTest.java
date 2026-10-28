package vn.edu.usth.flickrimagebrowser.ui.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Unit tests for DetailViewModel verifying favorite observation/toggling,
 * collection list observation, and adding photo to collection with observable mutation result.
 */
public class DetailViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private PhotoRepository repository;

    @Mock
    private Observer<Photo> photoObserver;

    @Mock
    private Observer<Boolean> favoriteObserver;

    @Mock
    private Observer<List<Collection>> collectionsObserver;

    private DetailViewModel viewModel;
    private Photo samplePhoto;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        samplePhoto = new Photo(
                "p100", "Sunset Beach", "John Doe", "A beautiful sunset",
                "https://example.com/thumb.jpg", "https://example.com/large.jpg",
                false, 0L, 0, 0
        );

        MutableLiveData<List<Collection>> collectionsLive = new MutableLiveData<>();
        when(repository.observeCollections()).thenReturn(collectionsLive);

        viewModel = new DetailViewModel(repository);
    }

    @Test
    public void setPhoto_and_getPhoto_returnsSetPhoto() {
        viewModel.getPhoto().observeForever(photoObserver);
        viewModel.setPhoto(samplePhoto);

        assertNotNull(viewModel.getPhoto().getValue());
        assertEquals("p100", viewModel.getPhoto().getValue().getId());
        assertEquals("Sunset Beach", viewModel.getPhoto().getValue().getTitle());
        verify(photoObserver, times(1)).onChanged(samplePhoto);
    }

    @Test
    public void getIsFavorite_observesFavoriteStateFromRepository() {
        MutableLiveData<Boolean> favoriteLive = new MutableLiveData<>();
        favoriteLive.setValue(true);
        when(repository.observeIsFavorite("p100")).thenReturn(favoriteLive);

        viewModel.getIsFavorite().observeForever(favoriteObserver);
        viewModel.setPhoto(samplePhoto);

        assertEquals(Boolean.TRUE, viewModel.getIsFavorite().getValue());
        verify(repository, times(1)).observeIsFavorite("p100");
        verify(favoriteObserver, times(1)).onChanged(true);
    }

    @Test
    public void toggleFavorite_callsRepositoryToggleFavorite() {
        viewModel.setPhoto(samplePhoto);
        viewModel.toggleFavorite();

        verify(repository, times(1)).toggleFavorite(samplePhoto);
    }

    @Test
    public void getCollections_observesCollectionsFromRepository() {
        Collection collection = new Collection(1L, "Nature", "Nature photos", 1000L, 1, "");
        MutableLiveData<List<Collection>> collectionsLive = new MutableLiveData<>(Collections.singletonList(collection));
        when(repository.observeCollections()).thenReturn(collectionsLive);

        DetailViewModel vm = new DetailViewModel(repository);
        vm.getCollections().observeForever(collectionsObserver);

        assertNotNull(vm.getCollections().getValue());
        assertEquals(1, vm.getCollections().getValue().size());
        assertEquals("Nature", vm.getCollections().getValue().get(0).getName());
        verify(repository, times(2)).observeCollections();
    }

    @Test
    public void addPhotoToCollection_callsRepositoryAndReturnsObservableResource() {
        MutableLiveData<Resource<Void>> mutationLive = new MutableLiveData<>();
        mutationLive.setValue(Resource.success(null));
        when(repository.addPhotoToCollection(10L, samplePhoto)).thenReturn(mutationLive);

        viewModel.setPhoto(samplePhoto);
        LiveData<Resource<Void>> result = viewModel.addPhotoToCollection(10L);

        verify(repository, times(1)).addPhotoToCollection(10L, samplePhoto);
        assertNotNull(result);
        assertEquals(Resource.Status.SUCCESS, result.getValue().getStatus());
    }

    @Test
    public void addToCollection_delegatesToAddPhotoToCollection() {
        MutableLiveData<Resource<Void>> mutationLive = new MutableLiveData<>();
        mutationLive.setValue(Resource.success(null));
        when(repository.addPhotoToCollection(10L, samplePhoto)).thenReturn(mutationLive);

        viewModel.setPhoto(samplePhoto);
        LiveData<Resource<Void>> result = viewModel.addToCollection(10L);

        verify(repository, times(1)).addPhotoToCollection(10L, samplePhoto);
        assertNotNull(result);
        assertEquals(Resource.Status.SUCCESS, result.getValue().getStatus());
    }

    @Test
    public void addPhotoToCollection_withoutPhoto_returnsErrorResource() {
        LiveData<Resource<Void>> result = viewModel.addPhotoToCollection(10L);

        assertNotNull(result);
        assertNotNull(result.getValue());
        assertEquals(Resource.Status.ERROR, result.getValue().getStatus());
    }
}
