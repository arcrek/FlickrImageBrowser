package vn.edu.usth.flickrimagebrowser.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * Room entity representing a user-defined photo collection.
 */
@Entity(
        tableName = "collections",
        indices = {@Index(value = {"name"}, unique = true)}
)
public class CollectionEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private final long id;

    @NonNull
    @ColumnInfo(name = "name")
    private final String name;

    @NonNull
    @ColumnInfo(name = "description")
    private final String description;

    @ColumnInfo(name = "created_at")
    private final long createdAt;

    public CollectionEntity(long id,
                            @NonNull String name,
                            @NonNull String description,
                            long createdAt) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.createdAt = createdAt;
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
}
