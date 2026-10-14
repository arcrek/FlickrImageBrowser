package vn.edu.usth.flickrimagebrowser.ui.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
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
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Unit tests for SearchViewModel verifying search triggering, deduplication across
 * configuration changes, and retry behavior.
 */
public class SearchViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private PhotoRepository repository;

    @Mock
    private Observer<Resource<List<Photo>>> observer;

    private SearchViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new SearchViewModel(repository);
    }

    @Test
    public void submitQuery_triggersRepositoryAndEmitsResults() {
        Photo sample = new Photo(
                "1", "Bridge", "Author", "",
                "https://example.com/1_q.jpg", "https://example.com/1_z.jpg",
                false, 0L
        );
        MutableLiveData<Resource<List<Photo>>> repoLive = new MutableLiveData<>();
        repoLive.setValue(Resource.success(Collections.singletonList(sample)));

        when(repository.searchPhotos("danang", SearchViewModel.DEFAULT_PAGE, SearchViewModel.DEFAULT_PER_PAGE))
                .thenReturn(repoLive);

        viewModel.getSearchResults().observeForever(observer);
        viewModel.submitQuery("  danang  ", false);

        assertEquals("danang", viewModel.getLastQuery());
        Resource<List<Photo>> current = viewModel.getSearchResults().getValue();
        assertNotNull(current);
        assertEquals(Resource.Status.SUCCESS, current.getStatus());
        assertNotNull(current.getData());
        assertEquals(1, current.getData().size());
        verify(repository, times(1)).searchPhotos(
                "danang", SearchViewModel.DEFAULT_PAGE, SearchViewModel.DEFAULT_PER_PAGE
        );
    }

    @Test
    public void submitQuery_doesNotRepeatIdenticalRequestUnlessForced() {
        MutableLiveData<Resource<List<Photo>>> repoLive = new MutableLiveData<>(Resource.empty());
        when(repository.searchPhotos("mountains", SearchViewModel.DEFAULT_PAGE, SearchViewModel.DEFAULT_PER_PAGE))
                .thenReturn(repoLive);

        viewModel.getSearchResults().observeForever(observer);
        viewModel.submitQuery("mountains", false);
        viewModel.submitQuery("mountains", false);

        verify(repository, times(1)).searchPhotos(
                "mountains", SearchViewModel.DEFAULT_PAGE, SearchViewModel.DEFAULT_PER_PAGE
        );

        viewModel.retryLastQuery();
        verify(repository, times(2)).searchPhotos(
                "mountains", SearchViewModel.DEFAULT_PAGE, SearchViewModel.DEFAULT_PER_PAGE
        );
    }
}
