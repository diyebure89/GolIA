package com.diyebure.golia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.diyebure.golia.presentation.adapter.PredictionHistoryAdapter;
import com.diyebure.golia.presentation.ui.history.HistorialViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Prediction history screen (R8).
 *
 * <p>{@code @AndroidEntryPoint} so Hilt can inject the {@link HistorialViewModel}.
 * The Activity is a thin view: it observes the ViewModel, shows a loading
 * indicator, the RecyclerView, the empty state or an error message, and
 * redirects to login when there is no valid session (R8.6, R8.7, R8.8).</p>
 */
@AndroidEntryPoint
public class HistorialPronosticosActivity extends AppCompatActivity {

    private HistorialViewModel viewModel;

    private ImageButton buttonBack;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;

    private PredictionHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historial_pronosticos);

        viewModel = new ViewModelProvider(this).get(HistorialViewModel.class);

        bindViews();
        setupRecycler();
        setupListeners();
        observeViewModel();
        applyWindowInsets();

        viewModel.load();
    }

    private void bindViews() {
        buttonBack = findViewById(R.id.button_back);
        recyclerView = findViewById(R.id.recycler_history);
        progressBar = findViewById(R.id.progressBar_historial);
        emptyView = findViewById(R.id.empty_view);
    }

    private void setupRecycler() {
        adapter = new PredictionHistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, this::showLoading);

        viewModel.getPredictions().observe(this, predictions -> {
            adapter.submitList(predictions);
            recyclerView.setVisibility(
                    predictions != null && !predictions.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsEmpty().observe(this, empty ->
                emptyView.setVisibility(Boolean.TRUE.equals(empty) ? View.VISIBLE : View.GONE));

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
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

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Maps a typed {@code PredictionError} name emitted by the ViewModel to a
     * user-facing string resource. Falls back to a generic error message.
     */
    private String mapError(String errorName) {
        switch (errorName) {
            case "SESSION_UNAVAILABLE":
                return getString(R.string.error_session_unavailable);
            case "PERSISTENCE_ERROR":
                return getString(R.string.error_persistence);
            default:
                return getString(R.string.error_generic);
        }
    }
}
