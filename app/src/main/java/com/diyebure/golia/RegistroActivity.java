package com.diyebure.golia;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.diyebure.golia.ui.auth.RegisterViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Registration activity for user signup.
 * Handles user registration form validation and submission.
 */
public class RegistroActivity extends AppCompatActivity {

    private RegisterViewModel registerViewModel;

    // Form fields
    private TextInputEditText editText_nombre;
    private TextInputEditText editText_usuario;
    private TextInputEditText editText_email;
    private TextInputEditText editText_password;
    private TextInputEditText editText_confirmar_password;

    // TextInputLayouts for error display
    private TextInputLayout textInputLayout_usuario;
    private TextInputLayout textInputLayout_email;
    private TextInputLayout textInputLayout_password;
    private TextInputLayout textInputLayout_confirmar_password;

    // Buttons
    private Button button_registro;

    // Progress indicator
    private ProgressBar progressBar_registro;

    // Login link
    private LinearLayout linearLayout_yacuenta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        // Initialize ViewModel
        registerViewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        // Initialize views
        initializeViews();
        setupTextWatchers();
        setupClickListeners();
        observeViewModel();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Initializes all view references.
     */
    private void initializeViews() {
        // Form fields
        editText_nombre = findViewById(R.id.editText_nombre);
        editText_usuario = findViewById(R.id.editText_usuario);
        editText_email = findViewById(R.id.editText_email);
        editText_password = findViewById(R.id.editText_password);
        editText_confirmar_password = findViewById(R.id.editText_confirmar_password);

        // TextInputLayouts for error display
        textInputLayout_usuario = findViewById(R.id.textInputLayout_usuario);
        textInputLayout_email = findViewById(R.id.textInputLayout_email);
        textInputLayout_password = findViewById(R.id.textInputLayout_password);
        textInputLayout_confirmar_password = findViewById(R.id.textInputLayout_confirmar_password);

        // Buttons and progress
        button_registro = findViewById(R.id.button_registro);
        progressBar_registro = findViewById(R.id.progressBar_registro);

        // Login link
        linearLayout_yacuenta = findViewById(R.id.linearLayout_yacuenta);
    }

    /**
     * Sets up text watchers to sync form fields with ViewModel.
     */
    private void setupTextWatchers() {
        editText_usuario.addTextChangedListener(new SimpleTextWatcher(value ->
            registerViewModel.setUsername(value)
        ));

        editText_email.addTextChangedListener(new SimpleTextWatcher(value ->
            registerViewModel.setEmail(value)
        ));

        editText_password.addTextChangedListener(new SimpleTextWatcher(value ->
            registerViewModel.setPassword(value)
        ));

        editText_confirmar_password.addTextChangedListener(new SimpleTextWatcher(value ->
            registerViewModel.setConfirmPassword(value)
        ));
    }

    /**
     * Sets up click listeners for buttons.
     */
    private void setupClickListeners() {
        // Registration button
        button_registro.setOnClickListener(v -> validateAndRegister());

        // Already have account - navigate to login
        linearLayout_yacuenta.setOnClickListener(v -> {
            Intent intent = new Intent(RegistroActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Observes ViewModel LiveData for state changes.
     */
    private void observeViewModel() {
        // Observe registration state
        registerViewModel.getRegistrationState().observe(this, state -> {
            if (state == RegisterViewModel.RegistrationState.LOADING) {
                showLoading(true);
            } else if (state == RegisterViewModel.RegistrationState.SUCCESS) {
                showLoading(false);
                Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show();
                navigateToMain();
            } else if (state == RegisterViewModel.RegistrationState.ERROR) {
                showLoading(false);
            }
        });

        // Observe error messages
        registerViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                registerViewModel.clearError();
            }
        });
    }

    /**
     * Validates inputs and initiates registration.
     */
    private void validateAndRegister() {
        // Clear previous errors
        clearErrors();

        String username = getTextValue(editText_usuario);
        String email = getTextValue(editText_email);
        String password = getTextValue(editText_password);
        String confirmPassword = getTextValue(editText_confirmar_password);

        boolean isValid = true;

        // Validate username
        if (!registerViewModel.validateUsername(username)) {
            textInputLayout_usuario.setError(getString(R.string.username_required));
            isValid = false;
        }

        // Validate email
        if (!registerViewModel.validateEmail(email)) {
            textInputLayout_email.setError(getString(R.string.email_invalid));
            isValid = false;
        }

        // Validate password
        if (!registerViewModel.validatePassword(password)) {
            textInputLayout_password.setError(getString(R.string.password_too_short));
            isValid = false;
        }

        // Validate confirm password
        if (!registerViewModel.validateConfirmPassword(password, confirmPassword)) {
            textInputLayout_confirmar_password.setError(getString(R.string.passwords_not_match));
            isValid = false;
        }

        if (isValid) {
            registerViewModel.register();
        }
    }

    /**
     * Shows or hides the loading indicator.
     */
    private void showLoading(boolean show) {
        progressBar_registro.setVisibility(show ? View.VISIBLE : View.GONE);
        button_registro.setEnabled(!show);
        editText_usuario.setEnabled(!show);
        editText_email.setEnabled(!show);
        editText_password.setEnabled(!show);
        editText_confirmar_password.setEnabled(!show);
    }

    /**
     * Clears all error displays.
     */
    private void clearErrors() {
        textInputLayout_usuario.setError(null);
        textInputLayout_email.setError(null);
        textInputLayout_password.setError(null);
        textInputLayout_confirmar_password.setError(null);
    }

    /**
     * Gets text value from EditText safely.
     */
    private String getTextValue(TextInputEditText editText) {
        Editable editable = editText.getText();
        return editable != null ? editable.toString().trim() : "";
    }

    /**
     * Navigates to MainActivity on successful registration.
     */
    private void navigateToMain() {
        Intent intent = new Intent(RegistroActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Simple TextWatcher implementation for single lambda support.
     */
    private static class SimpleTextWatcher implements TextWatcher {
        private final TextWatcherListener listener;

        interface TextWatcherListener {
            void onTextChanged(String value);
        }

        SimpleTextWatcher(TextWatcherListener listener) {
            this.listener = listener;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (listener != null) {
                listener.onTextChanged(s.toString());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}