package com.diyebure.golia.data.local.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Room migration test for {@link GolIADatabase}.
 *
 * <p>Verifies that {@link GolIADatabase#MIGRATION_2_3} correctly upgrades the
 * {@code matches} table from schema version 2 to 3 by adding the
 * {@code elapsed_minute}, {@code venue_name} and {@code venue_city} columns,
 * while preserving the data that existed before the migration.
 *
 * <p>This is an instrumented test: it must run on an Android device or
 * emulator (e.g. {@code ./gradlew connectedAndroidTest}). It relies on the
 * exported Room schema JSON files (version 2 and 3) produced when
 * {@code exportSchema = true} and {@code room.schemaLocation} are configured,
 * which are made available to this test via the androidTest assets source set.
 */
@RunWith(AndroidJUnit4.class)
public class MatchMigrationTest {

    private static final String TEST_DB = "migration-test-golia";

    /**
     * SQL that recreates the {@code matches} table exactly as it existed at
     * schema version 2 (before the three new columns were added).
     */
    private static final String CREATE_MATCHES_V2 =
            "CREATE TABLE IF NOT EXISTS `matches` ("
                    + "`id` TEXT NOT NULL, "
                    + "`external_id` TEXT, "
                    + "`competition_id` TEXT, "
                    + "`competition_name` TEXT, "
                    + "`matchday` INTEGER NOT NULL, "
                    + "`home_team_id` TEXT, "
                    + "`home_team_name` TEXT, "
                    + "`home_team_logo_url` TEXT, "
                    + "`away_team_id` TEXT, "
                    + "`away_team_name` TEXT, "
                    + "`away_team_logo_url` TEXT, "
                    + "`scheduled_date_time` INTEGER NOT NULL, "
                    + "`status` TEXT, "
                    + "`home_score` INTEGER, "
                    + "`away_score` INTEGER, "
                    + "`home_odds` REAL NOT NULL, "
                    + "`draw_odds` REAL NOT NULL, "
                    + "`away_odds` REAL NOT NULL, "
                    + "PRIMARY KEY(`id`))";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            GolIADatabase.class,
            new FrameworkSQLiteOpenHelperFactory());

    @Test
    public void migrate2To3_addsNewColumnsAndPreservesData() throws IOException {
        // 1) Create the database at version 2 and seed a row using the v2 schema.
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);
        db.execSQL(CREATE_MATCHES_V2);

        ContentValues seed = new ContentValues();
        seed.put("id", "match-1");
        seed.put("external_id", "ext-1");
        seed.put("competition_id", "39");
        seed.put("competition_name", "Premier League");
        seed.put("matchday", 5);
        seed.put("home_team_id", "home-1");
        seed.put("home_team_name", "Arsenal");
        seed.put("home_team_logo_url", "https://logo/home.png");
        seed.put("away_team_id", "away-1");
        seed.put("away_team_name", "Chelsea");
        seed.put("away_team_logo_url", "https://logo/away.png");
        seed.put("scheduled_date_time", 1_700_000_000_000L);
        seed.put("status", "SCHEDULED");
        seed.put("home_score", 1);
        seed.put("away_score", 2);
        seed.put("home_odds", 1.5d);
        seed.put("draw_odds", 3.2d);
        seed.put("away_odds", 4.1d);
        db.insert("matches", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, seed);

        // MigrationTestHelper requires the database to be closed before running
        // the migration.
        db.close();

        // 2) Run MIGRATION_2_3 and validate the resulting schema against the
        // exported version-3 schema.
        db = helper.runMigrationsAndValidate(
                TEST_DB, 3, true, GolIADatabase.MIGRATION_2_3);

        // 3a) The three new columns must exist in the migrated table.
        Set<String> columns = tableColumns(db, "matches");
        assertTrue("elapsed_minute column should exist after migration",
                columns.contains("elapsed_minute"));
        assertTrue("venue_name column should exist after migration",
                columns.contains("venue_name"));
        assertTrue("venue_city column should exist after migration",
                columns.contains("venue_city"));

        // 3b) The row inserted before the migration must be preserved, and the
        // new columns must default to NULL.
        Cursor cursor = db.query("SELECT * FROM matches WHERE id = 'match-1'");
        try {
            assertEquals("previously inserted row should be preserved",
                    1, cursor.getCount());
            assertTrue(cursor.moveToFirst());

            assertEquals("Premier League",
                    cursor.getString(cursor.getColumnIndexOrThrow("competition_name")));
            assertEquals("Arsenal",
                    cursor.getString(cursor.getColumnIndexOrThrow("home_team_name")));
            assertEquals("Chelsea",
                    cursor.getString(cursor.getColumnIndexOrThrow("away_team_name")));
            assertEquals(5,
                    cursor.getInt(cursor.getColumnIndexOrThrow("matchday")));
            assertEquals(1_700_000_000_000L,
                    cursor.getLong(cursor.getColumnIndexOrThrow("scheduled_date_time")));
            assertEquals(1,
                    cursor.getInt(cursor.getColumnIndexOrThrow("home_score")));
            assertEquals(2,
                    cursor.getInt(cursor.getColumnIndexOrThrow("away_score")));

            assertTrue("elapsed_minute should be NULL for pre-existing rows",
                    cursor.isNull(cursor.getColumnIndexOrThrow("elapsed_minute")));
            assertTrue("venue_name should be NULL for pre-existing rows",
                    cursor.isNull(cursor.getColumnIndexOrThrow("venue_name")));
            assertTrue("venue_city should be NULL for pre-existing rows",
                    cursor.isNull(cursor.getColumnIndexOrThrow("venue_city")));
        } finally {
            cursor.close();
        }
    }

    /**
     * Returns the set of column names for the given table using PRAGMA.
     */
    private static Set<String> tableColumns(SupportSQLiteDatabase db, String table) {
        Set<String> columns = new HashSet<>();
        Cursor cursor = db.query("PRAGMA table_info(`" + table + "`)");
        try {
            int nameIndex = cursor.getColumnIndex("name");
            if (nameIndex < 0) {
                fail("PRAGMA table_info did not return a 'name' column");
            }
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex));
            }
        } finally {
            cursor.close();
        }
        return columns;
    }
}
