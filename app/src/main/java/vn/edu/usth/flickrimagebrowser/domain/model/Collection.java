package vn.edu.usth.flickrimagebrowser.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable domain model representing a user-defined photo collection.
 */
public final class Collection implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;

    @NonNull
    private final String name;

    @NonNull
    private final String description;

    private final long createdAt;
    private final int photoCount;

    public Collection(long id,
                      @NonNull String name,
                      @Nullable String description,
                      long createdAt,
                      int photoCount) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.description = description != null ? description.trim() : "";
        this.createdAt = createdAt;
        this.photoCount = Math.max(0, photoCount);
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getPhotoCount() {
        return photoCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Collection)) return false;
        Collection that = (Collection) o;
        return id == that.id
                && createdAt == that.createdAt
                && photoCount == that.photoCount
                && name.equals(that.name)
                && description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, createdAt, photoCount);
    }
}
