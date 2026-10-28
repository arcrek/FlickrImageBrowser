package vn.edu.usth.flickrimagebrowser.data.local;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.sqlite.db.SupportSQLiteDatabase;

import org.junit.Test;

/**
 * Unit tests verifying Room database schema migration v1 -> v2.
 */
public class DatabaseMigrationTest {

    @Test
    public void migration1To2_executesAlterTableQueries() {
        SupportSQLiteDatabase db = mock(SupportSQLiteDatabase.class);

        AppDatabase.MIGRATION_1_2.migrate(db);

        verify(db).execSQL("ALTER TABLE photos ADD COLUMN width INTEGER NOT NULL DEFAULT 0");
        verify(db).execSQL("ALTER TABLE photos ADD COLUMN height INTEGER NOT NULL DEFAULT 0");
    }
}
