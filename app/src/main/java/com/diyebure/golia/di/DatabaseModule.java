package com.diyebure.golia.di;

import android.content.Context;

import androidx.room.Room;

import com.diyebure.golia.data.local.dao.CompetitionDao;
import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.dao.PredictionDao;
import com.diyebure.golia.data.local.dao.TeamDao;
import com.diyebure.golia.data.local.dao.UserDao;
import com.diyebure.golia.data.local.database.GolIADatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt module that provides the Room database and its DAOs.
 *
 * <p>Everything here is application-scoped ({@code @Singleton}) because the
 * database is an expensive, shared resource that must have a single instance
 * per process. DAOs are derived from the database and exposed individually so
 * repositories can inject only the DAO they need, keeping constructors focused
 * and testable.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public GolIADatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                        context,
                        GolIADatabase.class,
                        GolIADatabase.DATABASE_NAME)
                // Destructive migration keeps early development simple. Replace
                // with real Migration objects before shipping to production so
                // user data survives schema changes.
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    @Singleton
    public MatchDao provideMatchDao(GolIADatabase database) {
        return database.matchDao();
    }

    @Provides
    @Singleton
    public TeamDao provideTeamDao(GolIADatabase database) {
        return database.teamDao();
    }

    @Provides
    @Singleton
    public CompetitionDao provideCompetitionDao(GolIADatabase database) {
        return database.competitionDao();
    }

    @Provides
    @Singleton
    public PredictionDao providePredictionDao(GolIADatabase database) {
        return database.predictionDao();
    }

    @Provides
    @Singleton
    public UserDao provideUserDao(GolIADatabase database) {
        return database.userDao();
    }
}
