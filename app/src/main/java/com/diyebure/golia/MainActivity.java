package com.diyebure.golia;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.diyebure.golia.presentation.ui.common.Screen;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Main activity containing bottom navigation for the app.
 * Handles fragment transactions for 4 main screens: Inicio, Partidos, Pronósticos, and Perfil.
 */
public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private BottomNavigationView bottomNavigationView;
    private Fragment currentFragment;
    private int currentScreenIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupBottomNavigation();

        if (savedInstanceState == null) {
            // Load default fragment (Home/Inicio)
            loadFragment(Screen.HOME);
        } else {
            // Restore state after rotation
            currentScreenIndex = savedInstanceState.getInt("current_screen_index", 0);
            Fragment savedFragment = getSupportFragmentManager().findFragmentByTag("current_fragment");
            if (savedFragment != null) {
                currentFragment = savedFragment;
            }
        }
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        // Set gold color for active tab
        bottomNavigationView.setItemIconTintList(getColorStateListFromResource(R.color.gold));
        bottomNavigationView.setItemTextColor(getColorStateListFromResource(R.color.gold));
    }

    private android.content.res.ColorStateList getColorStateListFromResource(int colorResId) {
        return android.content.res.ColorStateList.valueOf(getResources().getColor(colorResId, getTheme()));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            return switchScreen(Screen.HOME, 0);
        } else if (itemId == R.id.nav_matches) {
            return switchScreen(Screen.MATCHES, 1);
        } else if (itemId == R.id.nav_predictions) {
            return switchScreen(Screen.PREDICTIONS, 2);
        } else if (itemId == R.id.nav_profile) {
            return switchScreen(Screen.PROFILE, 3);
        }

        return false;
    }

    private boolean switchScreen(Screen screen, int screenIndex) {
        if (currentScreenIndex == screenIndex) {
            // Same screen selected, do nothing
            return true;
        }

        currentScreenIndex = screenIndex;
        loadFragment(screen);
        return true;
    }

    private void loadFragment(Screen screen) {
        try {
            Fragment fragment = (Fragment) screen.getFragmentClass().newInstance();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment, "current_fragment");

            // Add to back stack for proper back navigation
            transaction.addToBackStack(null);
            transaction.commit();

            currentFragment = fragment;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_screen_index", currentScreenIndex);
    }

    @Override
    public void onBackPressed() {
        if (currentScreenIndex != 0) {
            // Navigate to home instead of exiting
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }
}