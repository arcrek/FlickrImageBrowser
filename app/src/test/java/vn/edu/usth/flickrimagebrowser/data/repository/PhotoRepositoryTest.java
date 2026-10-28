package vn.edu.usth.flickrimagebrowser.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.database.sqlite.SQLiteConstraintException;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import vn.edu.usth.flickrimagebrowser.data.local.CollectionDao;
import vn.edu.usth.flickrimagebrowser.data.local.PhotoDao;
import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionEntity;
import vn.edu.usth.flickrimagebrowser.data.local.entity.PhotoEntity;
import vn.edu.usth.flickrimagebrowser.data.remote.PixabayApiService;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PixabayHitDto;
import vn.edu.usth.flickrimagebrowser.data.remote.dto.PixabaySearchResponse;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.AppExecutors;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Unit tests for PhotoRepositoryImpl using mocked PixabayApiService and Room DAOs.
 */
public class PhotoRepositoryTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private PixabayApiService apiService;

    @Mock
    private PhotoDao photoDao;

    @Mock
    private CollectionDao collectionDao;

    @Mock
    private Call<PixabaySearchResponse> mockCall;

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
                "test_valid_pixabay_key"
        );
    }

    @Test
    public void demoPhotos_useQueryIndependentUrlsAndKeepLocalIdentity() {
        PhotoRepositoryImpl demo = new PhotoRepositoryImpl(
                apiService, photoDao, collectionDao, directExecutors, "demo");
        Photo first = demo.searchPhotos("medical condition", 1, 3).getValue().getData().get(0);
        Photo otherQuery = demo.searchPhotos("mountains", 1, 3).getValue().getData().get(0);
        Photo nextPage = demo.searchPhotos("medical condition", 2, 3).getValue().getData().get(0);
        assertEquals("demo_medical_condition_1", first.getId());
        assertEquals(first.getThumbnailUrl(), otherQuery.getThumbnailUrl());
        assertEquals(first.getDetailUrl(), otherQuery.getDetailUrl());
        org.junit.Assert.assertNotEquals(first.getThumbnailUrl(), nextPage.getThumbnailUrl());
        org.junit.Assert.assertFalse(first.getThumbnailUrl().contains("medical"));
        org.junit.Assert.assertFalse(first.getDetailUrl().contains("condition"));
    }

    @Test
    public void collectionWrites_reportSuccess() {
        when(collectionDao.renameCollection(5L, "New Name")).thenReturn(1);
        assertEquals(Resource.Status.SUCCESS, repository.createCollectionResult("Name", "").getValue().getStatus());
        assertEquals(Resource.Status.SUCCESS, repository.renameCollectionResult(5L, "New Name").getValue().getStatus());
    }

    @Test
    public void collectionWrites_reportStorageFailures() {
        Mockito.doThrow(new IllegalStateException("disk full"))
                .when(collectionDao).insertCollection(ArgumentMatchers.any());
        when(collectionDao.renameCollection(5L, "Name")).thenThrow(new IllegalStateException("disk full"));
        for (Resource<Boolean> result : Arrays.asList(
                repository.createCollectionResult("Name", "").getValue(),
                repository.renameCollectionResult(5L, "Name").getValue())) {
            assertEquals(Resource.Status.ERROR, result.getStatus());
            assertEquals("Unable to save collection. Please try again.", result.getMessage());
        }
    }

    @Test
    public void collectionWrites_reportReadFailures() {
        when(collectionDao.getCollectionsSync()).thenThrow(new IllegalStateException("corrupt"));
        assertEquals(Resource.Status.ERROR, repository.createCollectionResult("Name", "").getValue().getStatus());
        assertEquals(Resource.Status.ERROR, repository.renameCollectionResult(5L, "Name").getValue().getStatus());
    }

    @Test
    public void collectionWrites_distinguishUniqueNameFromOtherConstraints() {
        SQLiteConstraintException unique = Mockito.mock(
                SQLiteConstraintException.class);
        when(unique.getMessage()).thenReturn("UNIQUE constraint failed: collections.name (code 2067)");
        SQLiteConstraintException other = Mockito.mock(
                SQLiteConstraintException.class);
        when(other.getMessage()).thenReturn("NOT NULL constraint failed: collections.description");
        Mockito.doThrow(unique).doThrow(other)
                .when(collectionDao).insertCollection(ArgumentMatchers.any());
        when(collectionDao.renameCollection(5L, "Name")).thenThrow(unique).thenThrow(other);
        assertEquals("A collection with this name already exists.",
                repository.createCollectionResult("Name", "").getValue().getMessage());
        assertEquals("A collection with this name already exists.",
                repository.renameCollectionResult(5L, "Name").getValue().getMessage());
        assertEquals("Unable to save collection. Please try again.",
                repository.createCollectionResult("Name", "").getValue().getMessage());
        assertEquals("Unable to save collection. Please try again.",
                repository.renameCollectionResult(5L, "Name").getValue().getMessage());
    }

    @Test
    public void collectionWrites_duplicatePrecheckAndMissingRename_areErrors() {
        assertEquals(Resource.Status.ERROR, repository.renameCollectionResult(5L, "Name").getValue().getStatus());
        when(collectionDao.getCollectionsSync()).thenReturn(Collections.singletonList(
                new CollectionEntity(2L, "Name", "", 0L)));
        assertEquals("A collection with this name already exists.",
                repository.createCollectionResult(" name ", "").getValue().getMessage());
        assertEquals("A collection with this name already exists.",
                repository.renameCollectionResult(5L, " name ").getValue().getMessage());
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
        assertTrue(resource.getMessage().contains("PIXABAY_API_KEY"));
        verify(apiService, never()).searchPhotos(
                anyString(), anyString(), anyString(), anyBoolean(), anyInt(), anyInt()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void searchPhotos_whenPixabayResponseOk_emitsSuccessWithMergedFavoriteFlag() {
        PixabayHitDto dto = new PixabayHitDto(
                42L,
                "hanoi, street, vietnam",
                "photographer",
                "https://pixabay.com/photos/42/",
                "https://cdn.pixabay.com/photo/42_150.jpg",
                "https://cdn.pixabay.com/photo/42_640.jpg",
                "https://cdn.pixabay.com/photo/42_1280.jpg"
        );
        PixabaySearchResponse apiResponse = new PixabaySearchResponse(
                100, 100, Collections.singletonList(dto)
        );

        when(photoDao.getFavoriteIdsSync()).thenReturn(Collections.singletonList("42"));
        when(apiService.searchPhotos(
                eq("test_valid_pixabay_key"),
                eq("hanoi"),
                eq(PhotoRepositoryImpl.DEFAULT_IMAGE_TYPE),
                eq(PhotoRepositoryImpl.DEFAULT_SAFE_SEARCH),
                eq(1),
                eq(20)
        )).thenReturn(mockCall);

        doAnswer(invocation -> {
            Callback<PixabaySearchResponse> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(apiResponse));
            return null;
        }).when(mockCall).enqueue(ArgumentMatchers.any(Callback.class));

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
    public void searchPhotos_withEmptyQuery_callsGetFeaturedPhotosWithEditorsChoiceAndPopularOrder() {
        PixabayHitDto dto = new PixabayHitDto(
                108L,
                "featured, mountain, landscape",
                "featured_author",
                "https://pixabay.com/photos/108/",
                "https://cdn.pixabay.com/photo/108_150.jpg",
                "https://cdn.pixabay.com/photo/108_640.jpg",
                "https://cdn.pixabay.com/photo/108_1280.jpg"
        );
        PixabaySearchResponse apiResponse = new PixabaySearchResponse(
                50, 50, Collections.singletonList(dto)
        );

        when(photoDao.getFavoriteIdsSync()).thenReturn(Collections.emptyList());
        when(apiService.getFeaturedPhotos(
                eq("test_valid_pixabay_key"),
                eq(true),
                eq(PhotoRepositoryImpl.DEFAULT_FEATURED_ORDER),
                eq(PhotoRepositoryImpl.DEFAULT_IMAGE_TYPE),
                eq(PhotoRepositoryImpl.DEFAULT_SAFE_SEARCH),
                eq(1),
                eq(30)
        )).thenReturn(mockCall);

        doAnswer(invocation -> {
            Callback<PixabaySearchResponse> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(apiResponse));
            return null;
        }).when(mockCall).enqueue(ArgumentMatchers.any(Callback.class));

        LiveData<Resource<List<Photo>>> resultLive = repository.searchPhotos("   ", 1, 30);
        Resource<List<Photo>> resource = resultLive.getValue();

        assertNotNull(resource);
        assertEquals(Resource.Status.SUCCESS, resource.getStatus());
        assertNotNull(resource.getData());
        assertEquals(1, resource.getData().size());
        assertEquals("108", resource.getData().get(0).getId());
        verify(apiService).getFeaturedPhotos(
                "test_valid_pixabay_key",
                true,
                PhotoRepositoryImpl.DEFAULT_FEATURED_ORDER,
                PhotoRepositoryImpl.DEFAULT_IMAGE_TYPE,
                PhotoRepositoryImpl.DEFAULT_SAFE_SEARCH,
                1,
                30
        );
        verify(apiService, never()).searchPhotos(
                anyString(), anyString(), anyString(), anyBoolean(), anyInt(), anyInt()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void searchPhotos_whenHttp400Or401InvalidKey_emitsApiKeyError() {
        when(apiService.searchPhotos(
                anyString(), anyString(), anyString(), anyBoolean(), anyInt(), anyInt()
        )).thenReturn(mockCall);

        doAnswer(invocation -> {
            Callback<PixabaySearchResponse> callback = invocation.getArgument(0);
            Response<PixabaySearchResponse> unauthorizedResponse = Response.error(
                    400,
                    ResponseBody.create("[ERROR 400] \"key\" is missing", MediaType.parse("text/plain"))
            );
            callback.onResponse(mockCall, unauthorizedResponse);
            return null;
        }).when(mockCall).enqueue(ArgumentMatchers.any(Callback.class));

        LiveData<Resource<List<Photo>>> resultLive = repository.searchPhotos("nature", 1, 20);
        Resource<List<Photo>> resource = resultLive.getValue();

        assertNotNull(resource);
        assertEquals(Resource.Status.ERROR, resource.getStatus());
        assertNotNull(resource.getMessage());
        assertTrue(resource.getMessage().contains("400"));
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

    @Test
    public void deleteCollection_deletesFromCollectionDaoAndCleansUpAllOrphans() {
        repository.deleteCollection(10L);
        verify(collectionDao).deleteCollectionById(10L);
        verify(photoDao).deleteAllOrphanPhotos();
    }

    @Test
    public void renameCollection_whenUnique_updatesNameInDao() {
        when(collectionDao.getCollectionsSync()).thenReturn(Collections.emptyList());
        repository.renameCollection(5L, "New Name");
        verify(collectionDao).renameCollection(5L, "New Name");
    }

    @Test
    public void renameCollection_whenDuplicateCaseInsensitive_doesNotCallDao() {
        CollectionEntity existing =
                new CollectionEntity(10L, "Nature", "desc", 100L);
        when(collectionDao.getCollectionsSync()).thenReturn(Collections.singletonList(existing));
        repository.renameCollection(5L, "  nature  ");
        verify(collectionDao, never()).renameCollection(ArgumentMatchers.anyLong(), anyString());
    }

    @Test
    public void removePhotoFromCollection_removesFromCollectionDaoAndCleansUpOrphan() {
        repository.removePhotoFromCollection(10L, "photo_123");
        verify(collectionDao).removePhotoFromCollection(10L, "photo_123");
        verify(photoDao).deleteOrphanIfUnreferenced("photo_123");
    }

    @Test
    public void observePhotosInCollection_mapsEntitiesFromRelation() {
        vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionWithPhotos relation =
                new vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionWithPhotos();
        relation.collection = new CollectionEntity(1L, "Travel", "", 0L);
        relation.photos = Collections.singletonList(
                new PhotoEntity("p1", "Beach", "Author", "Desc", "q", "z", false, 100L)
        );
        androidx.lifecycle.MutableLiveData<vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionWithPhotos> liveData =
                new androidx.lifecycle.MutableLiveData<>(relation);
        when(collectionDao.observeCollectionWithPhotosById(1L)).thenReturn(liveData);

        LiveData<List<Photo>> resultLive = repository.observePhotosInCollection(1L);
        resultLive.observeForever(photos -> {});
        List<Photo> result = resultLive.getValue();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("p1", result.get(0).getId());
    }
}