package com.diyebure.golia.di;

import com.diyebure.golia.data.repository.LocalAuthRepositoryImpl;
import com.diyebure.golia.data.repository.MatchRepositoryImpl;
import com.diyebure.golia.data.repository.NewsRepositoryImpl;
import com.diyebure.golia.domain.repository.AuthRepository;
import com.diyebure.golia.domain.repository.MatchRepository;
import com.diyebure.golia.domain.repository.NewsRepository;

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

    @Binds
    @Singleton
    public abstract AuthRepository bindAuthRepository(LocalAuthRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract MatchRepository bindMatchRepository(MatchRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract NewsRepository bindNewsRepository(NewsRepositoryImpl impl);
}
