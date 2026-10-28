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
        version = 2,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "flickr_image_browser.db";

    private static volatile AppDatabase instance;

    /**
     * Migration from v1 to v2 adding width and height columns to photos table
     * for natural Pinterest-style aspect ratio rendering without re-fetching.
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE photos ADD COLUMN width INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE photos ADD COLUMN height INTEGER NOT NULL DEFAULT 0");
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
