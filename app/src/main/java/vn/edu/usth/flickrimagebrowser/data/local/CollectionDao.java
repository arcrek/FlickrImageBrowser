package vn.edu.usth.flickrimagebrowser.data.local;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionEntity;
import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionPhotoCrossRef;
import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionWithPhotos;
import vn.edu.usth.flickrimagebrowser.data.local.entity.PhotoEntity;

/**
 * Room DAO for managing user collections and collection-photo associations.
 */
@Dao
public interface CollectionDao {

    @Transaction
    @Query("SELECT * FROM collections ORDER BY created_at DESC")
    LiveData<List<CollectionWithPhotos>> observeCollectionsWithPhotos();

    @Transaction
    @Query("SELECT * FROM collections WHERE id = :collectionId")
    LiveData<CollectionWithPhotos> observeCollectionById(long collectionId);

    // Alias retained for the method name used in TODO 3.3 of the team plan.
    @Transaction
    @Query("SELECT * FROM collections WHERE id = :collectionId")
    LiveData<CollectionWithPhotos> observeCollectionWithPhotosById(long collectionId);

    @Query("UPDATE collections SET name = :newName WHERE id = :collectionId")
    int renameCollection(long collectionId, @NonNull String newName);

    @Query("UPDATE collections SET name = :newName, description = :newDescription WHERE id = :collectionId")
    int updateCollection(long collectionId, @NonNull String newName, @NonNull String newDescription);

    @Query("SELECT COUNT(*) FROM collection_photo_cross_ref WHERE collection_id = :collectionId")
    LiveData<Integer> observePhotoCount(long collectionId);

    @Query("SELECT * FROM collections ORDER BY created_at DESC")
    List<CollectionEntity> getCollectionsSync();

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertCollection(@NonNull CollectionEntity collectionEntity);

    @Delete
    void deleteCollection(@NonNull CollectionEntity collectionEntity);

    @Query("DELETE FROM collections WHERE id = :collectionId")
    void deleteCollectionById(long collectionId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addPhotoToCollection(@NonNull CollectionPhotoCrossRef crossRef);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addPhotosToCollection(@NonNull List<CollectionPhotoCrossRef> crossRefs);

    @Query("DELETE FROM collection_photo_cross_ref WHERE collection_id = :collectionId AND photo_id = :photoId")
    void removePhotoFromCollection(long collectionId, @NonNull String photoId);

    @Transaction
    default void addPhotosAndCrossRefs(@NonNull PhotoDao photoDao,
                                      @NonNull List<PhotoEntity> entities,
                                      @NonNull List<CollectionPhotoCrossRef> crossRefs) {
        photoDao.upsertPhotos(entities);
        addPhotosToCollection(crossRefs);
    }

    @Transaction
    default void removePhotosAndCleanOrphans(@NonNull PhotoDao photoDao,
                                            long collectionId,
                                            @NonNull List<String> photoIds) {
        for (String photoId : photoIds) {
            removePhotoFromCollection(collectionId, photoId);
            photoDao.deleteOrphanIfUnreferenced(photoId);
        }
    }

    @Transaction
    default void deleteCollectionAndCleanOrphans(@NonNull PhotoDao photoDao, long collectionId) {
        deleteCollectionById(collectionId);
        photoDao.deleteAllOrphanPhotos();
    }
}
