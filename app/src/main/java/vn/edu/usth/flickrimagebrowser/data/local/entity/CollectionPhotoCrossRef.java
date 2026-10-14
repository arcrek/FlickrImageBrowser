package vn.edu.usth.flickrimagebrowser.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import java.util.Objects;

/**
 * Many-to-many junction table associating collections and saved photos.
 */
@Entity(
        tableName = "collection_photo_cross_ref",
        primaryKeys = {"collection_id", "photo_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = CollectionEntity.class,
                        parentColumns = "id",
                        childColumns = "collection_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = PhotoEntity.class,
                        parentColumns = "id",
                        childColumns = "photo_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"collection_id"}),
                @Index(value = {"photo_id"})
        }
)
public class CollectionPhotoCrossRef {

    @ColumnInfo(name = "collection_id")
    private final long collectionId;

    @NonNull
    @ColumnInfo(name = "photo_id")
    private final String photoId;

    @ColumnInfo(name = "added_at")
    private final long addedAt;

    public CollectionPhotoCrossRef(long collectionId, @NonNull String photoId, long addedAt) {
        this.collectionId = collectionId;
        this.photoId = Objects.requireNonNull(photoId, "photoId must not be null");
        this.addedAt = addedAt;
    }

    public long getCollectionId() {
        return collectionId;
    }

    @NonNull
    public String getPhotoId() {
        return photoId;
    }

    public long getAddedAt() {
        return addedAt;
    }
}
