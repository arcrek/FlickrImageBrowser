package vn.edu.usth.flickrimagebrowser.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

import vn.edu.usth.flickrimagebrowser.data.local.entity.PhotoEntity;

/**
 * Room DAO for querying and mutating persisted photo metadata and favorites.
 */
@Dao
public interface PhotoDao {

    @Query("SELECT * FROM photos WHERE is_favorite = 1 ORDER BY saved_at DESC")
    LiveData<List<PhotoEntity>> observeFavoritePhotos();

    @Query("SELECT * FROM photos WHERE is_favorite = 1 " +
            "ORDER BY title COLLATE NOCASE ASC, id ASC")
    LiveData<List<PhotoEntity>> observeFavoritePhotosByTitle();

    @Query("SELECT * FROM photos WHERE is_favorite = 1 " +
            "AND (title LIKE '%' || :query || '%' OR owner_name LIKE '%' || :query || '%') " +
            "ORDER BY saved_at DESC, id ASC")
    LiveData<List<PhotoEntity>> searchFavoritePhotos(@NonNull String query);

    @Query("SELECT * FROM photos WHERE is_favorite = 1 " +
            "AND (title LIKE '%' || :query || '%' OR owner_name LIKE '%' || :query || '%') " +
            "ORDER BY title COLLATE NOCASE ASC, id ASC")
    LiveData<List<PhotoEntity>> searchFavoritePhotosByTitle(@NonNull String query);

    @Query("SELECT * FROM photos WHERE is_favorite = 1 ORDER BY saved_at DESC")
    List<PhotoEntity> getFavoritePhotosSync();

    @Query("SELECT id FROM photos WHERE is_favorite = 1")
    List<String> getFavoriteIdsSync();

    @Nullable
    @Query("SELECT * FROM photos WHERE id = :photoId LIMIT 1")
    PhotoEntity getPhotoByIdSync(@NonNull String photoId);

    @Query("SELECT COALESCE((SELECT is_favorite FROM photos WHERE id = :photoId LIMIT 1), 0)")
    LiveData<Boolean> observeIsFavorite(@NonNull String photoId);

    // REPLACE deletes the old row and cascades away its collection memberships.
    @Upsert
    void upsertPhoto(@NonNull PhotoEntity photoEntity);

    @Query("UPDATE photos SET is_favorite = :isFavorite, saved_at = :savedAt WHERE id = :photoId")
    void updateFavoriteState(@NonNull String photoId, boolean isFavorite, long savedAt);

    @Query("DELETE FROM photos WHERE id = :photoId AND is_favorite = 0 " +
            "AND NOT EXISTS (SELECT 1 FROM collection_photo_cross_ref WHERE photo_id = :photoId)")
    void deleteOrphanIfUnreferenced(@NonNull String photoId);
}
