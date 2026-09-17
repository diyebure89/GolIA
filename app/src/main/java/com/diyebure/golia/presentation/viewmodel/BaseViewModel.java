package com.diyebure.golia.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Base ViewModel class providing common state management
 * - LiveData for state management
 * - Loading state handling
 * - Error handling abstraction
 */
public abstract class BaseViewModel extends ViewModel {

    protected final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    protected final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    protected final MutableLiveData<Boolean> isSuccess = new MutableLiveData<>();

    /**
     * Get loading state LiveData
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Get error message LiveData
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Get success state LiveData
     */
    public LiveData<Boolean> getIsSuccess() {
        return isSuccess;
    }

    /**
     * Set loading state
     */
    protected void setLoading(boolean loading) {
        isLoading.postValue(loading);
    }

    /**
     * Set error message
     */
    protected void setError(String message) {
        errorMessage.postValue(message);
    }

    /**
     * Clear error message
     */
    protected void clearError() {
        errorMessage.postValue(null);
    }

    /**
     * Set success state
     */
    protected void setSuccess(boolean success) {
        isSuccess.postValue(success);
    }

    /**
     * Reset all states to initial values
     */
    protected void resetState() {
        isLoading.postValue(false);
        errorMessage.postValue(null);
        isSuccess.postValue(false);
    }

    /**
     * Check if currently loading
     */
    public boolean isLoadingState() {
        Boolean loading = isLoading.getValue();
        return loading != null && loading;
    }
}