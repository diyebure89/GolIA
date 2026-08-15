package com.diyebure.golia;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class InicioFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Dentro de tu Fragment
        Intent intent = new Intent(getActivity(),MainActivity.class);
        startActivity(intent);

        return inflater.inflate(R.layout.fragment_inicio, container, false);

    }
}
