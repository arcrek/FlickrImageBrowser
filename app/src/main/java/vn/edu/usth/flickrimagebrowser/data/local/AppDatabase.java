package vn.edu.usth.flickrimagebrowser.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionEntity;
import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionPhotoCrossRef;
import vn.edu.usth.flickrimagebrowser.data.local.entity.PhotoEntity;

/**
 * Room database holding saved Flickr photos, favorites, and user collections.
 * Uses explicit schema migrations rather than destructive migrations.
 */
@Database(
        entities = {
                PhotoEntity.class,
                CollectionEntity.class,
                CollectionPhotoCrossRef.class
        },
        version = 1,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "flickr_image_browser.db";

    private static volatile AppDatabase instance;

    /**
     * Explicit migration placeholder for future schema upgrades (v1 -> v2).
     * Avoids destructive migrations so saved favorites and collections are preserved.
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Reserved for future v2 schema additions (e.g., ALTER TABLE photos ADD COLUMN ...).
        }
    };

    public abstract PhotoDao photoDao();

    public abstract CollectionDao collectionDao();

    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
