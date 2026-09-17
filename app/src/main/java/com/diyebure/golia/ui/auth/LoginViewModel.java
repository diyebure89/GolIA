package com.diyebure.golia.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.diyebure.golia.data.LoginRepository;
import com.diyebure.golia.data.Result;

/**
 * ViewModel for handling login functionality.
 * Manages login state and validation for user authentication.
 */
public class LoginViewModel extends ViewModel {

    private final LoginRepository loginRepository;

    // Login state
    public enum LoginState {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    private final MutableLiveData<LoginState> loginState = new MutableLiveData<>(LoginState.IDLE);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LoginViewModel() {
        this.loginRepository = LoginRepository.getInstance(new com.diyebure.golia.data.LoginDataSource());
    }

    /**
     * Returns the current login state as LiveData.
     */
    public LiveData<LoginState> getLoginState() {
        return loginState;
    }

    /**
     * Returns the current error message as LiveData.
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Validates the email format.
     *
     * @param email The email to validate
     * @return true if valid, false otherwise
     */
    public boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            errorMessage.setValue("Email is required");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage.setValue("Invalid email format");
            return false;
        }
        return true;
    }

    /**
     * Validates the password.
     *
     * @param password The password to validate
     * @return true if valid, false otherwise
     */
    public boolean validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            errorMessage.setValue("Password is required");
            return false;
        }
        if (password.length() < 6) {
            errorMessage.setValue("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    /**
     * Attempts to log in with the provided email and password.
     *
     * @param email    The user's email
     * @param password The user's password
     */
    public void login(String email, String password) {
        if (!validateEmail(email) || !validatePassword(password)) {
            return;
        }

        loginState.setValue(LoginState.LOADING);

        // Perform login via repository
        Result<com.diyebure.golia.data.model.LoggedInUser> result = loginRepository.login(email, password);

        if (result.isSuccess()) {
            loginState.setValue(LoginState.SUCCESS);
        } else {
            loginState.setValue(LoginState.ERROR);
            Exception error = result.getErrorOrNull();
            errorMessage.setValue(error != null ? error.getMessage() : "Login failed");
        }
    }

    /**
     * Clears the current error message.
     */
    public void clearError() {
        errorMessage.setValue(null);
    }

    /**
     * Resets the login state to idle.
     */
    public void resetState() {
        loginState.setValue(LoginState.IDLE);
        clearError();
    }
}