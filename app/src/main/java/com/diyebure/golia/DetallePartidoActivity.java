package com.diyebure.golia;

import android.content.Intent;
import android.os.Bundle;
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
import com.diyebure.golia.presentation.ui.match.DetallePartidoViewModel;
import com.diyebure.golia.presentation.ui.match.MatchDetailUiModel;
import com.diyebure.golia.presentation.ui.match.PredictButtonState;
import com.diyebure.golia.presentation.ui.match.SeasonStatsUiModel;
import com.diyebure.golia.util.Constants;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Match detail screen (R1, R2, R3, R4).
 *
 * <p>{@code @AndroidEntryPoint} so Hilt can inject the
 * {@link DetallePartidoViewModel}. The Activity is a thin view: it reads the
 * {@link Constants#EXTRA_MATCH_ID} extra, asks the ViewModel to load, and renders
 * the {@link androidx.lifecycle.LiveData} state. It performs no data access.</p>
 */
@AndroidEntryPoint
public class DetallePartidoActivity extends AppCompatActivity {

    private DetallePartidoViewModel viewModel;

    private ImageButton buttonBack;
    private TextView textLeagueRound;
    private ImageView imageHomeLogo;
    private ImageView imageAwayLogo;
    private TextView textHomeName;
    private TextView textAwayName;
    private TextView textScore;
    private TextView textElapsed;
    private TextView textStatus;
    private TextView textKickoff;
    private TextView textHomeWins;
    private TextView textAwayWins;
    private ProgressBar barHomeWins;
    private ProgressBar barAwayWins;
    private TextView textHomeAvg;
    private TextView textAwayAvg;
    private ProgressBar barHomeAvg;
    private ProgressBar barAwayAvg;
    private TextView textHomeForm;
    private TextView textAwayForm;
    private ProgressBar barHomeForm;
    private ProgressBar barAwayForm;
    private TextView textPredictionClosed;
    private Button buttonPronostico;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_partido);

        viewModel = new ViewModelProvider(this).get(DetallePartidoViewModel.class);

        bindViews();
        setupListeners();
        observeViewModel();
        applyWindowInsets();

        String matchId = getIntent() != null
                ? getIntent().getStringExtra(Constants.EXTRA_MATCH_ID)
                : null;
        viewModel.load(matchId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-evaluate the prediction lock threshold: a match may have crossed the
        // 10-minute window while this screen was open (R4.3).
        viewModel.reevaluateButtonState();
    }

    private void bindViews() {
        buttonBack = findViewById(R.id.button_back);
        textLeagueRound = findViewById(R.id.text_league_round);
        imageHomeLogo = findViewById(R.id.image_home_logo);
        imageAwayLogo = findViewById(R.id.image_away_logo);
        textHomeName = findViewById(R.id.text_home_name);
        textAwayName = findViewById(R.id.text_away_name);
        textScore = findViewById(R.id.text_score);
        textElapsed = findViewById(R.id.text_elapsed);
        textStatus = findViewById(R.id.text_status);
        textKickoff = findViewById(R.id.text_kickoff);
        textHomeWins = findViewById(R.id.text_home_wins);
        textAwayWins = findViewById(R.id.text_away_wins);
        barHomeWins = findViewById(R.id.bar_home_wins);
        barAwayWins = findViewById(R.id.bar_away_wins);
        textHomeAvg = findViewById(R.id.text_home_avg);
        textAwayAvg = findViewById(R.id.text_away_avg);
        barHomeAvg = findViewById(R.id.bar_home_avg);
        barAwayAvg = findViewById(R.id.bar_away_avg);
        textHomeForm = findViewById(R.id.text_home_form);
        textAwayForm = findViewById(R.id.text_away_form);
        barHomeForm = findViewById(R.id.bar_home_form);
        barAwayForm = findViewById(R.id.bar_away_form);
        textPredictionClosed = findViewById(R.id.text_prediction_closed);
        buttonPronostico = findViewById(R.id.button_pronostico);
        progressBar = findViewById(R.id.progressBar_detalle);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
        buttonPronostico.setOnClickListener(v -> viewModel.onPredictClicked());
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, this::showLoading);

        viewModel.getDetail().observe(this, this::bindDetail);

        viewModel.getSeasonStats().observe(this, this::bindSeasonStats);

        viewModel.getButtonState().observe(this, this::bindButtonState);

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

        viewModel.getNavigateToPrediction().observe(this, event -> {
            if (event == null) {
                return;
            }
            String matchId = event.getContentIfNotHandled();
            if (matchId != null) {
                navigateToPrediction(matchId);
            }
        });
    }

    private void bindDetail(MatchDetailUiModel model) {
        if (model == null) {
            return;
        }
        textLeagueRound.setText(buildLeagueRound(model.leagueName, model.round));
        textHomeName.setText(model.homeName);
        textAwayName.setText(model.awayName);
        textStatus.setText(model.statusLabel);
        textKickoff.setText(model.kickoffLocal);

        if (model.scoreText != null) {
            textScore.setVisibility(View.VISIBLE);
            textScore.setText(model.scoreText);
        } else {
            textScore.setVisibility(View.GONE);
        }

        if (model.elapsedText != null) {
            textElapsed.setVisibility(View.VISIBLE);
            textElapsed.setText(model.elapsedText);
        } else {
            textElapsed.setVisibility(View.GONE);
        }

        loadLogo(imageHomeLogo, model.homeLogoUrl);
        loadLogo(imageAwayLogo, model.awayLogoUrl);
    }

    private void bindSeasonStats(SeasonStatsUiModel model) {
        if (model == null) {
            return;
        }
        textHomeWins.setText(String.valueOf(model.homeWins));
        textAwayWins.setText(String.valueOf(model.awayWins));
        setBars(barHomeWins, barAwayWins, model.winsRatio);

        textHomeAvg.setText(formatAvg(model.homeAvg));
        textAwayAvg.setText(formatAvg(model.awayAvg));
        setBars(barHomeAvg, barAwayAvg, model.avgRatio);

        textHomeForm.setText(String.valueOf(model.homeFormV));
        textAwayForm.setText(String.valueOf(model.awayFormV));
        setBars(barHomeForm, barAwayForm, model.formRatio);
    }

    private void bindButtonState(PredictButtonState state) {
        if (state == null) {
            return;
        }
        switch (state) {
            case ENABLED_NEW:
                buttonPronostico.setEnabled(true);
                buttonPronostico.setText(R.string.detalle_hacer_pronostico);
                textPredictionClosed.setVisibility(View.GONE);
                break;
            case ENABLED_EDIT:
                buttonPronostico.setEnabled(true);
                buttonPronostico.setText(R.string.detalle_editar_pronostico);
                textPredictionClosed.setVisibility(View.GONE);
                break;
            case VIEW_ONLY:
                buttonPronostico.setEnabled(true);
                buttonPronostico.setText(R.string.detalle_ver_pronostico);
                textPredictionClosed.setVisibility(View.VISIBLE);
                break;
            case CLOSED:
            default:
                buttonPronostico.setEnabled(false);
                buttonPronostico.setText(R.string.detalle_hacer_pronostico);
                textPredictionClosed.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToPrediction(@NonNull String matchId) {
        Intent intent = new Intent(this, PronosticoActivity.class);
        intent.putExtra(Constants.EXTRA_MATCH_ID, matchId);
        startActivity(intent);
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

    /**
     * Splits the home/away shares of a metric across the two opposing bars. The
     * home bar (mirrored in the layout) shows the home share, the away bar the
     * complementary share.
     */
    private static void setBars(ProgressBar homeBar, ProgressBar awayBar, double homeRatio) {
        int home = (int) Math.round(clamp(homeRatio) * 100.0);
        homeBar.setProgress(home);
        awayBar.setProgress(100 - home);
    }

    private static double clamp(double ratio) {
        if (ratio < 0) {
            return 0;
        }
        if (ratio > 1) {
            return 1;
        }
        return ratio;
    }

    private static String formatAvg(double value) {
        return String.format(java.util.Locale.getDefault(), "%.1f", value);
    }

    private String buildLeagueRound(String league, String round) {
        String safeLeague = league == null ? "" : league;
        if (round == null || round.isEmpty()) {
            return safeLeague;
        }
        if (safeLeague.isEmpty()) {
            return round;
        }
        return safeLeague + " — " + round;
    }

    /**
     * Maps a typed {@code PredictionError} name emitted by the ViewModel to a
     * user-facing string resource. Falls back to a generic error message.
     */
    private String mapError(String errorName) {
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
