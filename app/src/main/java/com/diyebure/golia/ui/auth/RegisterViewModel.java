package com.diyebure.golia.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.diyebure.golia.domain.usecase.auth.RegisterUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the registration screen.
 *
 * <p>Owns the form state (fields + validation errors) and delegates the actual
 * account creation to {@link RegisterUseCase}. Validation stays here; the
 * network/business work lives in the use case and repository. Results arrive on
 * a background thread, so state is published with {@code postValue}.
 */
@HiltViewModel
public class RegisterViewModel extends ViewModel {

    public enum RegistrationState {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    private final RegisterUseCase registerUseCase;

    private final MutableLiveData<RegistrationState> registrationState = new MutableLiveData<>(RegistrationState.IDLE);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Form fields
    private final MutableLiveData<String> username = new MutableLiveData<>("");
    private final MutableLiveData<String> email = new MutableLiveData<>("");
    private final MutableLiveData<String> password = new MutableLiveData<>("");
    private final MutableLiveData<String> confirmPassword = new MutableLiveData<>("");

    @Inject
    public RegisterViewModel(RegisterUseCase registerUseCase) {
        this.registerUseCase = registerUseCase;
    }

    /**
     * Returns the current registration state as LiveData.
     */
    public LiveData<RegistrationState> getRegistrationState() {
        return registrationState;
    }

    /**
     * Returns the current error message as LiveData.
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the username LiveData.
     */
    public LiveData<String> getUsername() {
        return username;
    }

    /**
     * Returns the email LiveData.
     */
    public LiveData<String> getEmail() {
        return email;
    }

    /**
     * Returns the password LiveData.
     */
    public LiveData<String> getPassword() {
        return password;
    }

    /**
     * Returns the confirm password LiveData.
     */
    public LiveData<String> getConfirmPassword() {
        return confirmPassword;
    }

    /**
     * Sets the username value.
     */
    public void setUsername(String value) {
        username.setValue(value);
    }

    /**
     * Sets the email value.
     */
    public void setEmail(String value) {
        email.setValue(value);
    }

    /**
     * Sets the password value.
     */
    public void setPassword(String value) {
        password.setValue(value);
    }

    /**
     * Sets the confirm password value.
     */
    public void setConfirmPassword(String value) {
        confirmPassword.setValue(value);
    }

    /**
     * Validates the username.
     *
     * @param value The username to validate
     * @return true if valid, false otherwise
     */
    public boolean validateUsername(String value) {
        if (value == null || value.trim().isEmpty()) {
            errorMessage.setValue("Username is required");
            return false;
        }
        if (value.trim().length() < 3) {
            errorMessage.setValue("Username must be at least 3 characters");
            return false;
        }
        if (!value.matches("^[a-zA-Z0-9_]+$")) {
            errorMessage.setValue("Username can only contain letters, numbers, and underscores");
            return false;
        }
        return true;
    }

    /**
     * Validates the email format.
     *
     * @param value The email to validate
     * @return true if valid, false otherwise
     */
    public boolean validateEmail(String value) {
        if (value == null || value.trim().isEmpty()) {
            errorMessage.setValue("Email is required");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            errorMessage.setValue("Invalid email format");
            return false;
        }
        return true;
    }

    /**
     * Validates the password.
     *
     * @param value The password to validate
     * @return true if valid, false otherwise
     */
    public boolean validatePassword(String value) {
        if (value == null || value.isEmpty()) {
            errorMessage.setValue("Password is required");
            return false;
        }
        if (value.length() < 6) {
            errorMessage.setValue("Password must be at least 6 characters");
            return false;
        }
        if (!value.matches(".*[A-Z].*")) {
            errorMessage.setValue("Password must contain at least one uppercase letter");
            return false;
        }
        if (!value.matches(".*[0-9].*")) {
            errorMessage.setValue("Password must contain at least one number");
            return false;
        }
        return true;
    }

    /**
     * Validates that confirm password matches password.
     *
     * @param passwordValue        The original password
     * @param confirmPasswordValue The confirmation password
     * @return true if valid, false otherwise
     */
    public boolean validateConfirmPassword(String passwordValue, String confirmPasswordValue) {
        if (confirmPasswordValue == null || confirmPasswordValue.isEmpty()) {
            errorMessage.setValue("Please confirm your password");
            return false;
        }
        if (!confirmPasswordValue.equals(passwordValue)) {
            errorMessage.setValue("Passwords do not match");
            return false;
        }
        return true;
    }

    /**
     * Attempts to register a new user with the provided credentials.
     */
    public void register() {
        String usernameValue = username.getValue();
        String emailValue = email.getValue();
        String passwordValue = password.getValue();
        String confirmValue = confirmPassword.getValue();

        // Validate all fields
        if (!validateUsername(usernameValue != null ? usernameValue : "")) {
            return;
        }
        if (!validateEmail(emailValue != null ? emailValue : "")) {
            return;
        }
        if (!validatePassword(passwordValue != null ? passwordValue : "")) {
            return;
        }
        if (!validateConfirmPassword(passwordValue != null ? passwordValue : "", confirmValue != null ? confirmValue : "")) {
            return;
        }

        registrationState.setValue(RegistrationState.LOADING);

        // Country is not collected by the current form yet; pass empty so the
        // backend can apply its default. Extend the form + this call when a
        // country selector is added.
        registerUseCase.execute(
                usernameValue,
                emailValue,
                passwordValue,
                "",
                result -> {
                    if (result.isSuccess()) {
                        registrationState.postValue(RegistrationState.SUCCESS);
                    } else {
                        Exception error = result.getErrorOrNull();
                        errorMessage.postValue(error != null ? error.getMessage() : "Error al registrarse");
                        registrationState.postValue(RegistrationState.ERROR);
                    }
                });
    }

    /**
     * Clears the current error message.
     */
    public void clearError() {
        errorMessage.setValue(null);
    }

    /**
     * Resets the registration state to idle and clears all fields.
     */
    public void resetState() {
        registrationState.setValue(RegistrationState.IDLE);
        username.setValue("");
        email.setValue("");
        password.setValue("");
        confirmPassword.setValue("");
        clearError();
    }
}