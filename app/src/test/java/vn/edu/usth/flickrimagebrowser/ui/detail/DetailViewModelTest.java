package vn.edu.usth.flickrimagebrowser.ui.detail;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

public class DetailViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private PhotoRepository repository;

    private DetailViewModel viewModel;
    private Photo photo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(repository.observeCollections()).thenReturn(new MutableLiveData<>(Collections.emptyList()));
        when(repository.observeIsFavorite("photo-1")).thenReturn(new MutableLiveData<>(false));
        viewModel = new DetailViewModel(repository);
        photo = new Photo("photo-1", "Title", "Owner", "Description",
                "https://example.com/thumb.jpg", "https://example.com/detail.jpg", false, 0L);
        viewModel.setPhoto(photo);
    }

    @Test
    public void addPhotoToCollection_delegatesCurrentPhotoToRepository() {
        viewModel.addPhotoToCollection(42L);

        verify(repository).addPhotoToCollection(42L, photo);
    }

    @Test
    public void addPhotoToInvalidCollection_doesNothing() {
        viewModel.addPhotoToCollection(-1L);

        verify(repository, never()).addPhotoToCollection(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(Photo.class));
    }
}
