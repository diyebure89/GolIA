package com.diyebure.golia.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.diyebure.golia.data.local.dao.CompetitionDao;
import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.dao.NewsArticleDao;
import com.diyebure.golia.data.local.dao.PredictionDao;
import com.diyebure.golia.data.local.dao.TeamDao;
import com.diyebure.golia.data.local.dao.UserDao;
import com.diyebure.golia.data.local.entity.CompetitionEntity;
import com.diyebure.golia.data.local.entity.MatchEntity;
import com.diyebure.golia.data.local.entity.NewsArticleEntity;
import com.diyebure.golia.data.local.entity.NewsLeagueCrossRefEntity;
import com.diyebure.golia.data.local.entity.NewsLeagueMetaEntity;
import com.diyebure.golia.data.local.entity.PredictionEntity;
import com.diyebure.golia.data.local.entity.TeamEntity;
import com.diyebure.golia.data.local.entity.UserEntity;

/**
 * Room database for the GolIA application.
 *
 * <p>Manages local storage of matches, teams, competitions and predictions for
 * offline support.
 *
 * <p>The database instance and its DAOs are provided as singletons by
 * {@code DatabaseModule} (Hilt). This class intentionally does NOT expose a
 * static {@code getInstance()} factory: creation and lifetime are owned by the
 * DI container, which keeps the data layer testable (a fake database/DAOs can
 * be bound in tests) and avoids hidden global state.
 */
@Database(
        entities = {
                MatchEntity.class,
                TeamEntity.class,
                CompetitionEntity.class,
                PredictionEntity.class,
                UserEntity.class,
                NewsArticleEntity.class,
                NewsLeagueCrossRefEntity.class,
                NewsLeagueMetaEntity.class
        },
        version = 4,
        exportSchema = true
)
public abstract class GolIADatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "golia_database";

    /**
     * Migration from schema version 2 to 3.
     *
     * <p>Adds the {@code elapsed_minute}, {@code venue_name} and
     * {@code venue_city} columns to the {@code matches} table to support the
     * live minute and venue information from API-Football. Existing rows are
     * preserved; the new columns default to NULL.
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE matches ADD COLUMN elapsed_minute INTEGER");
            db.execSQL("ALTER TABLE matches ADD COLUMN venue_name TEXT");
            db.execSQL("ALTER TABLE matches ADD COLUMN venue_city TEXT");
        }
    };

    /**
     * Migration from schema version 3 to 4.
     *
     * <p>Adds the local storage for the football news feed: the
     * {@code news_article} table, the {@code news_league_cross_ref} join table
     * (with its index on {@code league_key}) and the {@code news_league_meta}
     * table tracking the last fetch instant per league. Existing rows in other
     * tables are preserved; only new tables are created.
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `news_article` (`article_id` TEXT NOT NULL, `title` TEXT, `description` TEXT, `image_url` TEXT, `source_name` TEXT, `article_url` TEXT, `published_at_epoch_utc` INTEGER NOT NULL, PRIMARY KEY(`article_id`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `news_league_cross_ref` (`article_id` TEXT NOT NULL, `league_key` TEXT NOT NULL, PRIMARY KEY(`article_id`, `league_key`))");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_news_league_cross_ref_league_key` ON `news_league_cross_ref` (`league_key`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `news_league_meta` (`league_key` TEXT NOT NULL, `last_fetched_at_epoch_ms` INTEGER NOT NULL, PRIMARY KEY(`league_key`))");
        }
    };

    public abstract MatchDao matchDao();

    public abstract TeamDao teamDao();

    public abstract CompetitionDao competitionDao();

    public abstract PredictionDao predictionDao();

    public abstract UserDao userDao();

    public abstract NewsArticleDao newsArticleDao();
}
