package com.diyebure.golia.presentation.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

/**
 * Base Activity class providing common functionality
 * - ViewBinding support
 * - Loading state management
 * - Error handling abstraction
 */
public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {

    protected VB binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = inflateBinding(getLayoutInflater());
        setContentView(binding.getRoot());
        setupViews();
        observeData();
    }

    /**
     * Inflate the ViewBinding for this activity
     */
    protected abstract VB inflateBinding(LayoutInflater inflater);

    /**
     * Setup views and click listeners
     */
    protected abstract void setupViews();

    /**
     * Observe data from ViewModel
     */
    protected abstract void observeData();

    /**
     * Show loading indicator
     */
    protected void showLoading() {
        // Override in subclasses to implement loading UI
    }

    /**
     * Hide loading indicator
     */
    protected void hideLoading() {
        // Override in subclasses to implement loading UI
    }

    /**
     * Show error message to user
     */
    protected void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Show success message to user
     */
    protected void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Get the ViewBinding instance
     */
    protected VB getBinding() {
        return binding;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}