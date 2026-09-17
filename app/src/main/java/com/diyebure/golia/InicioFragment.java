package com.diyebure.golia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Home fragment displaying upcoming matches and user welcome information.
 */
public class InicioFragment extends Fragment {

    private TextView welcomeTextView;
    private TextView statsTextView;
    private LinearLayout streakButton;
    private LinearLayout matchCard1;
    private LinearLayout matchCard2;
    private TextView upcomingMatchesTitle;

    public InicioFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupUI();
    }

    private void initializeViews(View view) {
        welcomeTextView = view.findViewById(R.id.text_welcome);
        statsTextView = view.findViewById(R.id.text_stats);
        streakButton = view.findViewById(R.id.button_streak);
        matchCard1 = view.findViewById(R.id.card_match_1);
        matchCard2 = view.findViewById(R.id.card_match_2);
        upcomingMatchesTitle = view.findViewById(R.id.text_upcoming_matches);
    }

    private void setupUI() {
        // Set user welcome message
        if (welcomeTextView != null) {
            welcomeTextView.setText("BIENVENIDO Carlos Rodríguez");
        }

        // Set user statistics
        if (statsTextView != null) {
            statsTextView.setText("48 Pronóst. | 67% Aciertos | 1 250 Puntos | #142 Ranking");
        }

        // Setup match cards with click listeners for predictions
        if (matchCard1 != null) {
            matchCard1.setOnClickListener(v -> {
                // Navigate to predictions for this match
            });
        }

        if (matchCard2 != null) {
            matchCard2.setOnClickListener(v -> {
                // Navigate to predictions for this match
            });
        }
    }
}