package vn.edu.usth.flickrimagebrowser.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static vn.edu.usth.flickrimagebrowser.data.local.LiveDataTestUtil.awaitValue;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionEntity;
import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionPhotoCrossRef;
import vn.edu.usth.flickrimagebrowser.data.local.entity.PhotoEntity;

/**
 * Room DAO instrumentation test verifying favorite persistence and collection relationships.
 */
@RunWith(AndroidJUnit4.class)
public class PhotoDaoTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private PhotoDao photoDao;
    private CollectionDao collectionDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        photoDao = database.photoDao();
        collectionDao = database.collectionDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void upsertAndUnfavoritePhoto_persistsAndCleansUpUnreferencedOrphan() {
        PhotoEntity entity = new PhotoEntity(
                "p100",
                "Mekong Delta",
                "usth_user",
                "Floating market at sunrise",
                "https://example.com/p100_q.jpg",
                "https://example.com/p100_z.jpg",
                true,
                1000L
        );

        photoDao.upsertPhoto(entity);

        List<PhotoEntity> favorites = photoDao.getFavoritePhotosSync();
        assertEquals(1, favorites.size());
        assertEquals("p100", favorites.get(0).getId());
        assertTrue(favorites.get(0).isFavorite());

        photoDao.updateFavoriteState("p100", false, 2000L);
        photoDao.deleteOrphanIfUnreferenced("p100");

        assertTrue(photoDao.getFavoritePhotosSync().isEmpty());
        assertNull(photoDao.getPhotoByIdSync("p100"));
    }

    @Test
    public void photoReferencedInCollection_isRetainedWhenUnfavorited() {
        PhotoEntity entity = new PhotoEntity(
                "p200",
                "Imperial Citadel",
                "hue_author",
                "Historic gate",
                "https://example.com/p200_q.jpg",
                "https://example.com/p200_z.jpg",
                true,
                1500L
        );
        photoDao.upsertPhoto(entity);

        long collectionId = collectionDao.insertCollection(
                new CollectionEntity(0L, "Architecture", "Saved heritage photos", 1600L)
        );
        collectionDao.addPhotoToCollection(new CollectionPhotoCrossRef(collectionId, "p200", 1700L));

        photoDao.updateFavoriteState("p200", false, 1800L);
        photoDao.deleteOrphanIfUnreferenced("p200");

        PhotoEntity retained = photoDao.getPhotoByIdSync("p200");
        assertNotNull(retained);
        assertFalse(retained.isFavorite());
    }

    @Test
    public void searchFavorites_matchesTitleOrOwnerAndExcludesNonFavorites() throws Exception {
        photoDao.upsertPhoto(photo("title", "Hanoi sunset", "Alice", true, 100));
        photoDao.upsertPhoto(photo("owner", "Bridge", "Hanoi photographer", true, 200));
        photoDao.upsertPhoto(photo("other", "Mountains", "Bob", true, 300));
        photoDao.upsertPhoto(photo("not-favorite", "Hanoi", "Hanoi", false, 400));

        assertEquals(Arrays.asList("owner", "title"), ids(awaitValue(
                photoDao.searchFavoritePhotos("hAnOi"))));
        assertEquals(3, awaitValue(photoDao.searchFavoritePhotos("")).size());
        assertTrue(awaitValue(photoDao.searchFavoritePhotos("no match")).isEmpty());
    }

    @Test
    public void favorites_supportSavedDateAndCaseInsensitiveTitleSorting() throws Exception {
        photoDao.upsertPhoto(photo("a", "alpha", "Owner", true, 100));
        photoDao.upsertPhoto(photo("b", "Beta", "Owner", true, 300));
        photoDao.upsertPhoto(photo("c", "alpha", "Owner", true, 200));

        assertEquals(Arrays.asList("b", "c", "a"), ids(awaitValue(
                photoDao.observeFavoritePhotos())));
        assertEquals(Arrays.asList("a", "c", "b"), ids(awaitValue(
                photoDao.observeFavoritePhotosByTitle())));
        assertEquals(Arrays.asList("a", "c", "b"), ids(awaitValue(
                photoDao.searchFavoritePhotosByTitle("Owner"))));
    }

    @Test
    public void favoriteSearch_emitsUpdatedResultsAfterUnfavoriting() throws Exception {
        photoDao.upsertPhoto(photo("p1", "River", "Owner", true, 100));
        androidx.lifecycle.LiveData<List<PhotoEntity>> search =
                photoDao.searchFavoritePhotos("River");
        assertEquals(1, awaitValue(search).size());

        photoDao.updateFavoriteState("p1", false, 200);

        assertTrue(awaitValue(search, List::isEmpty).isEmpty());
    }

    @Test
    public void orphanCleanup_retainsFavoriteWithoutCollection() {
        photoDao.upsertPhoto(photo("p1", "River", "Owner", true, 100));

        photoDao.deleteOrphanIfUnreferenced("p1");

        assertNotNull(photoDao.getPhotoByIdSync("p1"));
    }

    @Test
    public void savedPhotosAndMemberships_surviveDatabaseReopen() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        String name = "photo-persistence-" + java.util.UUID.randomUUID() + ".db";
        AppDatabase diskDatabase = null;
        try {
            diskDatabase = Room.databaseBuilder(context, AppDatabase.class, name).build();
            diskDatabase.photoDao().upsertPhoto(photo("p1", "River", "Owner", true, 100));
            long id = diskDatabase.collectionDao().insertCollection(
                    new CollectionEntity(0, "Travel", "", 100));
            diskDatabase.collectionDao().addPhotoToCollection(
                    new CollectionPhotoCrossRef(id, "p1", 100));
            diskDatabase.close();

            diskDatabase = Room.databaseBuilder(context, AppDatabase.class, name).build();
            assertTrue(diskDatabase.photoDao().getPhotoByIdSync("p1").isFavorite());
            assertEquals("p1", awaitValue(diskDatabase.collectionDao()
                    .observeCollectionById(id)).photos.get(0).getId());
        } finally {
            if (diskDatabase != null) {
                diskDatabase.close();
            }
            context.deleteDatabase(name);
        }
    }

    static PhotoEntity photo(String id, String title, String owner, boolean favorite, long savedAt) {
        return new PhotoEntity(id, title, owner, "Description",
                "https://example.com/thumb.jpg", "https://example.com/detail.jpg", favorite, savedAt);
    }

    private static List<String> ids(List<PhotoEntity> photos) {
        return photos.stream().map(PhotoEntity::getId).collect(Collectors.toList());
    }
}
