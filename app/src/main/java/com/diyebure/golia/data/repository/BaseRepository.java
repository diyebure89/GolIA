package com.diyebure.golia.data.repository;

import com.diyebure.golia.data.Result;

/**
 * Base Repository class providing foundation for repository pattern
 * - Result wrapper for handling success/failure states
 * - Common repository operations
 */
public abstract class BaseRepository {

    /**
     * Execute a repository operation safely
     * Wraps the result in a Result object
     */
    protected <T> Result<T> executeSafe(RepositoryOperation<T> operation) {
        try {
            T data = operation.execute();
            return new Result.Success<>(data);
        } catch (Exception e) {
            return new Result.Error(e);
        }
    }

    /**
     * Functional interface for repository operations
     */
    @FunctionalInterface
    protected interface RepositoryOperation<T> {
        T execute() throws Exception;
    }
}