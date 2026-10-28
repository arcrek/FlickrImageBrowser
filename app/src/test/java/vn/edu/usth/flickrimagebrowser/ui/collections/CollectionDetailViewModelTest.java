package vn.edu.usth.flickrimagebrowser.ui.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * Unit tests for CollectionDetailViewModel verifying photo observation and removal.
 */
public class CollectionDetailViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private PhotoRepository repository;

    private CollectionDetailViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new CollectionDetailViewModel(repository);
    }

    @Test
    public void getPhotos_withoutSettingCollectionId_returnsEmptyList() {
        viewModel.getPhotos().observeForever(photos -> {});
        List<Photo> photos = viewModel.getPhotos().getValue();
        assertNotNull(photos);
        assertTrue(photos.isEmpty());
    }

    @Test
    public void setCollectionId_observesPhotosInCollectionFromRepository() {
        long collectionId = 42L;
        MutableLiveData<List<Photo>> liveData = new MutableLiveData<>();
        Photo samplePhoto = new Photo(
                "p1", "Sunrise", "Author", "Desc",
                "thumbUrl", "detailUrl", false, 1000L, 0, 0
        );
        liveData.setValue(Collections.singletonList(samplePhoto));
        when(repository.observePhotosInCollection(collectionId)).thenReturn(liveData);

        viewModel.getPhotos().observeForever(photos -> {});
        viewModel.setCollectionId(collectionId);

        List<Photo> result = viewModel.getPhotos().getValue();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("p1", result.get(0).getId());
    }

    @Test
    public void removePhotoFromCollection_delegatesToRepository() {
        long collectionId = 15L;
        MutableLiveData<List<Photo>> liveData = new MutableLiveData<>(Collections.emptyList());
        when(repository.observePhotosInCollection(collectionId)).thenReturn(liveData);

        viewModel.getPhotos().observeForever(photos -> {});
        viewModel.setCollectionId(collectionId);

        Photo photoToRemove = new Photo(
                "p99", "Sea", "Author", "Desc",
                "thumbUrl", "detailUrl", false, 1000L, 0, 0
        );
        viewModel.removePhotoFromCollection(photoToRemove);

        verify(repository).removePhotoFromCollection(collectionId, "p99");
    }

    @Test
    public void removePhotosFromCollection_delegatesBatchToRepository() {
        long collectionId = 20L;
        viewModel.setCollectionId(collectionId);
        List<String> ids = Collections.singletonList("p55");

        viewModel.removePhotosFromCollection(ids);

        verify(repository).removePhotosFromCollection(collectionId, ids);
    }

    @Test
    public void addPhotosToCollection_delegatesBatchToRepository() {
        long collectionId = 25L;
        viewModel.setCollectionId(collectionId);
        Photo p = new Photo("p77", "Title", "Owner", "Desc", "u1", "u2", false, 100L, 0, 0);
        List<Photo> photos = Collections.singletonList(p);

        viewModel.addPhotosToCollection(photos);

        verify(repository).addPhotosToCollection(collectionId, photos);
    }
}
