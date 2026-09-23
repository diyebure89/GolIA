package com.diyebure.golia.presentation.ui.common;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Contrato común de poblado de UI. Cada fragment centraliza el llenado de su
 * vista en {@link #bindPlaceholderData()}, invocado desde onViewCreated.
 */
public abstract class BasePlaceholderFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindPlaceholderData();
    }

    /** Puebla la UI con datos placeholder (o, en el futuro, del ViewModel). */
    protected abstract void bindPlaceholderData();
}
