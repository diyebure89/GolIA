package com.diyebure.golia.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.diyebure.golia.domain.usecase.auth.LoginUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the login screen.
 *
 * <p>Holds UI state ({@link LoginState} + error text as {@link LiveData}) and
 * delegates the actual authentication to {@link LoginUseCase}. It performs only
 * input validation; business/authentication logic lives in the use case and the
 * repository. Threading is handled by the use case, so results arrive on a
 * background thread and are published with {@code postValue}.
 *
 * <p>Annotated with {@code @HiltViewModel} so Hilt supplies its dependencies;
 * the Activity obtains it via the standard {@code ViewModelProvider}.
 */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    public enum LoginState {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    private final LoginUseCase loginUseCase;

    private final MutableLiveData<LoginState> loginState = new MutableLiveData<>(LoginState.IDLE);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    @Inject
    public LoginViewModel(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    public LiveData<LoginState> getLoginState() {
        return loginState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            errorMessage.setValue("El correo es obligatorio");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage.setValue("Formato de correo inválido");
            return false;
        }
        return true;
    }

    public boolean validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            errorMessage.setValue("La contraseña es obligatoria");
            return false;
        }
        if (password.length() < 6) {
            errorMessage.setValue("La contraseña debe tener al menos 6 caracteres");
            return false;
        }
        return true;
    }

    /**
     * Validates input and, if valid, triggers authentication through the use case.
     * The result is published on {@link #getLoginState()} / {@link #getErrorMessage()}.
     */
    public void login(String email, String password) {
        if (!validateEmail(email) || !validatePassword(password)) {
            loginState.setValue(LoginState.ERROR);
            return;
        }

        loginState.setValue(LoginState.LOADING);

        loginUseCase.execute(email, password, result -> {
            if (result.isSuccess()) {
                loginState.postValue(LoginState.SUCCESS);
            } else {
                Exception error = result.getErrorOrNull();
                errorMessage.postValue(error != null ? error.getMessage() : "Error al iniciar sesión");
                loginState.postValue(LoginState.ERROR);
            }
        });
    }

    public void clearError() {
        errorMessage.setValue(null);
    }

    public void resetState() {
        loginState.setValue(LoginState.IDLE);
        clearError();
    }
}
