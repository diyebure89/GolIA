package com.diyebure.golia;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.diyebure.golia.domain.validation.ScoreFieldError;
import com.diyebure.golia.presentation.ui.match.PronosticoConfirmationArgs;
import com.diyebure.golia.presentation.ui.match.PronosticoHeaderUiModel;
import com.diyebure.golia.presentation.ui.match.PronosticoPrefill;
import com.diyebure.golia.presentation.ui.match.PronosticoViewModel;
import com.diyebure.golia.util.Constants;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Exact-score prediction form (R5).
 *
 * <p>{@code @AndroidEntryPoint} so Hilt can inject the
 * {@link PronosticoViewModel}. The Activity is a thin view: it reads the
 * {@link Constants#EXTRA_MATCH_ID} extra, asks the ViewModel to load the match
 * header and preload an existing prediction, and renders the
 * {@link androidx.lifecycle.LiveData} state. The screen shows only the two exact
 * score fields (local/visitante) &mdash; no 1X2 selection and no odds.</p>
 */
@AndroidEntryPoint
public class PronosticoActivity extends AppCompatActivity {

    private PronosticoViewModel viewModel;

    private ImageButton buttonBack;
    private ImageView imageHomeLogo;
    private ImageView imageAwayLogo;
    private TextView textHomeName;
    private TextView textAwayName;
    private TextView textKickoff;
    private TextInputLayout inputLayoutHome;
    private TextInputLayout inputLayoutAway;
    private TextInputEditText editHomeScore;
    private TextInputEditText editAwayScore;
    private Button buttonSubmit;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pronostico);

        viewModel = new ViewModelProvider(this).get(PronosticoViewModel.class);

        bindViews();
        setupListeners();
        observeViewModel();
        applyWindowInsets();

        String matchId = getIntent() != null
                ? getIntent().getStringExtra(Constants.EXTRA_MATCH_ID)
                : null;
        viewModel.load(matchId);
    }

    private void bindViews() {
        buttonBack = findViewById(R.id.button_back);
        imageHomeLogo = findViewById(R.id.image_home_logo);
        imageAwayLogo = findViewById(R.id.image_away_logo);
        textHomeName = findViewById(R.id.text_home_name);
        textAwayName = findViewById(R.id.text_away_name);
        textKickoff = findViewById(R.id.text_kickoff);
        inputLayoutHome = findViewById(R.id.input_layout_home_score);
        inputLayoutAway = findViewById(R.id.input_layout_away_score);
        editHomeScore = findViewById(R.id.edit_home_score);
        editAwayScore = findViewById(R.id.edit_away_score);
        buttonSubmit = findViewById(R.id.button_submit);
        progressBar = findViewById(R.id.progressBar_pronostico);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
        buttonSubmit.setOnClickListener(v -> viewModel.submit(
                textOf(editHomeScore), textOf(editAwayScore)));
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, this::showLoading);

        viewModel.getHeader().observe(this, this::bindHeader);

        viewModel.getPrefill().observe(this, this::bindPrefill);

        viewModel.getHomeFieldError().observe(this, error ->
                inputLayoutHome.setError(mapFieldError(error)));

        viewModel.getAwayFieldError().observe(this, error ->
                inputLayoutAway.setError(mapFieldError(error)));

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, mapError(message), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getNavigateToLogin().observe(this, event -> {
            if (event == null) {
                return;
            }
            Boolean go = event.getContentIfNotHandled();
            if (Boolean.TRUE.equals(go)) {
                navigateToLogin();
            }
        });

        viewModel.getNavigateToConfirmation().observe(this, event -> {
            if (event == null) {
                return;
            }
            PronosticoConfirmationArgs args = event.getContentIfNotHandled();
            if (args != null) {
                navigateToConfirmation(args);
            }
        });
    }

    private void bindHeader(PronosticoHeaderUiModel model) {
        if (model == null) {
            return;
        }
        textHomeName.setText(model.homeName);
        textAwayName.setText(model.awayName);
        textKickoff.setText(model.kickoffLocal);
        loadLogo(imageHomeLogo, model.homeLogoUrl);
        loadLogo(imageAwayLogo, model.awayLogoUrl);
    }

    private void bindPrefill(PronosticoPrefill prefill) {
        if (prefill == null) {
            return;
        }
        editHomeScore.setText(String.valueOf(prefill.homeScore));
        editAwayScore.setText(String.valueOf(prefill.awayScore));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        // Anti double-submit: disable the control while the save is in flight (R5.10).
        buttonSubmit.setEnabled(!show);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigates to {@link ConfirmacionActivity} carrying the match id, team
     * names, predicted score, preformatted date and possible points (R5.11).
     * Finishes so "back" returns to the match detail, not the form.
     */
    private void navigateToConfirmation(@NonNull PronosticoConfirmationArgs args) {
        Intent intent = new Intent(this, ConfirmacionActivity.class);
        intent.putExtra(Constants.EXTRA_MATCH_ID, args.matchId);
        intent.putExtra(Constants.EXTRA_HOME_NAME, args.homeName);
        intent.putExtra(Constants.EXTRA_AWAY_NAME, args.awayName);
        intent.putExtra(Constants.EXTRA_PREDICTED_HOME_SCORE, args.homeScore);
        intent.putExtra(Constants.EXTRA_PREDICTED_AWAY_SCORE, args.awayScore);
        intent.putExtra(Constants.EXTRA_MATCH_DATE, args.matchDate);
        intent.putExtra(Constants.EXTRA_POSSIBLE_POINTS, Constants.PREDICTION_MAX_POINTS);
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

    private void loadLogo(ImageView target, String url) {
        if (target == null) {
            return;
        }
        Glide.with(target.getContext())
                .load(url)
                .placeholder(R.drawable.ic_team_placeholder)
                .error(R.drawable.ic_team_placeholder)
                .fallback(R.drawable.ic_team_placeholder)
                .into(target);
    }

    private static String textOf(TextInputEditText field) {
        CharSequence text = field.getText();
        return text == null ? "" : text.toString();
    }

    /**
     * Maps a per-field {@link ScoreFieldError} to a localized message, or
     * {@code null} to clear the error on the field.
     */
    private String mapFieldError(ScoreFieldError error) {
        if (error == null) {
            return null;
        }
        switch (error) {
            case REQUIRED:
                return getString(R.string.pronostico_error_requerido);
            case OUT_OF_RANGE:
            default:
                return getString(R.string.pronostico_error_rango);
        }
    }

    /**
     * Maps a typed {@code PredictionError} name emitted by the ViewModel to a
     * user-facing string resource. Falls back to a generic error message.
     */
    private String mapError(String errorName) {
        if (TextUtils.isEmpty(errorName)) {
            return getString(R.string.error_generic);
        }
        switch (errorName) {
            case "MATCH_NOT_FOUND":
                return getString(R.string.error_match_not_found);
            case "SESSION_UNAVAILABLE":
                return getString(R.string.error_session_unavailable);
            case "PREDICTION_CLOSED":
                return getString(R.string.detalle_pronostico_cerrado);
            case "PERSISTENCE_ERROR":
                return getString(R.string.error_persistence);
            default:
                return getString(R.string.error_generic);
        }
    }
}
