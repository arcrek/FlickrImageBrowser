package vn.edu.usth.flickrimagebrowser.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable domain model representing a Flickr photo across UI and repository layers.
 */
public final class Photo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NonNull
    private final String id;

    @NonNull
    private final String title;

    @NonNull
    private final String ownerName;

    @NonNull
    private final String description;

    @NonNull
    private final String thumbnailUrl;

    @NonNull
    private final String detailUrl;

    private final boolean favorite;
    private final long savedAt;

    public Photo(@NonNull String id,
                 @Nullable String title,
                 @Nullable String ownerName,
                 @Nullable String description,
                 @Nullable String thumbnailUrl,
                 @Nullable String detailUrl,
                 boolean favorite,
                 long savedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.title = title != null && !title.trim().isEmpty() ? title.trim() : "Untitled Photo";
        this.ownerName = ownerName != null && !ownerName.trim().isEmpty() ? ownerName.trim() : "Unknown Author";
        this.description = description != null ? description.trim() : "";
        this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl.trim() : "";
        this.detailUrl = detailUrl != null && !detailUrl.trim().isEmpty()
                ? detailUrl.trim()
                : this.thumbnailUrl;
        this.favorite = favorite;
        this.savedAt = savedAt;
    }

    @NonNull
    public Photo withFavorite(boolean newFavorite, long newSavedAt) {
        return new Photo(id, title, ownerName, description, thumbnailUrl, detailUrl, newFavorite, newSavedAt);
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

    public boolean isFavorite() {
        return favorite;
    }

    public long getSavedAt() {
        return savedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Photo)) return false;
        Photo photo = (Photo) o;
        return favorite == photo.favorite
                && savedAt == photo.savedAt
                && id.equals(photo.id)
                && title.equals(photo.title)
                && ownerName.equals(photo.ownerName)
                && description.equals(photo.description)
                && thumbnailUrl.equals(photo.thumbnailUrl)
                && detailUrl.equals(photo.detailUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, ownerName, description, thumbnailUrl, detailUrl, favorite, savedAt);
    }
}
