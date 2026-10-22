package vn.edu.usth.flickrimagebrowser.ui.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Unit tests for SearchViewModel verifying search triggering, deduplication across
 * configuration changes, multi-page pagination accumulation, and failure recovery.
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

    @Test
    public void loadNextPage_accumulatesPhotosAcrossPages() {
        List<Photo> page1List = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            page1List.add(new Photo(
                    String.valueOf(i), "Title " + i, "Author", "",
                    "https://example.com/" + i + "_q.jpg", "https://example.com/" + i + "_z.jpg",
                    false, 0L
            ));
        }
        MutableLiveData<Resource<List<Photo>>> page1Live = new MutableLiveData<>(Resource.success(page1List));
        when(repository.searchPhotos("hanoi", 1, 30)).thenReturn(page1Live);

        viewModel.getSearchResults().observeForever(observer);
        viewModel.submitQuery("hanoi", false);

        assertEquals(1, viewModel.getCurrentPage());
        assertTrue(viewModel.hasMorePages());
        assertEquals(30, viewModel.getSearchResults().getValue().getData().size());

        List<Photo> page2List = new ArrayList<>();
        for (int i = 31; i <= 40; i++) {
            page2List.add(new Photo(
                    String.valueOf(i), "Title " + i, "Author", "",
                    "https://example.com/" + i + "_q.jpg", "https://example.com/" + i + "_z.jpg",
                    false, 0L
            ));
        }
        MutableLiveData<Resource<List<Photo>>> page2Live = new MutableLiveData<>(Resource.success(page2List));
        when(repository.searchPhotos("hanoi", 2, 30)).thenReturn(page2Live);

        viewModel.loadNextPage();

        assertEquals(2, viewModel.getCurrentPage());
        assertFalse(viewModel.hasMorePages()); // 10 < 30 items means end of feed
        Resource<List<Photo>> combinedResource = viewModel.getSearchResults().getValue();
        assertNotNull(combinedResource);
        assertEquals(Resource.Status.SUCCESS, combinedResource.getStatus());
        assertNotNull(combinedResource.getData());
        assertEquals(40, combinedResource.getData().size());
        assertEquals("1", combinedResource.getData().get(0).getId());
        assertEquals("40", combinedResource.getData().get(39).getId());

        verify(repository, times(1)).searchPhotos("hanoi", 2, 30);
    }

    @Test
    public void loadNextPage_doesNotTriggerWhenEndReached() {
        List<Photo> smallPage = Collections.singletonList(new Photo(
                "1", "Solo", "Author", "",
                "https://example.com/1_q.jpg", "https://example.com/1_z.jpg",
                false, 0L
        ));
        MutableLiveData<Resource<List<Photo>>> page1Live = new MutableLiveData<>(Resource.success(smallPage));
        when(repository.searchPhotos("small", 1, 30)).thenReturn(page1Live);

        viewModel.getSearchResults().observeForever(observer);
        viewModel.submitQuery("small", false);

        assertFalse(viewModel.hasMorePages());
        viewModel.loadNextPage();

        verify(repository, never()).searchPhotos("small", 2, 30);
    }

    @Test
    public void loadNextPage_onError_preservesExistingPhotos() {
        List<Photo> page1List = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            page1List.add(new Photo(
                    String.valueOf(i), "Title " + i, "Author", "",
                    "https://example.com/" + i + "_q.jpg", "https://example.com/" + i + "_z.jpg",
                    false, 0L
            ));
        }
        MutableLiveData<Resource<List<Photo>>> page1Live = new MutableLiveData<>(Resource.success(page1List));
        when(repository.searchPhotos("city", 1, 30)).thenReturn(page1Live);

        viewModel.getSearchResults().observeForever(observer);
        viewModel.submitQuery("city", false);

        MutableLiveData<Resource<List<Photo>>> errorLive = new MutableLiveData<>(
                Resource.error("Network timeout", null)
        );
        when(repository.searchPhotos("city", 2, 30)).thenReturn(errorLive);

        viewModel.loadNextPage();

        Resource<List<Photo>> current = viewModel.getSearchResults().getValue();
        assertNotNull(current);
        assertEquals(Resource.Status.ERROR, current.getStatus());
        assertEquals("Network timeout", current.getMessage());
        assertNotNull(current.getData());
        assertEquals(30, current.getData().size()); // Existing page 1 photos retained!
        assertEquals(1, viewModel.getCurrentPage());
        assertFalse(viewModel.isLoadingNextPage());
    }

    @Test
    public void submitQuery_resetsPaginationState() {
        List<Photo> page1List = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            page1List.add(new Photo(
                    String.valueOf(i), "Title " + i, "Author", "",
                    "https://example.com/" + i + "_q.jpg", "https://example.com/" + i + "_z.jpg",
                    false, 0L
            ));
        }
        MutableLiveData<Resource<List<Photo>>> page1Live = new MutableLiveData<>(Resource.success(page1List));
        when(repository.searchPhotos("hanoi", 1, 30)).thenReturn(page1Live);

        viewModel.getSearchResults().observeForever(observer);
        viewModel.submitQuery("hanoi", false);

        MutableLiveData<Resource<List<Photo>>> newQueryLive = new MutableLiveData<>(
                Resource.success(Collections.singletonList(new Photo(
                        "99", "Saigon River", "Author", "",
                        "https://example.com/99_q.jpg", "https://example.com/99_z.jpg",
                        false, 0L
                )))
        );
        when(repository.searchPhotos("saigon", 1, 30)).thenReturn(newQueryLive);

        viewModel.submitQuery("saigon", false);

        assertEquals("saigon", viewModel.getLastQuery());
        assertEquals(1, viewModel.getCurrentPage());
        Resource<List<Photo>> result = viewModel.getSearchResults().getValue();
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals("99", result.getData().get(0).getId());
    }
}
