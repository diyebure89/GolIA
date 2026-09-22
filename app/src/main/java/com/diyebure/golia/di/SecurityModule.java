package com.diyebure.golia.di;

import com.diyebure.golia.data.security.Pbkdf2PasswordHasher;
import com.diyebure.golia.domain.security.PasswordHasher;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Binds security contracts (domain layer) to their implementations
 * (data layer).
 *
 * <p>This module follows the same idiomatic pattern as {@code RepositoryModule}:
 * it is abstract and uses {@code @Binds} instead of {@code @Provides}. Since
 * {@code Pbkdf2PasswordHasher} already declares an {@code @Inject} constructor,
 * Hilt knows how to build it; we only need to tell it "when someone asks for
 * {@link PasswordHasher}, give them this implementation".
 *
 * <p>{@code RegistrationValidator} is a concrete class with an {@code @Inject}
 * constructor and therefore needs no binding here.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class SecurityModule {

    @Binds
    @Singleton
    public abstract PasswordHasher bindPasswordHasher(Pbkdf2PasswordHasher impl);
}
