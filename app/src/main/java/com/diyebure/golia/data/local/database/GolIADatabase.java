package com.diyebure.golia.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.diyebure.golia.data.local.dao.CompetitionDao;
import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.dao.TeamDao;
import com.diyebure.golia.data.local.entity.CompetitionEntity;
import com.diyebure.golia.data.local.entity.MatchEntity;
import com.diyebure.golia.data.local.entity.TeamEntity;

/**
 * Room database for the GolIA application.
 * Manages local storage of matches, teams, and competitions for offline support.
 */
@Database(
        entities = {
                MatchEntity.class,
                TeamEntity.class,
                CompetitionEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class GolIADatabase extends RoomDatabase {

    private static final String TAG = "GolIADatabase";
    private static final String DATABASE_NAME = "golia_database";

    // DAOs
    public abstract MatchDao matchDao();
    public abstract TeamDao teamDao();
    public abstract CompetitionDao competitionDao();

    // Singleton instance
    private static volatile GolIADatabase INSTANCE;

    /**
     * Get the singleton instance of the database.
     * Uses double-checked locking for thread safety.
     *
     * @param context Application context
     * @return Database instance
     */
    public static GolIADatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (GolIADatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = buildDatabase(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Build the Room database with all DAOs.
     *
     * @param context Application context
     * @return Database instance
     */
    private static GolIADatabase buildDatabase(Context context) {
        return Room.databaseBuilder(
                        context,
                        GolIADatabase.class,
                        DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }

    /**
     * Close the database instance.
     * Should be called when the application is being destroyed.
     */
    public static void closeDatabase() {
        if (INSTANCE != null) {
            if (INSTANCE.isOpen()) {
                INSTANCE.close();
            }
            INSTANCE = null;
        }
    }

    /**
     * Check if the database instance exists and is open.
     *
     * @return true if database is open, false otherwise
     */
    public boolean isDatabaseOpen() {
        return INSTANCE != null && INSTANCE.isOpen();
    }
}