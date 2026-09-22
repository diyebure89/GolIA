package com.diyebure.golia;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.diyebure.golia.domain.validation.ValidationError;
import com.diyebure.golia.domain.validation.ValidationResult;
import com.diyebure.golia.ui.auth.RegisterViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Registration activity for user signup.
 * Handles user registration form validation and submission.
 *
 * <p>Annotated with {@code @AndroidEntryPoint} so Hilt can supply the
 * {@link RegisterViewModel}. The Activity only handles view wiring, validation
 * feedback and navigation; account creation flows through the ViewModel.
 */
@AndroidEntryPoint
public class RegistroActivity extends AppCompatActivity {

    private RegisterViewModel registerViewModel;

    // Form fields
    private TextInputEditText editText_nombre;
    private TextInputEditText editText_usuario;
    private TextInputEditText editText_email;
    private TextInputEditText editText_password;
    private TextInputEditText editText_confirmar_password;

    // TextInputLayouts for error display
    private TextInputLayout textInputLayout_nombre;
    private TextInputLayout textInputLayout_usuario;
    private TextInputLayout textInputLayout_email;
    private TextInputLayout textInputLayout_password;
    private TextInputLayout textInputLayout_confirmar_password;

    // Buttons
    private Button button_registro;
    private Button button2;

    // Progress indicator
    private ProgressBar progressBar_registro;

    // Login link
    private LinearLayout linearLayout_yacuenta;
    private TextView textView_iniciar_sesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        // Initialize ViewModel
        registerViewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        // Initialize views
        initializeViews();
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
        textInputLayout_nombre = findViewById(R.id.textInputLayout_nombre);
        textInputLayout_usuario = findViewById(R.id.textInputLayout_usuario);
        textInputLayout_email = findViewById(R.id.textInputLayout_email);
        textInputLayout_password = findViewById(R.id.textInputLayout_password);
        textInputLayout_confirmar_password = findViewById(R.id.textInputLayout_confirmar_password);

        // Buttons and progress
        button_registro = findViewById(R.id.button_registro);
        button2 = findViewById(R.id.button2);
        progressBar_registro = findViewById(R.id.progressBar_registro);

        // Login link
        linearLayout_yacuenta = findViewById(R.id.linearLayout_yacuenta);
        textView_iniciar_sesion = findViewById(R.id.textView_iniciar_sesion);
    }

    /**
     * Sets up click listeners for buttons.
     */
    private void setupClickListeners() {
        // Registration button
        button_registro.setOnClickListener(v -> submitRegistration());

        // Already have account - navigate to login (tap the "Inicia sesión" link
        // inside linearLayout_yacuenta). The whole row is kept tappable too for a
        // larger touch target.
        textView_iniciar_sesion.setOnClickListener(v -> navigateToLogin());
        linearLayout_yacuenta.setOnClickListener(v -> navigateToLogin());

        // Back button returns to the previous screen.
        button2.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    /**
     * Observes ViewModel LiveData for state changes.
     */
    private void observeViewModel() {
        // Loading state blocks input and shows progress (survives rotation).
        registerViewModel.getLoading().observe(this, this::showLoading);

        // Per-field validation errors (one-shot).
        registerViewModel.getFieldErrors().observe(this, event -> {
            if (event == null) {
                return;
            }
            Map<ValidationResult.Field, ValidationError> errors = event.getContentIfNotHandled();
            if (errors != null) {
                showFieldErrors(errors);
            }
        });

        // Success (one-shot): registration does NOT auto-login; go to Login.
        registerViewModel.getRegisterSuccess().observe(this, event -> {
            if (event == null) {
                return;
            }
            Boolean handled = event.getContentIfNotHandled();
            if (handled != null) {
                Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        });

        // Error (one-shot): mapped string resource id.
        registerViewModel.getErrorMessage().observe(this, event -> {
            if (event == null) {
                return;
            }
            Integer messageRes = event.getContentIfNotHandled();
            if (messageRes != null) {
                Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Reads the form values and delegates registration to the ViewModel.
     */
    private void submitRegistration() {
        clearErrors();
        registerViewModel.register(
                getTextValue(editText_nombre),
                getTextValue(editText_usuario),
                getTextValue(editText_email),
                getTextValue(editText_password),
                getTextValue(editText_confirmar_password));
    }

    /**
     * Displays per-field validation errors on their {@link TextInputLayout}s.
     */
    private void showFieldErrors(Map<ValidationResult.Field, ValidationError> errors) {
        clearErrors();
        for (Map.Entry<ValidationResult.Field, ValidationError> entry : errors.entrySet()) {
            String message = getString(messageFor(entry.getValue()));
            switch (entry.getKey()) {
                case FULL_NAME:
                    textInputLayout_nombre.setError(message);
                    break;
                case USERNAME:
                    textInputLayout_usuario.setError(message);
                    break;
                case EMAIL:
                    textInputLayout_email.setError(message);
                    break;
                case PASSWORD:
                    textInputLayout_password.setError(message);
                    break;
                case CONFIRM_PASSWORD:
                    textInputLayout_confirmar_password.setError(message);
                    break;
            }
        }
    }

    /**
     * Maps a typed {@link ValidationError} to its specific user-facing string
     * resource. Each enum value has a dedicated message so the field error is
     * precise (R12.1, R12.2, R12.3, R15.2).
     */
    @StringRes
    private int messageFor(ValidationError error) {
        switch (error) {
            // Full name (R1)
            case FULL_NAME_REQUIRED:
                return R.string.fullname_required;
            case FULL_NAME_TOO_SHORT:
                return R.string.fullname_too_short;
            case FULL_NAME_TOO_LONG:
                return R.string.fullname_too_long;
            // Username (R2)
            case USERNAME_TOO_SHORT:
                return R.string.username_too_short;
            case USERNAME_TOO_LONG:
                return R.string.username_too_long;
            case USERNAME_FORMAT:
                return R.string.username_format;
            // Email (R3)
            case EMAIL_REQUIRED:
                return R.string.email_required;
            case EMAIL_INVALID:
                return R.string.email_invalid;
            // Password (R4)
            case PASSWORD_REQUIRED:
                return R.string.password_required;
            case PASSWORD_TOO_SHORT:
                return R.string.password_too_short;
            case PASSWORD_NO_UPPER:
                return R.string.password_no_upper;
            case PASSWORD_NO_LOWER:
                return R.string.password_no_lower;
            case PASSWORD_NO_DIGIT:
                return R.string.password_no_digit;
            // Confirm password (R5)
            case CONFIRM_REQUIRED:
                return R.string.confirm_required;
            case CONFIRM_MISMATCH:
                return R.string.confirm_mismatch;
            default:
                return R.string.registration_error;
        }
    }

    /**
     * Shows or hides the loading indicator and blocks the form while loading.
     */
    private void showLoading(boolean show) {
        progressBar_registro.setVisibility(show ? View.VISIBLE : View.GONE);
        button_registro.setEnabled(!show);
        editText_nombre.setEnabled(!show);
        editText_usuario.setEnabled(!show);
        editText_email.setEnabled(!show);
        editText_password.setEnabled(!show);
        editText_confirmar_password.setEnabled(!show);
    }

    /**
     * Clears all error displays.
     */
    private void clearErrors() {
        textInputLayout_nombre.setError(null);
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
        return editable != null ? editable.toString() : "";
    }

    /**
     * Navigates to the login screen (registration does not auto-login).
     */
    private void navigateToLogin() {
        Intent intent = new Intent(RegistroActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
