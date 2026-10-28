package vn.edu.usth.flickrimagebrowser.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Locale;
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

    private final int width;
    private final int height;
    private final boolean favorite;
    private final long savedAt;

    public Photo(@NonNull String id,
                 @Nullable String title,
                 @Nullable String ownerName,
                 @Nullable String description,
                 @Nullable String thumbnailUrl,
                 @Nullable String detailUrl,
                 int width,
                 int height,
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
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.favorite = favorite;
        this.savedAt = savedAt;
    }

    public Photo(@NonNull String id,
                 @Nullable String title,
                 @Nullable String ownerName,
                 @Nullable String description,
                 @Nullable String thumbnailUrl,
                 @Nullable String detailUrl,
                 boolean favorite,
                 long savedAt) {
        this(id, title, ownerName, description, thumbnailUrl, detailUrl, 0, 0, favorite, savedAt);
    }

    @NonNull
    public Photo withFavorite(boolean newFavorite, long newSavedAt) {
        return new Photo(id, title, ownerName, description, thumbnailUrl, detailUrl, width, height, newFavorite, newSavedAt);
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

    /**
     * Calculates the clamped aspect ratio (width / height) bounded between 0.5f (tall 1:2)
     * and 2.0f (wide 2:1) for harmonious Pinterest-style feed display.
     */
    public float getAspectRatio() {
        if (width <= 0 || height <= 0) {
            return 1.0f;
        }
        float ratio = (float) width / (float) height;
        if (ratio < 0.5f) {
            return 0.5f;
        } else if (ratio > 2.0f) {
            return 2.0f;
        }
        return ratio;
    }

    /**
     * Returns the ConstraintLayout dimension ratio string formatted for height-constrained
     * views (e.g., "H,1:1", "H,1:2", "H,2:1", or "H,width:height").
     */
    @NonNull
    public String getDimensionRatio() {
        if (width <= 0 || height <= 0) {
            return "H,1:1";
        }
        float ratio = (float) width / (float) height;
        if (ratio < 0.5f) {
            return "H,1:2";
        } else if (ratio > 2.0f) {
            return "H,2:1";
        }
        return String.format(Locale.US, "H,%d:%d", width, height);
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
        return width == photo.width
                && height == photo.height
                && favorite == photo.favorite
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
        return Objects.hash(id, title, ownerName, description, thumbnailUrl, detailUrl, width, height, favorite, savedAt);
    }
}
