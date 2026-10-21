package vn.edu.usth.flickrimagebrowser.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.usth.flickrimagebrowser.data.local.CollectionDao;
import vn.edu.usth.flickrimagebrowser.data.local.PhotoDao;
import vn.edu.usth.flickrimagebrowser.data.local.entity.PhotoEntity;
import vn.edu.usth.flickrimagebrowser.data.remote.PexelsApiService;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PexelsPhotoDto;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PexelsPhotoSrcDto;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PexelsSearchResponse;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.AppExecutors;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Unit tests for PhotoRepositoryImpl using mocked PexelsApiService and Room DAOs.
 */
public class PhotoRepositoryTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private PexelsApiService apiService;

    @Mock
    private PhotoDao photoDao;

    @Mock
    private CollectionDao collectionDao;

    @Mock
    private Call<PexelsSearchResponse> mockCall;

    private AppExecutors directExecutors;
    private PhotoRepositoryImpl repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        directExecutors = new AppExecutors(Runnable::run, Runnable::run, Runnable::run);
        repository = new PhotoRepositoryImpl(
                apiService,
                photoDao,
                collectionDao,
                directExecutors,
                "test_valid_pexels_key"
        );
    }

    @Test
    public void searchPhotos_withMissingApiKey_returnsErrorWithoutNetworkCall() {
        PhotoRepositoryImpl unconfiguredRepo = new PhotoRepositoryImpl(
                apiService,
                photoDao,
                collectionDao,
                directExecutors,
                ""
        );

        LiveData<Resource<List<Photo>>> liveData = unconfiguredRepo.searchPhotos("vietnam", 1, 20);
        Resource<List<Photo>> resource = liveData.getValue();

        assertNotNull(resource);
        assertEquals(Resource.Status.ERROR, resource.getStatus());
        assertNotNull(resource.getMessage());
        assertTrue(resource.getMessage().contains("PEXELS_API_KEY"));
        verify(apiService, never()).searchPhotos(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void searchPhotos_whenPexelsResponseOk_emitsSuccessWithMergedFavoriteFlag() {
        PexelsPhotoSrcDto src = new PexelsPhotoSrcDto(
                "https://images.pexels.com/42/tiny.jpg",
                null,
                null,
                "https://images.pexels.com/42/large.jpg",
                null,
                null
        );
        PexelsPhotoDto dto = new PexelsPhotoDto(
                42L,
                "Hanoi Street",
                "photographer",
                "https://www.pexels.com/photo/42/",
                src
        );
        PexelsSearchResponse apiResponse = new PexelsSearchResponse(
                1, 20, 100, Collections.singletonList(dto), null
        );

        when(photoDao.getFavoriteIdsSync()).thenReturn(Collections.singletonList("42"));
        when(apiService.searchPhotos(
                eq("test_valid_pexels_key"), eq("hanoi"), eq(1), eq(20)
        )).thenReturn(mockCall);

        doAnswer(invocation -> {
            Callback<PexelsSearchResponse> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(apiResponse));
            return null;
        }).when(mockCall).enqueue(org.mockito.ArgumentMatchers.any(Callback.class));

        LiveData<Resource<List<Photo>>> resultLive = repository.searchPhotos("hanoi", 1, 20);
        Resource<List<Photo>> resource = resultLive.getValue();

        assertNotNull(resource);
        assertEquals(Resource.Status.SUCCESS, resource.getStatus());
        assertNotNull(resource.getData());
        assertEquals(1, resource.getData().size());
        assertEquals("42", resource.getData().get(0).getId());
        assertTrue(resource.getData().get(0).isFavorite());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void searchPhotos_whenHttp401Unauthorized_emitsApiKeyError() {
        when(apiService.searchPhotos(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);

        doAnswer(invocation -> {
            Callback<PexelsSearchResponse> callback = invocation.getArgument(0);
            Response<PexelsSearchResponse> unauthorizedResponse = Response.error(
                    401,
                    ResponseBody.create("{\"error\":\"Invalid API Key\"}", MediaType.parse("application/json"))
            );
            callback.onResponse(mockCall, unauthorizedResponse);
            return null;
        }).when(mockCall).enqueue(org.mockito.ArgumentMatchers.any(Callback.class));

        LiveData<Resource<List<Photo>>> resultLive = repository.searchPhotos("nature", 1, 20);
        Resource<List<Photo>> resource = resultLive.getValue();

        assertNotNull(resource);
        assertEquals(Resource.Status.ERROR, resource.getStatus());
        assertNotNull(resource.getMessage());
        assertTrue(resource.getMessage().contains("401"));
    }

    @Test
    public void toggleFavorite_whenNotYetSaved_upsertsFavoritePhotoInDao() {
        Photo photo = new Photo(
                "900", "Lotus Pond", "Author", "Desc",
                "https://example.com/q.jpg", "https://example.com/z.jpg",
                false, 0L
        );
        when(photoDao.getPhotoByIdSync("900")).thenReturn(null);

        repository.toggleFavorite(photo);

        ArgumentCaptor<PhotoEntity> captor = ArgumentCaptor.forClass(PhotoEntity.class);
        verify(photoDao).upsertPhoto(captor.capture());
        PhotoEntity saved = captor.getValue();
        assertEquals("900", saved.getId());
        assertTrue(saved.isFavorite());
        verify(photoDao, never()).deleteOrphanIfUnreferenced("900");
    }
}
