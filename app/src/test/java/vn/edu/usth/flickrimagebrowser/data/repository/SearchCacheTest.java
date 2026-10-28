package vn.edu.usth.flickrimagebrowser.data.repository;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.usth.flickrimagebrowser.data.local.CollectionDao;
import vn.edu.usth.flickrimagebrowser.data.local.PhotoDao;
import vn.edu.usth.flickrimagebrowser.data.remote.PixabayApiService;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PixabayHitDto;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PixabaySearchResponse;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.AppExecutors;
import vn.edu.usth.flickrimagebrowser.util.Resource;

public class SearchCacheTest {
    @Rule public final InstantTaskExecutorRule rule = new InstantTaskExecutorRule();
    private final PixabayApiService api = mock(PixabayApiService.class);
    private final PhotoDao photos = mock(PhotoDao.class);
    private final List<Callback<PixabaySearchResponse>> callbacks = new ArrayList<>();
    private Call<PixabaySearchResponse> call;
    private PhotoRepositoryImpl repository;
    private final AtomicLong clock = new AtomicLong();

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        call = mock(Call.class);
        when(api.searchPhotos(anyString(), anyString(), anyString(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(call);
        when(api.getFeaturedPhotos(anyString(), anyBoolean(), anyString(), anyString(),
                anyBoolean(), anyInt(), anyInt())).thenReturn(call);
        doAnswer(invocation -> {
            callbacks.add(invocation.getArgument(0));
            return null;
        }).when(call).enqueue(any());
        repository = new PhotoRepositoryImpl(api, photos, mock(CollectionDao.class),
                new AppExecutors(Runnable::run, Runnable::run, Runnable::run), "test-key", clock::get);
    }

    @Test
    public void repeatPage_reusesResponseAndReadsCurrentFavorites() {
        repository.searchPhotos(" cat ", 1, 30);
        succeed(0, 42);
        repository.searchPhotos("dog", 1, 30);
        succeed(1, 43);
        when(photos.getFavoriteIdsSync()).thenReturn(Collections.singletonList("42"));
        Resource<List<Photo>> cached = repository.searchPhotos("cat", 1, 30).getValue();
        assertEquals("42", cached.getData().get(0).getId());
        assertTrue(cached.getData().get(0).isFavorite());
        cached.getData().clear();
        when(photos.getFavoriteIdsSync()).thenReturn(Collections.emptyList());
        assertFalse(repository.searchPhotos("cat", 1, 30).getValue().getData().get(0).isFavorite());
        assertEquals(2, callbacks.size());
    }

    @Test
    public void pendingIdenticalRequests_shareOneApiCall() {
        LiveData<Resource<List<Photo>>> first = repository.searchPhotos("cat", 1, 30);
        assertSame(first, repository.searchPhotos("cat", 1, 30));
        assertEquals(1, callbacks.size());
        succeed(0, 42);
        assertEquals(Resource.Status.SUCCESS, first.getValue().getStatus());
    }

    @Test
    public void cacheKeys_useEffectivePageAndSizeAndIncludeFeatured() {
        repository.searchPhotos("", 0, 1);
        succeed(0, 1);
        repository.searchPhotos("   ", 1, 3);
        assertEquals(1, callbacks.size());
        repository.searchPhotos("", 2, 3);
        succeed(1, 2);
        repository.searchPhotos("", 1, 30);
        succeed(2, 3);
        assertEquals(3, callbacks.size());
        assertEquals("1", repository.searchPhotos("", 1, 3).getValue().getData().get(0).getId());
    }

    @Test
    public void refresh_invalidatesOnlyThatQueryAndRejectsLatePages() {
        repository.searchPhotos("cat", 1, 30);
        succeed(0, 1);
        repository.searchPhotos("cat", 2, 30);
        succeed(1, 2);
        repository.searchPhotos("dog", 1, 30);
        succeed(2, 3);
        repository.searchPhotos("cat", 3, 30); // Remains in flight during refresh.
        LiveData<Resource<List<Photo>>> refreshed = repository.searchPhotos("cat", 1, 30, true);
        assertSame(refreshed, repository.searchPhotos("cat", 1, 30));
        succeed(4, 10);
        succeed(3, 30); // Must not repopulate the refreshed query's cache.
        assertEquals("10", repository.searchPhotos("cat", 1, 30).getValue().getData().get(0).getId());
        repository.searchPhotos("dog", 1, 30);
        assertEquals(5, callbacks.size());
        repository.searchPhotos("cat", 2, 30);
        repository.searchPhotos("cat", 3, 30);
        assertEquals(7, callbacks.size());
    }

    @Test
    public void lateFirstPage_cannotReplaceRefreshedResponse() {
        repository.searchPhotos("cat", 1, 30);
        repository.searchPhotos("cat", 1, 30, true);
        succeed(1, 20);
        succeed(0, 10);
        assertEquals("20", repository.searchPhotos("cat", 1, 30).getValue().getData().get(0).getId());
        assertEquals(2, callbacks.size());
    }

    @Test
    public void failedRefresh_keepsCachedPagesAndCanBeRetried() {
        repository.searchPhotos("cat", 1, 30);
        succeed(0, 1);
        repository.searchPhotos("cat", 2, 30);
        succeed(1, 2);
        LiveData<Resource<List<Photo>>> refreshed = repository.searchPhotos("cat", 1, 30, true);
        callbacks.get(2).onFailure(call, new IOException("Offline"));
        assertEquals(Resource.Status.ERROR, refreshed.getValue().getStatus());
        assertEquals("1", repository.searchPhotos("cat", 1, 30).getValue().getData().get(0).getId());
        assertEquals("2", repository.searchPhotos("cat", 2, 30).getValue().getData().get(0).getId());
        assertEquals(3, callbacks.size());
        repository.searchPhotos("cat", 1, 30, true);
        succeed(3, 10);
        assertEquals("10", repository.searchPhotos("cat", 1, 30).getValue().getData().get(0).getId());
    }

    @Test
    public void favoriteReadFailure_duringNormalFetchDoesNotRequireAnotherApiCall() {
        when(photos.getFavoriteIdsSync()).thenThrow(new IllegalStateException("disk unavailable"));
        LiveData<Resource<List<Photo>>> first = repository.searchPhotos("cat", 1, 30);
        succeed(0, 42);
        assertEquals(Resource.Status.ERROR, first.getValue().getStatus());
        doReturn(Collections.singletonList("42")).when(photos).getFavoriteIdsSync();
        assertTrue(repository.searchPhotos("cat", 1, 30).getValue().getData().get(0).isFavorite());
        assertEquals(1, callbacks.size());
    }

    @Test
    public void favoriteReadFailure_duringRefreshPreservesPreviousCache() {
        repository.searchPhotos("cat", 1, 30);
        succeed(0, 1);
        repository.searchPhotos("cat", 2, 30);
        succeed(1, 2);
        when(photos.getFavoriteIdsSync()).thenThrow(new IllegalStateException("disk unavailable"));
        LiveData<Resource<List<Photo>>> refreshed = repository.searchPhotos("cat", 1, 30, true);
        succeed(2, 10);
        assertEquals(Resource.Status.ERROR, refreshed.getValue().getStatus());
        doReturn(Collections.emptyList()).when(photos).getFavoriteIdsSync();
        assertEquals("1", repository.searchPhotos("cat", 1, 30).getValue().getData().get(0).getId());
        assertEquals("2", repository.searchPhotos("cat", 2, 30).getValue().getData().get(0).getId());
        assertEquals(3, callbacks.size());
    }

    @Test
    public void emptyResponses_areCachedButHttpAndNetworkErrorsAreNot() {
        LiveData<Resource<List<Photo>>> first = repository.searchPhotos("none", 1, 30);
        callbacks.get(0).onResponse(call, Response.error(429,
                ResponseBody.create("Limit", MediaType.parse("text/plain"))));
        assertEquals(Resource.Status.ERROR, first.getValue().getStatus());
        repository.searchPhotos("none", 1, 30);
        callbacks.get(1).onFailure(call, new IOException("Offline"));
        repository.searchPhotos("none", 1, 30);
        callbacks.get(2).onResponse(call, Response.success(new PixabaySearchResponse(
                0, 0, Collections.emptyList())));
        assertEquals(Resource.Status.EMPTY, repository.searchPhotos("none", 1, 30).getValue().getStatus());
        assertEquals(3, callbacks.size());
    }

    @Test
    public void ttl_expiresAtThirtyMinutesAndCacheReadsDoNotExtendIt() {
        repository.searchPhotos("cat", 1, 30);
        succeed(0, 1);
        clock.set(TimeUnit.MINUTES.toMillis(30) - 1);
        assertEquals("1", repository.searchPhotos("cat", 1, 30).getValue().getData().get(0).getId());
        assertEquals(1, callbacks.size());
        clock.incrementAndGet();
        LiveData<Resource<List<Photo>>> expired = repository.searchPhotos("cat", 1, 30);
        assertEquals(Resource.Status.LOADING, expired.getValue().getStatus());
        assertSame(expired, repository.searchPhotos("cat", 1, 30));
        assertEquals(2, callbacks.size());
        succeed(1, 2);
        clock.addAndGet(TimeUnit.MINUTES.toMillis(30) - 1);
        assertEquals("2", repository.searchPhotos("cat", 1, 30).getValue().getData().get(0).getId());
        assertEquals(2, callbacks.size());
    }

    @Test
    public void ttl_startsWhenResponseArrivesAndEachPageExpiresIndependently() {
        repository.searchPhotos("cat", 1, 30);
        clock.set(TimeUnit.MINUTES.toMillis(10));
        succeed(0, 1);
        repository.searchPhotos("cat", 2, 30);
        clock.set(TimeUnit.MINUTES.toMillis(20));
        succeed(1, 2);
        clock.set(TimeUnit.MINUTES.toMillis(40));
        repository.searchPhotos("cat", 1, 30);
        assertEquals(3, callbacks.size());
        assertEquals("2", repository.searchPhotos("cat", 2, 30).getValue().getData().get(0).getId());
        assertEquals(3, callbacks.size());
    }

    @Test
    public void ttl_appliesToEmptyFeaturedPagesAndFailedFetchDoesNotRenewIt() {
        repository.searchPhotos("", 1, 30);
        callbacks.get(0).onResponse(call, Response.success(new PixabaySearchResponse(
                0, 0, Collections.emptyList())));
        clock.set(TimeUnit.MINUTES.toMillis(30));
        repository.searchPhotos("", 1, 30);
        assertEquals(2, callbacks.size());
        callbacks.get(1).onFailure(call, new IOException("Offline"));
        repository.searchPhotos("", 1, 30);
        assertEquals(3, callbacks.size());
    }

    @Test
    public void ttl_failedManualRefreshDoesNotExtendPreviousCacheLifetime() {
        repository.searchPhotos("cat", 1, 30);
        succeed(0, 1);
        clock.set(TimeUnit.MINUTES.toMillis(29));
        repository.searchPhotos("cat", 1, 30, true);
        callbacks.get(1).onFailure(call, new IOException("Offline"));
        assertEquals("1", repository.searchPhotos("cat", 1, 30).getValue().getData().get(0).getId());
        clock.set(TimeUnit.MINUTES.toMillis(30));
        repository.searchPhotos("cat", 1, 30);
        assertEquals(3, callbacks.size());
    }

    private void succeed(int index, long id) {
        PixabayHitDto hit = new PixabayHitDto(id, "tags", "Author", "page", "preview", "thumb", "detail");
        callbacks.get(index).onResponse(call, Response.success(new PixabaySearchResponse(
                1, 1, Collections.singletonList(hit))));
    }
}
