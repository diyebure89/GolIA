package com.diyebure.golia;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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
    private Button button_ingresar;
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
        button_ingresar = findViewById(R.id.button_ingresar);
    }

    private void setupListeners() {
        text_crear_cuenta.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegistroActivity.class)));

        button_ingresar.setOnClickListener(v -> {
            String email = input_correo != null && input_correo.getText() != null
                    ? input_correo.getText().toString().trim() : "";
            String password = input_contrasena != null && input_contrasena.getText() != null
                    ? input_contrasena.getText().toString() : "";
            loginViewModel.login(email, password);
        });
    }

    private void observeViewModel() {
        loginViewModel.getLoginState().observe(this, state -> {
            if (state == null) {
                return;
            }
            switch (state) {
                case LOADING:
                    button_ingresar.setEnabled(false);
                    break;
                case SUCCESS:
                    button_ingresar.setEnabled(true);
                    navigateToHome();
                    break;
                case ERROR:
                    button_ingresar.setEnabled(true);
                    break;
                case IDLE:
                default:
                    button_ingresar.setEnabled(true);
                    break;
            }
        });

        loginViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
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
