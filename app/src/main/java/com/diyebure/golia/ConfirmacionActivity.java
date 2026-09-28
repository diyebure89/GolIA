package com.diyebure.golia;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.diyebure.golia.util.Constants;

/**
 * Prediction confirmation screen (R7).
 *
 * <p>Static screen: it receives the already-computed data via the Intent
 * (team names, predicted score, match date and possible points = 19) and only
 * renders them. It performs NO database access and needs no ViewModel or Hilt
 * injection (R7.6).</p>
 *
 * <p>Navigation:</p>
 * <ul>
 *   <li>"Ver historial" opens {@link HistorialPronosticosActivity} on top of
 *       this screen without leaving {@code PronosticoActivity} in the back stack
 *       (R7.4).</li>
 *   <li>"Ir al inicio" navigates to {@link MainActivity} clearing the prediction
 *       flow from the back stack with
 *       {@code FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP} so that
 *       "back" does not re-enter the prediction flow (R7.3).</li>
 * </ul>
 *
 * <p>The system "back" button uses the default behavior, which simply finishes
 * this Activity and returns to the previous screen without re-saving the
 * prediction or re-opening the prediction form (R7.5).</p>
 */
public class ConfirmacionActivity extends AppCompatActivity {

    private TextView textHomeName;
    private TextView textAwayName;
    private TextView textPredictedScore;
    private TextView textMatchDate;
    private TextView textPossiblePoints;
    private Button buttonVerHistorial;
    private Button buttonIrInicio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirmacion);

        bindViews();
        applyWindowInsets();
        bindData();
        setupListeners();
    }

    private void bindViews() {
        textHomeName = findViewById(R.id.text_home_name);
        textAwayName = findViewById(R.id.text_away_name);
        textPredictedScore = findViewById(R.id.text_predicted_score);
        textMatchDate = findViewById(R.id.text_match_date);
        textPossiblePoints = findViewById(R.id.text_possible_points);
        buttonVerHistorial = findViewById(R.id.button_ver_historial);
        buttonIrInicio = findViewById(R.id.button_ir_inicio);
    }

    private void bindData() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        String homeName = intent.getStringExtra(Constants.EXTRA_HOME_NAME);
        String awayName = intent.getStringExtra(Constants.EXTRA_AWAY_NAME);
        int homeScore = intent.getIntExtra(Constants.EXTRA_PREDICTED_HOME_SCORE, 0);
        int awayScore = intent.getIntExtra(Constants.EXTRA_PREDICTED_AWAY_SCORE, 0);
        String matchDate = intent.getStringExtra(Constants.EXTRA_MATCH_DATE);
        int possiblePoints = intent.getIntExtra(
                Constants.EXTRA_POSSIBLE_POINTS, Constants.PREDICTION_MAX_POINTS);

        textHomeName.setText(homeName != null ? homeName : "");
        textAwayName.setText(awayName != null ? awayName : "");
        textPredictedScore.setText(getString(
                R.string.confirmacion_marcador_valor, homeScore, awayScore));
        textMatchDate.setText(matchDate != null ? matchDate : "");
        textPossiblePoints.setText(getString(
                R.string.confirmacion_puntos_posibles, possiblePoints));
    }

    private void setupListeners() {
        buttonVerHistorial.setOnClickListener(v -> navigateToHistorial());
        buttonIrInicio.setOnClickListener(v -> navigateToHome());
    }

    /**
     * Opens the prediction history without leaving the prediction form above this
     * screen in the back stack (R7.4).
     */
    private void navigateToHistorial() {
        Intent intent = new Intent(this, HistorialPronosticosActivity.class);
        startActivity(intent);
    }

    /**
     * Navigates to {@link MainActivity} clearing the prediction flow from the
     * back stack so that "back" does not re-enter PronosticoActivity (R7.3).
     */
    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
