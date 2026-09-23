package com.diyebure.golia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.diyebure.golia.ui.auth.LoginViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Login screen.
 *
 * <p>Annotated with {@code @AndroidEntryPoint} so Hilt can inject the
 * {@link LoginViewModel} (and its use case graph). The Activity is a thin view:
 * it forwards user input to the ViewModel and reacts to its {@link LiveData}
 * state, without touching repositories or performing any authentication itself.
 */
@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private EditText input_correo;
    private EditText input_contrasena;
    private TextView text_crear_cuenta;
    private TextView text_recuperar_password;
    private Button button_ingresar;
    private ProgressBar progressBar_login;
    private LoginViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        bindViews();
        setupListeners();
        observeViewModel();
        applyWindowInsets();
    }

    private void bindViews() {
        input_correo = findViewById(R.id.input_correo);
        input_contrasena = findViewById(R.id.input_contrasena);
        text_crear_cuenta = findViewById(R.id.text_crear_cuenta);
        text_recuperar_password = findViewById(R.id.textView6);
        button_ingresar = findViewById(R.id.button_ingresar);
        progressBar_login = findViewById(R.id.progressBar_login);
    }

    private void setupListeners() {
        text_crear_cuenta.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegistroActivity.class)));

        text_recuperar_password.setOnClickListener(v ->
                Toast.makeText(LoginActivity.this, R.string.recuperar_password_no_disponible, Toast.LENGTH_SHORT).show());

        button_ingresar.setOnClickListener(v -> {
            String email = input_correo != null && input_correo.getText() != null
                    ? input_correo.getText().toString().trim() : "";
            String password = input_contrasena != null && input_contrasena.getText() != null
                    ? input_contrasena.getText().toString() : "";
            loginViewModel.login(email, password);
        });
    }

    private void observeViewModel() {
        // LOADING is plain LiveData: show the progress indicator and disable the
        // button while a request is in flight (survives rotation).
        loginViewModel.getLoading().observe(this, this::showLoading);

        // One-shot navigation to Home after a successful login (session already saved by the ViewModel).
        loginViewModel.getNavigateToHome().observe(this, event -> {
            if (event == null) {
                return;
            }
            Boolean go = event.getContentIfNotHandled();
            if (Boolean.TRUE.equals(go)) {
                navigateToHome();
            }
        });

        // One-shot error: global Toast with a mapped string resource id (does not reveal the field).
        loginViewModel.getErrorMessage().observe(this, event -> {
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
     * Shows or hides the loading indicator and disables the login button while a
     * request is in flight (R10.4). The button stays hidden behind the spinner so
     * the user cannot trigger a second submit.
     */
    private void showLoading(boolean show) {
        progressBar_login.setVisibility(show ? View.VISIBLE : View.GONE);
        button_ingresar.setEnabled(!show);
    }

    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class); // antes: HomeActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
