package vn.edu.usth.flickrimagebrowser.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static vn.edu.usth.flickrimagebrowser.data.local.LiveDataTestUtil.awaitValue;
import static vn.edu.usth.flickrimagebrowser.data.local.PhotoDaoTest.photo;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionEntity;
import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionPhotoCrossRef;
import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionWithPhotos;

@RunWith(AndroidJUnit4.class)
public class CollectionDaoTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private PhotoDao photos;
    private CollectionDao collections;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries().build();
        photos = database.photoDao();
        collections = database.collectionDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void singleCollectionAndCount_emitMembershipChanges() throws Exception {
        long id = collection("Travel");
        long otherId = collection("Other");
        photos.upsertPhoto(photo("p1", "River", "Owner", false, 100));
        photos.upsertPhoto(photo("p2", "Mountain", "Owner", false, 200));
        collections.addPhotoToCollection(new CollectionPhotoCrossRef(otherId, "p2", 100));
        LiveData<CollectionWithPhotos> relation = collections.observeCollectionById(id);
        LiveData<Integer> count = collections.observePhotoCount(id);
        assertTrue(awaitValue(relation).photos.isEmpty());
        assertEquals(Integer.valueOf(0), awaitValue(count));

        collections.addPhotoToCollection(new CollectionPhotoCrossRef(id, "p1", 100));
        // The composite primary key makes repeated assignments idempotent.
        collections.addPhotoToCollection(new CollectionPhotoCrossRef(id, "p1", 200));
        CollectionWithPhotos updated = awaitValue(relation, value -> value.photos.size() == 1);
        assertEquals("p1", updated.photos.get(0).getId());
        assertEquals(Integer.valueOf(1), awaitValue(count, value -> value == 1));
        assertEquals(id, awaitValue(collections.observeCollectionWithPhotosById(id))
                .collection.getId());

        collections.removePhotoFromCollection(id, "p1");
        assertTrue(awaitValue(relation, value -> value.photos.isEmpty()).photos.isEmpty());
        assertEquals(Integer.valueOf(0), awaitValue(count, value -> value == 0));
    }

    @Test
    public void renameCollection_preservesPhotosAndEnforcesUniqueNames() throws Exception {
        long id = collection("Travel");
        collection("Taken");
        photos.upsertPhoto(photo("p1", "River", "Owner", true, 100));
        collections.addPhotoToCollection(new CollectionPhotoCrossRef(id, "p1", 100));

        assertEquals(1, collections.renameCollection(id, "Renamed"));
        CollectionWithPhotos renamed = awaitValue(collections.observeCollectionById(id));
        assertEquals("Renamed", renamed.collection.getName());
        assertEquals(1, renamed.photos.size());
        assertEquals(0, collections.renameCollection(Long.MAX_VALUE, "Missing"));
        expectConstraint(() -> collections.renameCollection(id, "Taken"));
        assertEquals("Renamed", awaitValue(collections.observeCollectionById(id))
                .collection.getName());
        assertNull(awaitValue(collections.observeCollectionById(Long.MAX_VALUE)));
    }

    @Test
    public void upsertExistingPhoto_preservesAllCollectionMemberships() throws Exception {
        long first = collection("First");
        long second = collection("Second");
        photos.upsertPhoto(photo("p1", "Original", "Owner", true, 100));
        collections.addPhotoToCollection(new CollectionPhotoCrossRef(first, "p1", 100));
        collections.addPhotoToCollection(new CollectionPhotoCrossRef(second, "p1", 100));

        photos.upsertPhoto(photo("p1", "Updated", "New owner", false, 200));
        photos.deleteOrphanIfUnreferenced("p1");

        assertNotNull(photos.getPhotoByIdSync("p1"));
        assertEquals("Updated", awaitValue(collections.observeCollectionById(first))
                .photos.get(0).getTitle());
        assertEquals("New owner", awaitValue(collections.observeCollectionById(second))
                .photos.get(0).getOwnerName());
    }

    @Test
    public void deleteCollection_cascadesLinksAndRetainsSharedAndFavoritePhotos() throws Exception {
        long first = collection("First");
        long second = collection("Second");
        photos.upsertPhoto(photo("shared", "Shared", "Owner", false, 100));
        photos.upsertPhoto(photo("favorite", "Favorite", "Owner", true, 100));
        collections.addPhotoToCollection(new CollectionPhotoCrossRef(first, "shared", 100));
        collections.addPhotoToCollection(new CollectionPhotoCrossRef(second, "shared", 100));
        collections.addPhotoToCollection(new CollectionPhotoCrossRef(first, "favorite", 100));

        collections.deleteCollection(new CollectionEntity(first, "First", "", 100));
        assertNull(awaitValue(collections.observeCollectionById(first)));
        assertEquals(Integer.valueOf(0), awaitValue(collections.observePhotoCount(first)));
        photos.deleteOrphanIfUnreferenced("shared");
        photos.deleteOrphanIfUnreferenced("favorite");
        assertNotNull(photos.getPhotoByIdSync("shared"));
        assertTrue(photos.getPhotoByIdSync("favorite").isFavorite());

        collections.removePhotoFromCollection(second, "shared");
        photos.deleteOrphanIfUnreferenced("shared");
        assertNull(photos.getPhotoByIdSync("shared"));
    }

    @Test
    public void foreignKeys_rejectMissingParentsAndCascadePhotoDeletion() throws Exception {
        long id = collection("Travel");
        expectConstraint(() -> collections.addPhotoToCollection(
                new CollectionPhotoCrossRef(id, "missing", 100)));
        photos.upsertPhoto(photo("p1", "River", "Owner", false, 100));
        expectConstraint(() -> collections.addPhotoToCollection(
                new CollectionPhotoCrossRef(Long.MAX_VALUE, "p1", 100)));
        collections.addPhotoToCollection(new CollectionPhotoCrossRef(id, "p1", 100));

        database.getOpenHelper().getWritableDatabase().execSQL(
                "DELETE FROM photos WHERE id = ?", new Object[]{"p1"});

        assertEquals(Integer.valueOf(0), awaitValue(collections.observePhotoCount(id)));
        assertNotNull(awaitValue(collections.observeCollectionById(id)));
    }

    private long collection(String name) {
        return collections.insertCollection(new CollectionEntity(0, name, "", 100));
    }

    private static void expectConstraint(Runnable action) {
        try {
            action.run();
            fail("Expected a SQLite constraint violation");
        } catch (SQLiteConstraintException expected) {
            // Constraint failure must leave existing rows intact.
        }
    }
}
