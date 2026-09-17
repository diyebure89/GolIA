package com.diyebure.golia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

/**
 * Fragment for displaying user's prediction history.
 * Contains filter tabs for pending, correct, and incorrect predictions.
 */
public class PredictionsFragment extends Fragment {

    private TabLayout tabLayout;

    public PredictionsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_predictions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupTabs(view);
    }

    private void setupTabs(View view) {
        tabLayout = view.findViewById(R.id.tab_layout_predictions);
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    // Filter predictions based on tab position
                    int position = tab.getPosition();
                    filterPredictions(position);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    // No action needed
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    // Re-apply filter
                    filterPredictions(tab.getPosition());
                }
            });
        }
    }

    private void filterPredictions(int filterType) {
        // filterType: 0 = Pending, 1 = Correct, 2 = Incorrect
        // Apply filtering logic to show only predictions of the selected type
        // This would typically filter a RecyclerView or ListView
    }
}