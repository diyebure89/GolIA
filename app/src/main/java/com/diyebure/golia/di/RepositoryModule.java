package com.diyebure.golia.di;

import com.diyebure.golia.data.repository.AuthRepositoryImpl;
import com.diyebure.golia.data.repository.MatchRepositoryImpl;
import com.diyebure.golia.data.repository.NewsRepositoryImpl;
import com.diyebure.golia.data.repository.RemotePredictionRepository;
import com.diyebure.golia.data.repository.RealRankingDataSource;
import com.diyebure.golia.data.repository.ProfileRepositoryImpl;
import com.diyebure.golia.domain.repository.AuthRepository;
import com.diyebure.golia.domain.repository.MatchRepository;
import com.diyebure.golia.domain.repository.NewsRepository;
import com.diyebure.golia.domain.repository.PredictionRepository;
import com.diyebure.golia.domain.repository.ProfileRepository;
import com.diyebure.golia.domain.repository.RankingDataSource;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Binds repository interfaces (domain layer) to their implementations
 * (data layer).
 *
 * <p>This module is abstract and uses {@code @Binds} instead of {@code @Provides}.
 * Because {@code LocalAuthRepositoryImpl} and {@code MatchRepositoryImpl} already
 * declare {@code @Inject} constructors, Hilt knows how to build them; we only
 * need to tell it "when someone asks for the interface, give them this impl".
 *
 * <p>{@link AuthRepository} is bound to the local, Room-backed
 * {@code LocalAuthRepositoryImpl} for the MVP. The remote
 * {@code AuthRepositoryImpl} is intentionally left unbound.
 *
 * <p>This is the idiomatic pattern: the presentation and use-case layers depend
 * only on the domain interfaces ({@link AuthRepository}, {@link MatchRepository}),
 * never on the concrete data-layer classes. Swapping an implementation (e.g. a
 * fake for tests) is a one-line change here.
 *
 * <p>{@code PreferencesManager}, the DAOs and the API services are provided by
 * their own modules / {@code @Inject} constructors, so they are not repeated
 * here.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    // Auth remoto: habla con el backend GolIA (Spring Boot + MySQL), de modo que
    // las cuentas se comparten entre dispositivos. El LocalAuthRepositoryImpl
    // (Room) queda sin enlazar como respaldo/historico.
    @Binds
    @Singleton
    public abstract AuthRepository bindAuthRepository(AuthRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract MatchRepository bindMatchRepository(MatchRepositoryImpl impl);

    // Predicciones remotas: se guardan en el backend GolIA (MySQL) para que se
    // compartan entre dispositivos. PredictionRepositoryImpl (Room) queda sin enlazar.
    @Binds
    @Singleton
    public abstract PredictionRepository bindPredictionRepository(RemotePredictionRepository impl);

    @Binds
    @Singleton
    public abstract NewsRepository bindNewsRepository(NewsRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract ProfileRepository bindProfileRepository(ProfileRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract RankingDataSource bindRankingDataSource(RealRankingDataSource impl);
}
