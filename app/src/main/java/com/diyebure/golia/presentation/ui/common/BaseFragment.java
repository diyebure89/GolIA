package com.diyebure.golia.presentation.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

/**
 * Base Fragment class providing common functionality
 * - ViewBinding support
 * - Loading state management
 * - Error handling abstraction
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    protected VB binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = inflateBinding(inflater, container);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        observeData();
    }

    /**
     * Inflate the ViewBinding for this fragment
     */
    protected abstract VB inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

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
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show success message to user
     */
    protected void showSuccess(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get the ViewBinding instance
     */
    protected VB getBinding() {
        return binding;
    }

    /**
     * Get the activity context safely
     */
    protected android.content.Context getSafeContext() {
        if (getActivity() != null) {
            return getActivity();
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}