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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment displaying all matches with filtering options.
 */
public class PartidosFragment extends Fragment {

    private TabLayout filterTabs;
    private RecyclerView matchesRecyclerView;
    private TextView emptyView;

    private List<MatchItem> allMatches;
    private int currentFilter = 0; // 0 = All, 1 = Today, 2 = Tomorrow, 3 = ThisWeek

    public PartidosFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_partidos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupFilterTabs();
        loadMatches();
    }

    private void initializeViews(View view) {
        filterTabs = view.findViewById(R.id.tab_layout_filters);
        matchesRecyclerView = view.findViewById(R.id.recycler_matches);
        emptyView = view.findViewById(R.id.empty_view);

        matchesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupFilterTabs() {
        if (filterTabs != null) {
            filterTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    currentFilter = tab.getPosition();
                    filterMatches();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    filterMatches();
                }
            });
        }
    }

    private void loadMatches() {
        allMatches = new ArrayList<>();

        // Sample match data - in a real app, this would come from an API
        allMatches.add(new MatchItem("LA LIGA", "JORNADA 38",
                "FC Barcelona", "Real Madrid",
                "14 Jun", "20:00", "2.1", "3.2", "2.8"));

        allMatches.add(new MatchItem("NBA PLAYOFFS", "CONF. FINAL",
                "LA Lakers", "Golden State",
                "14 Jun", "22:30", "1.9", "-", "1.95"));

        allMatches.add(new MatchItem("CHAMPIONS LEAGUE", "SEMIFINAL",
                "Manchester City", "Real Madrid",
                "15 Jun", "21:00", "1.75", "3.5", "4.0"));

        allMatches.add(new MatchItem("LA LIGA", "JORNADA 38",
                "Sevilla", "Atlético Madrid",
                "15 Jun", "18:30", "2.3", "3.1", "2.9"));

        filterMatches();
    }

    private void filterMatches() {
        List<MatchItem> filteredMatches = new ArrayList<>();

        switch (currentFilter) {
            case 0: // All
                filteredMatches.addAll(allMatches);
                break;
            case 1: // Today
                for (MatchItem match : allMatches) {
                    if ("14 Jun".equals(match.date)) {
                        filteredMatches.add(match);
                    }
                }
                break;
            case 2: // Tomorrow
                for (MatchItem match : allMatches) {
                    if ("15 Jun".equals(match.date)) {
                        filteredMatches.add(match);
                    }
                }
                break;
            case 3: // This Week
                filteredMatches.addAll(allMatches);
                break;
        }

        updateMatchesList(filteredMatches);
    }

    private void updateMatchesList(List<MatchItem> matches) {
        if (matchesRecyclerView != null) {
            MatchesAdapter adapter = new MatchesAdapter(matches);
            matchesRecyclerView.setAdapter(adapter);
        }

        if (emptyView != null) {
            emptyView.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
            matchesRecyclerView.setVisibility(matches.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Data class representing a match item.
     */
    public static class MatchItem {
        public String league;
        public String leagueRound;
        public String team1;
        public String team2;
        public String date;
        public String time;
        public String odds1;
        public String oddsX;
        public String odds2;

        public MatchItem(String league, String leagueRound, String team1, String team2,
                         String date, String time, String odds1, String oddsX, String odds2) {
            this.league = league;
            this.leagueRound = leagueRound;
            this.team1 = team1;
            this.team2 = team2;
            this.date = date;
            this.time = time;
            this.odds1 = odds1;
            this.oddsX = oddsX;
            this.odds2 = odds2;
        }
    }

    /**
     * RecyclerView adapter for displaying matches.
     */
    private class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.MatchViewHolder> {

        private final List<MatchItem> matches;

        MatchesAdapter(List<MatchItem> matches) {
            this.matches = matches;
        }

        @NonNull
        @Override
        public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_match, parent, false);
            return new MatchViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
            holder.bind(matches.get(position));
        }

        @Override
        public int getItemCount() {
            return matches.size();
        }

        class MatchViewHolder extends RecyclerView.ViewHolder {
            private final TextView leagueText;
            private final TextView leagueRoundText;
            private final TextView team1Text;
            private final TextView team2Text;
            private final TextView dateTimeText;
            private final TextView odds1Text;
            private final TextView oddsXText;
            private final TextView odds2Text;
            private final LinearLayout cardContainer;

            MatchViewHolder(@NonNull View itemView) {
                super(itemView);
                leagueText = itemView.findViewById(R.id.text_league);
                leagueRoundText = itemView.findViewById(R.id.text_league_round);
                team1Text = itemView.findViewById(R.id.text_team1);
                team2Text = itemView.findViewById(R.id.text_team2);
                dateTimeText = itemView.findViewById(R.id.text_date_time);
                odds1Text = itemView.findViewById(R.id.text_odds1);
                oddsXText = itemView.findViewById(R.id.text_odds_x);
                odds2Text = itemView.findViewById(R.id.text_odds2);
                cardContainer = itemView.findViewById(R.id.card_match_container);
            }

            void bind(MatchItem match) {
                leagueText.setText(match.league);
                leagueRoundText.setText(match.leagueRound);
                team1Text.setText(match.team1);
                team2Text.setText(match.team2);
                dateTimeText.setText(match.date + " · " + match.time);

                odds1Text.setText(match.odds1);
                oddsXText.setText(match.oddsX);
                odds2Text.setText(match.odds2);

                cardContainer.setOnClickListener(v -> {
                    // Navigate to prediction screen for this match
                });
            }
        }
    }
}