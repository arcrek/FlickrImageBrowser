package vn.edu.usth.flickrimagebrowser.data.local.entity;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

/**
 * Room relation projection combining a CollectionEntity with its associated PhotoEntities.
 */
public class CollectionWithPhotos {

    @Embedded
    public CollectionEntity collection;

    @Relation(
            parentColumn = "id",
            entityColumn = "id",
            associateBy = @Junction(
                    value = CollectionPhotoCrossRef.class,
                    parentColumn = "collection_id",
                    entityColumn = "photo_id"
            )
    )
    public List<PhotoEntity> photos;
}
