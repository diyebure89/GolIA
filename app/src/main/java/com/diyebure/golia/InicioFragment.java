package com.diyebure.golia;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.presentation.adapter.MatchesAdapter;
import com.diyebure.golia.presentation.ui.common.BasePlaceholderFragment;
import com.diyebure.golia.presentation.ui.inicio.InicioViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Home fragment displaying the welcome card, placeholder statistics and the
 * "Próximos partidos" section populated with real data.
 *
 * <p>El nombre mostrado proviene únicamente de la sesión local
 * ({@link PreferencesManager}), sin tocar base de datos ni repositorios
 * (R11.2). Las estadísticas siguen siendo placeholders. Los próximos partidos
 * se obtienen de {@link InicioViewModel}, que comparte la fuente de datos con la
 * Pantalla_Partidos, y "Ver todos" navega a la pestaña Partidos.
 */
@AndroidEntryPoint
public class InicioFragment extends BasePlaceholderFragment {

    /** Nombre por defecto cuando la sesión no tiene un nombre válido (R7.2/R11.3). */
    private static final String DEFAULT_NAME = "Invitado";

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
    protected void bindPlaceholderData() {
        View v = requireView();

        // Nombre desde sesión local con fallback (R7.2, R11.2, R11.3)
        String name = readSessionName();
        TextView welcome = v.findViewById(R.id.text_welcome);
        welcome.setText(getString(R.string.inicio_bienvenida_formato, resolveName(name)));

        // 4 estadísticas independientes placeholder (R7.3)
        ((TextView) v.findViewById(R.id.text_stat_pronosticos_value)).setText("48");
        ((TextView) v.findViewById(R.id.text_stat_aciertos_value)).setText("67%");
        ((TextView) v.findViewById(R.id.text_stat_puntos_value)).setText("1 250");
        ((TextView) v.findViewById(R.id.text_stat_ranking_value)).setText("#142");

        // Campana de notificaciones clickable sin acción (R7.1)
        v.findViewById(R.id.icon_notifications).setOnClickListener(view -> { /* sin acción */ });

        // Lista de próximos partidos con datos reales.
        RecyclerView recyclerUpcoming = v.findViewById(R.id.recycler_upcoming);
        recyclerUpcoming.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerUpcoming.setNestedScrollingEnabled(false);
        MatchesAdapter adapter = new MatchesAdapter();
        recyclerUpcoming.setAdapter(adapter);

        InicioViewModel viewModel = new ViewModelProvider(this).get(InicioViewModel.class);
        viewModel.getUpcomingMatches().observe(getViewLifecycleOwner(), adapter::submitList);

        // "Ver todos" navega a la pestaña Partidos.
        v.findViewById(R.id.text_see_all).setOnClickListener(view ->
                ((MainActivity) requireActivity()).navigateToPartidos());
    }

    /**
     * Resuelve el nombre a mostrar aplicando el fallback.
     *
     * <p>Método estático puro (sin dependencias de Android) para poder testearse
     * en JVM sin instrumentación (R7.2/R11.3).
     *
     * @param sessionName nombre leído de la sesión local (puede ser {@code null})
     * @return el nombre si no es nulo ni solo espacios, o {@link #DEFAULT_NAME}
     */
    static String resolveName(String sessionName) {
        return (sessionName == null || sessionName.trim().isEmpty()) ? DEFAULT_NAME : sessionName;
    }

    /**
     * Lee el nombre desde {@link PreferencesManager} (solo sesión local, R11.2).
     *
     * <p>Se instancia {@code PreferencesManager} con el {@code ApplicationContext}
     * (mismo comportamiento que su constructor {@code @Inject}), evitando
     * acoplar el fragment a Hilt y manteniendo la lectura restringida a la
     * sesión local.
     */
    private String readSessionName() {
        Context appContext = requireContext().getApplicationContext();
        return new PreferencesManager(appContext).getUserName();
    }
}
