package vn.edu.usth.flickrimagebrowser.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
}
