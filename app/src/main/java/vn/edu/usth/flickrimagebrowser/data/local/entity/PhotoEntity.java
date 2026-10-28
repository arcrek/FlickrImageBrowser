package vn.edu.usth.flickrimagebrowser.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * Room entity storing photo metadata for offline favorites and collections.
 */
@Entity(
        tableName = "photos",
        indices = {@Index(value = {"is_favorite", "saved_at"})}
)
public class PhotoEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private final String id;

    @NonNull
    @ColumnInfo(name = "title")
    private final String title;

    @NonNull
    @ColumnInfo(name = "owner_name")
    private final String ownerName;

    @NonNull
    @ColumnInfo(name = "description")
    private final String description;

    @NonNull
    @ColumnInfo(name = "thumbnail_url")
    private final String thumbnailUrl;

    @NonNull
    @ColumnInfo(name = "detail_url")
    private final String detailUrl;

    @ColumnInfo(name = "width", defaultValue = "0")
    private final int width;

    @ColumnInfo(name = "height", defaultValue = "0")
    private final int height;

    @ColumnInfo(name = "is_favorite")
    private final boolean isFavorite;

    @ColumnInfo(name = "saved_at")
    private final long savedAt;

    public PhotoEntity(@NonNull String id,
                       @NonNull String title,
                       @NonNull String ownerName,
                       @NonNull String description,
                       @NonNull String thumbnailUrl,
                       @NonNull String detailUrl,
                       int width,
                       int height,
                       boolean isFavorite,
                       long savedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.ownerName = Objects.requireNonNull(ownerName, "ownerName must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.thumbnailUrl = Objects.requireNonNull(thumbnailUrl, "thumbnailUrl must not be null");
        this.detailUrl = Objects.requireNonNull(detailUrl, "detailUrl must not be null");
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.isFavorite = isFavorite;
        this.savedAt = savedAt;
    }

    @Ignore
    public PhotoEntity(@NonNull String id,
                       @NonNull String title,
                       @NonNull String ownerName,
                       @NonNull String description,
                       @NonNull String thumbnailUrl,
                       @NonNull String detailUrl,
                       boolean isFavorite,
                       long savedAt) {
        this(id, title, ownerName, description, thumbnailUrl, detailUrl, 0, 0, isFavorite, savedAt);
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getOwnerName() {
        return ownerName;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @NonNull
    public String getDetailUrl() {
        return detailUrl;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public long getSavedAt() {
        return savedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhotoEntity)) return false;
        PhotoEntity that = (PhotoEntity) o;
        return width == that.width
                && height == that.height
                && isFavorite == that.isFavorite
                && savedAt == that.savedAt
                && id.equals(that.id)
                && title.equals(that.title)
                && ownerName.equals(that.ownerName)
                && description.equals(that.description)
                && thumbnailUrl.equals(that.thumbnailUrl)
                && detailUrl.equals(that.detailUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, ownerName, description, thumbnailUrl, detailUrl, width, height, isFavorite, savedAt);
    }
}
