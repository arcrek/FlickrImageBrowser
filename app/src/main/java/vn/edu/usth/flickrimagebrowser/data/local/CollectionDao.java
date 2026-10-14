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

/**
 * Room DAO for managing user collections and collection-photo associations.
 */
@Dao
public interface CollectionDao {

    @Transaction
    @Query("SELECT * FROM collections ORDER BY created_at DESC")
    LiveData<List<CollectionWithPhotos>> observeCollectionsWithPhotos();

    @Query("SELECT * FROM collections ORDER BY created_at DESC")
    List<CollectionEntity> getCollectionsSync();

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertCollection(@NonNull CollectionEntity collectionEntity);

    @Delete
    void deleteCollection(@NonNull CollectionEntity collectionEntity);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addPhotoToCollection(@NonNull CollectionPhotoCrossRef crossRef);

    @Query("DELETE FROM collection_photo_cross_ref WHERE collection_id = :collectionId AND photo_id = :photoId")
    void removePhotoFromCollection(long collectionId, @NonNull String photoId);
}
