package com.diyebure.golia;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.diyebure.golia.domain.error.AuthError;
import com.diyebure.golia.domain.error.AuthException;
import com.diyebure.golia.domain.model.RankingSnapshot;
import com.diyebure.golia.domain.validation.ValidationError;
import com.diyebure.golia.presentation.ProfileFormState;
import com.diyebure.golia.util.DisplayName;
import com.diyebure.golia.presentation.viewmodel.PerfilViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment de la {@code Pantalla_Perfil}.
 *
 * <p>Capa de vista fina anotada con {@code @AndroidEntryPoint} para que Hilt pueda
 * inyectar el {@link PerfilViewModel}. Observa el {@link androidx.lifecycle.LiveData}
 * de estado (form, avatar, ranking, carga) y los {@link com.diyebure.golia.presentation.util.Event}
 * de un solo consumo (navegación, éxito, error, timeout), traduce el {@link AuthError}
 * tipado a recursos de string mediante {@link #messageFor(Exception)} y muestra errores
 * por campo con {@link TextInputLayout#setError}. No accede a repositorios ni a la BD
 * (R1, R2, R3, R4, R6, R7, R8, R9, R11, R13, R14).</p>
 */
@AndroidEntryPoint
public class PerfilFragment extends Fragment {

    private PerfilViewModel viewModel;

    // Avatar + editar foto
    private ShapeableImageView imageAvatar;
    private FloatingActionButton buttonEditPhoto;

    // Datos personales
    private TextView textFullName;
    private TextInputLayout inputLayoutEmail;
    private TextInputEditText editEmail;
    private TextInputLayout inputLayoutUsername;
    private TextInputEditText editUsername;
    private Button buttonSaveChanges;

    // Cambio de contraseña
    private TextInputLayout inputLayoutCurrentPassword;
    private TextInputEditText editCurrentPassword;
    private TextInputLayout inputLayoutNewPassword;
    private TextInputEditText editNewPassword;
    private TextInputLayout inputLayoutConfirmPassword;
    private TextInputEditText editConfirmPassword;
    private Button buttonChangePassword;

    // Historial (ranking)
    private TextView textPredictions;
    private TextView textAccuracy;
    private TextView textPoints;
    private TextView textRanking;

    // Sesión + carga
    private Button buttonLogout;
    private ProgressBar progressBar;

    /** URI del {@link FileProvider} del archivo destino de la captura de cámara en curso. */
    private Uri pendingCameraUri;

    // ==================== ActivityResult launchers ====================

    /** Galería: devuelve un {@code content://} de la imagen elegida (o {@code null} si se cancela). */
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    viewModel.updatePhoto(uri);
                }
                // Cancelar (uri == null) => sin cambios (R8.2).
            });

    /** Cámara: {@code true} si la foto se guardó en {@link #pendingCameraUri}. */
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success) && pendingCameraUri != null) {
                    viewModel.updatePhoto(pendingCameraUri);
                }
                pendingCameraUri = null;
                // Cancelar (success == false) => sin cambios (R8.2).
            });

    /** Permiso de cámara: gestiona concesión, denegación simple y denegación permanente (R8.4, R8.5). */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    launchCamera();
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    // Denegación simple: aún se puede volver a pedir (R8.4).
                    toast(R.string.perfil_permiso_camara_denegado);
                } else {
                    // Denegación permanente: hay que habilitarlo en ajustes (R8.5).
                    toast(R.string.perfil_permiso_camara_ajustes);
                }
            });

    public PerfilFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PerfilViewModel.class);

        bindViews(view);
        setupListeners();
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Carga/valida la sesión al mostrar la pantalla (R1). El anti-doble-submit del
        // ViewModel ignora una segunda llamada si la carga sigue en curso.
        viewModel.load();
    }

    // ==================== View binding ====================

    private void bindViews(View view) {
        imageAvatar = view.findViewById(R.id.imageAvatar);
        buttonEditPhoto = view.findViewById(R.id.buttonEditPhoto);

        textFullName = view.findViewById(R.id.textFullName);
        inputLayoutEmail = view.findViewById(R.id.inputLayoutEmail);
        editEmail = view.findViewById(R.id.editEmail);
        inputLayoutUsername = view.findViewById(R.id.inputLayoutUsername);
        editUsername = view.findViewById(R.id.editUsername);
        buttonSaveChanges = view.findViewById(R.id.buttonSaveChanges);

        inputLayoutCurrentPassword = view.findViewById(R.id.inputLayoutCurrentPassword);
        editCurrentPassword = view.findViewById(R.id.editCurrentPassword);
        inputLayoutNewPassword = view.findViewById(R.id.inputLayoutNewPassword);
        editNewPassword = view.findViewById(R.id.editNewPassword);
        inputLayoutConfirmPassword = view.findViewById(R.id.inputLayoutConfirmPassword);
        editConfirmPassword = view.findViewById(R.id.editConfirmPassword);
        buttonChangePassword = view.findViewById(R.id.buttonChangePassword);

        textPredictions = view.findViewById(R.id.textPredictions);
        textAccuracy = view.findViewById(R.id.textAccuracy);
        textPoints = view.findViewById(R.id.textPoints);
        textRanking = view.findViewById(R.id.textRanking);

        buttonLogout = view.findViewById(R.id.buttonLogout);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        buttonEditPhoto.setOnClickListener(v -> showPhotoChooser());

        buttonSaveChanges.setOnClickListener(v ->
                viewModel.savePersonalData(text(editUsername), text(editEmail)));

        buttonChangePassword.setOnClickListener(v ->
                viewModel.changePassword(text(editCurrentPassword),
                        text(editNewPassword), text(editConfirmPassword)));

        buttonLogout.setOnClickListener(v -> viewModel.logout());
    }

    // ==================== Observación de estado y eventos ====================

    private void observeViewModel() {
        viewModel.getFormState().observe(getViewLifecycleOwner(), this::renderFormState);
        viewModel.getAvatarPreviewPath().observe(getViewLifecycleOwner(), this::renderAvatar);
        viewModel.getRanking().observe(getViewLifecycleOwner(), this::renderRanking);

        // Carga: muestra/oculta el progress y bloquea los botones (anti-doble-submit, R13.1).
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::showLoading);

        // Navegación de un solo consumo.
        viewModel.getNavigationEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) {
                return;
            }
            PerfilViewModel.NavTarget target = event.getContentIfNotHandled();
            if (target == PerfilViewModel.NavTarget.LOGIN) {
                navigateToLogin();
            }
        });

        // Éxito de un solo consumo: Toast corto por operación.
        viewModel.getSuccessEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) {
                return;
            }
            PerfilViewModel.UiMessage message = event.getContentIfNotHandled();
            if (message != null) {
                onSuccess(message);
            }
        });

        // Error de un solo consumo: traduce AuthError -> string y muestra Toast.
        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) {
                return;
            }
            AuthError error = event.getContentIfNotHandled();
            if (error != null) {
                toast(messageFor(new AuthException(error)));
            }
        });

        // Timeout de un solo consumo: mensaje de timeout y controles reactivados por isLoading.
        viewModel.getTimeoutEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) {
                return;
            }
            PerfilViewModel.Operation op = event.getContentIfNotHandled();
            if (op != null) {
                toast(R.string.error_timeout);
            }
        });
    }

    /**
     * Rellena el nombre completo (solo lectura), correo y usuario, y muestra los
     * errores por campo. Solo se sobreescribe el texto del campo cuando difiere del
     * valor del estado, para no interferir con la edición en curso ni mover el cursor
     * innecesariamente; los datos editados se conservan en error (R6.7, R13.4).
     */
    private void renderFormState(ProfileFormState state) {
        if (state == null) {
            return;
        }
        // Prioriza el nombre de usuario; si no existe, usa el nombre completo (fallback vacio).
        textFullName.setText(DisplayName.resolve(state.getUsername(), state.getFullName(), ""));

        setTextIfChanged(editEmail, state.getEmail());
        setTextIfChanged(editUsername, state.getUsername());

        inputLayoutEmail.setError(errorText(state.getEmailError()));
        inputLayoutUsername.setError(errorText(state.getUsernameError()));
    }

    /**
     * Carga la imagen del avatar desde su ruta absoluta en {@code filesDir}. Si la
     * ruta es nula o el archivo no existe, muestra el avatar por defecto sin error
     * (R1.7, R9.5, R9.6, R9.7).
     */
    private void renderAvatar(String absolutePath) {
        if (!TextUtils.isEmpty(absolutePath)) {
            File file = new File(absolutePath);
            if (file.exists()) {
                // El nombre de archivo es determinista (mismo URI al reemplazar la
                // foto). Descartar el drawable actual fuerza a ImageView a releer el
                // bitmap y no servir la versión cacheada.
                imageAvatar.setImageDrawable(null);
                imageAvatar.setImageURI(Uri.fromFile(file));
                return;
            }
        }
        imageAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
    }

    /**
     * Rellena las cuatro métricas del Historial desde el {@link RankingSnapshot}
     * compartido con Inicio (proveniente del {@code RankingDataSource}): pronósticos,
     * aciertos %, puntos y ranking. El porcentaje de aciertos se formatea con 1
     * decimal. Cuando no hay métricas (snapshot nulo), muestra un estado neutro con
     * valores placeholder y SIN mostrar ningún error al usuario (R11.1, R11.2, R11.5).
     */
    private void renderRanking(RankingSnapshot ranking) {
        if (ranking == null) {
            // Sin métricas: estado neutro/placeholder, nunca un mensaje de error (R11.5).
            textPredictions.setText(R.string.perfil_stat_placeholder_valor);
            textAccuracy.setText(R.string.perfil_stat_placeholder_porcentaje);
            textPoints.setText(R.string.perfil_stat_placeholder_valor);
            textRanking.setText(R.string.perfil_stat_placeholder_ranking);
            return;
        }
        textPredictions.setText(String.valueOf(ranking.getPredictionsMade()));
        textAccuracy.setText(String.format(Locale.getDefault(), "%.1f%%", ranking.getAccuracyPercentage()));
        textPoints.setText(String.valueOf(ranking.getTotalPoints()));
        textRanking.setText(String.format(Locale.getDefault(), "#%d", ranking.getRankingPosition()));
    }

    private void onSuccess(PerfilViewModel.UiMessage message) {
        switch (message) {
            case PERSONAL_DATA_SAVED:
                toast(R.string.perfil_exito_datos);
                break;
            case PASSWORD_CHANGED:
                // Limpia los tres campos de contraseña tras un cambio correcto (R7.13).
                clearPasswordFields();
                toast(R.string.perfil_exito_password);
                break;
            case PHOTO_UPDATED:
                toast(R.string.perfil_exito_foto);
                break;
        }
    }

    // ==================== Selector de foto y cámara (R8) ====================

    /** Muestra un selector con exactamente dos opciones: galería y cámara (R8.1). */
    private void showPhotoChooser() {
        CharSequence[] options = {
                getString(R.string.perfil_foto_galeria),
                getString(R.string.perfil_foto_camara)
        };
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.perfil_foto_titulo)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        galleryLauncher.launch("image/*");
                    } else {
                        requestCamera();
                    }
                })
                .show();
    }

    /** Comprueba el permiso de cámara y, si ya está concedido, abre la cámara (R8.4). */
    private void requestCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Crea un archivo destino en {@code filesDir/images/}, obtiene su {@link FileProvider}
     * URI (autoridad {@code ${applicationId}.fileprovider}) y lanza la captura (R8.3).
     */
    private void launchCamera() {
        try {
            File imagesDir = new File(requireContext().getFilesDir(), "images");
            if (!imagesDir.exists() && !imagesDir.mkdirs()) {
                toast(R.string.perfil_foto_error);
                return;
            }
            File destination = new File(imagesDir, "camera_capture.jpg");
            String authority = requireContext().getPackageName() + ".fileprovider";
            pendingCameraUri = FileProvider.getUriForFile(requireContext(), authority, destination);
            cameraLauncher.launch(pendingCameraUri);
        } catch (IllegalArgumentException | SecurityException e) {
            pendingCameraUri = null;
            toast(R.string.perfil_foto_error);
        }
    }

    // ==================== Navegación ====================

    /**
     * Navega a {@link LoginActivity} tras un cierre de sesión correcto (o cuando la
     * sesión no es válida). Establece {@link Intent#FLAG_ACTIVITY_NEW_TASK} |
     * {@link Intent#FLAG_ACTIVITY_CLEAR_TASK} para vaciar la pila de la sesión
     * cerrada, de modo que el botón atrás no regrese al Perfil (R12.4).
     *
     * <p>El fallo de logout ({@code SESSION_CLEAR_FAILED}) no llega aquí: el
     * {@code PerfilViewModel} lo emite por el {@code errorEvent}, así que la vista
     * permanece en Perfil y muestra el mensaje de error correspondiente (R12.5).
     */
    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    // ==================== Loading ====================

    /**
     * Muestra u oculta el indicador de carga y deshabilita los botones de acción
     * mientras hay una operación en curso, evitando envíos duplicados (R13.1).
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonSaveChanges.setEnabled(!show);
        buttonChangePassword.setEnabled(!show);
        buttonLogout.setEnabled(!show);
        buttonEditPhoto.setEnabled(!show);
    }

    // ==================== Mapeo de errores ====================

    /**
     * Traduce un {@link AuthError} tipado (transportado por {@link AuthException})
     * a su recurso de string localizado, según la tabla del diseño (R13.6). Cualquier
     * excepción que no sea {@link AuthException} se trata como error de persistencia.
     */
    @StringRes
    private int messageFor(Exception ex) {
        if (ex instanceof AuthException) {
            AuthError error = ((AuthException) ex).getAuthError();
            if (error != null) {
                switch (error) {
                    case USERNAME_TAKEN:
                        return R.string.error_username_taken;
                    case EMAIL_TAKEN:
                        return R.string.error_email_taken;
                    case INVALID_CREDENTIALS:
                        return R.string.error_current_password_incorrect;
                    case SESSION_UNAVAILABLE:
                        return R.string.error_session_unavailable;
                    case SESSION_CLEAR_FAILED:
                        return R.string.error_session_clear_failed;
                    case VALIDATION_ERROR:
                        return R.string.error_validation;
                    case PERSISTENCE_ERROR:
                    default:
                        return R.string.error_persistence;
                }
            }
        }
        return R.string.error_persistence;
    }

    /**
     * Traduce un {@link ValidationError} por campo a su recurso de string localizado
     * para mostrarlo con {@link TextInputLayout#setError}. Devuelve {@code null}
     * cuando el campo es válido, lo que limpia el error del layout.
     */
    @Nullable
    private CharSequence errorText(@Nullable ValidationError error) {
        if (error == null) {
            return null;
        }
        return getString(fieldMessageFor(error));
    }

    @StringRes
    private int fieldMessageFor(ValidationError error) {
        switch (error) {
            case USERNAME_TOO_SHORT:
                return R.string.username_too_short;
            case USERNAME_TOO_LONG:
                return R.string.username_too_long;
            case USERNAME_FORMAT:
                return R.string.username_format;
            case EMAIL_REQUIRED:
                return R.string.email_required;
            case EMAIL_INVALID:
                return R.string.email_invalid;
            default:
                return R.string.error_validation;
        }
    }

    // ==================== Helpers ====================

    private void clearPasswordFields() {
        editCurrentPassword.setText("");
        editNewPassword.setText("");
        editConfirmPassword.setText("");
        inputLayoutCurrentPassword.setError(null);
        inputLayoutNewPassword.setError(null);
        inputLayoutConfirmPassword.setError(null);
    }

    private static String text(TextInputEditText field) {
        return field != null && field.getText() != null ? field.getText().toString() : "";
    }

    /** Solo actualiza el texto si difiere, para no reposicionar el cursor durante la edición. */
    private static void setTextIfChanged(TextInputEditText field, String value) {
        String current = text(field);
        String next = value == null ? "" : value;
        if (!current.equals(next)) {
            field.setText(next);
        }
    }

    private void toast(@StringRes int messageRes) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
    }
}
