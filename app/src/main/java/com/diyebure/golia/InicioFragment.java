package com.diyebure.golia;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.diyebure.golia.domain.model.RankingSnapshot;
import com.diyebure.golia.presentation.adapter.MatchesAdapter;
import com.diyebure.golia.presentation.ui.common.BasePlaceholderFragment;
import com.diyebure.golia.presentation.ui.inicio.InicioViewModel;
import com.diyebure.golia.presentation.ui.partidos.MatchUiModel;
import com.diyebure.golia.util.Constants;
import com.diyebure.golia.util.DisplayName;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Home fragment displaying the welcome card, placeholder statistics and the
 * "Próximos partidos" section populated with real data.
 *
 * <p>La tarjeta de bienvenida muestra, centrados, el avatar del usuario, el
 * texto fijo "BIENVENIDO" y el nombre a mostrar. El nombre y el avatar provienen
 * únicamente de la sesión local ({@link PreferencesManager}) y del almacenamiento
 * interno, sin tocar base de datos ni repositorios (R11.2). El nombre da
 * prioridad al nombre de usuario sobre el nombre completo y usa "Invitado" como
 * último recurso. Las estadísticas siguen siendo placeholders. Los próximos
 * partidos se obtienen de {@link InicioViewModel}, que comparte la fuente de
 * datos con la Pantalla_Partidos, y "Ver todos" navega a la pestaña Partidos.
 */
@AndroidEntryPoint
public class InicioFragment extends BasePlaceholderFragment {

    /** Nombre por defecto cuando la sesión no tiene usuario ni nombre válidos (R7.2/R11.3). */
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

        // Nombre y avatar de la tarjeta de bienvenida (se refrescan también en
        // onResume para reflejar cambios hechos en la Pantalla_Perfil).
        refreshWelcomeCard(v);

        // Estado neutro inicial mientras se calcula el ranking real en segundo plano.
        renderRanking(v, null);

        // Campana de notificaciones clickable sin acción (R7.1)
        v.findViewById(R.id.icon_notifications).setOnClickListener(view -> { /* sin acción */ });

        // Lista de próximos partidos con datos reales.
        RecyclerView recyclerUpcoming = v.findViewById(R.id.recycler_upcoming);
        recyclerUpcoming.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerUpcoming.setNestedScrollingEnabled(false);
        // Al pulsar una tarjeta se abre el detalle con la PK del partido
        // (MatchUiModel.id, no el externalId) en EXTRA_MATCH_ID (R1.1, R1.2, R1.3).
        MatchesAdapter adapter = new MatchesAdapter(this::openMatchDetail);
        recyclerUpcoming.setAdapter(adapter);

        InicioViewModel viewModel = new ViewModelProvider(this).get(InicioViewModel.class);
        viewModel.getUpcomingMatches().observe(getViewLifecycleOwner(), adapter::submitList);
        // Estadísticas reales del ranking del usuario actual (R7.3, R11).
        viewModel.getRanking().observe(getViewLifecycleOwner(),
                snapshot -> renderRanking(requireView(), snapshot));

        // "Ver todos" navega a la pestaña Partidos.
        v.findViewById(R.id.text_see_all).setOnClickListener(view ->
                ((MainActivity) requireActivity()).navigateToPartidos());
    }

    /**
     * Pinta las cuatro estadísticas de la tarjeta de bienvenida con los datos
     * reales del ranking del usuario actual. Cuando no hay snapshot (usuario sin
     * posición en el ranking) muestra un estado neutro con guiones, nunca un
     * error (R11.5).
     */
    private void renderRanking(@NonNull View v, @Nullable RankingSnapshot snapshot) {
        TextView pronosticos = v.findViewById(R.id.text_stat_pronosticos_value);
        TextView aciertos = v.findViewById(R.id.text_stat_aciertos_value);
        TextView puntos = v.findViewById(R.id.text_stat_puntos_value);
        TextView ranking = v.findViewById(R.id.text_stat_ranking_value);

        if (snapshot == null) {
            pronosticos.setText("-");
            aciertos.setText("-");
            puntos.setText("-");
            ranking.setText("-");
            return;
        }

        pronosticos.setText(String.valueOf(snapshot.getPredictionsMade()));
        aciertos.setText(String.format(Locale.getDefault(), "%.0f%%",
                snapshot.getAccuracyPercentage()));
        puntos.setText(String.valueOf(snapshot.getTotalPoints()));
        ranking.setText(String.format(Locale.getDefault(), "#%d",
                snapshot.getRankingPosition()));
    }

    /**
     * Abre {@link DetallePartidoActivity} para el partido pulsado, transportando
     * su clave primaria ({@link MatchUiModel#id}) en {@link Constants#EXTRA_MATCH_ID}
     * (R1.2, R1.3).
     */
    private void openMatchDetail(@NonNull MatchUiModel match) {
        Intent intent = new Intent(requireContext(), DetallePartidoActivity.class);
        intent.putExtra(Constants.EXTRA_MATCH_ID, match.id);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Al volver a la pestaña Inicio (show/hide no recrea el fragment), refresca
        // el avatar y el nombre para reflejar de inmediato un cambio hecho en Perfil.
        View v = getView();
        if (v != null) {
            refreshWelcomeCard(v);
        }
    }

    /**
     * Refresca el nombre y el avatar de la tarjeta de bienvenida desde la sesión
     * local y el almacenamiento interno. Se llama al crear la vista y en cada
     * {@code onResume} para reflejar cambios hechos en la Pantalla_Perfil.
     *
     * <p>El prefijo fijo "BIENVENIDO" lo aporta el TextView del layout, por lo que
     * aquí solo se fija el nombre resuelto (R7.2, R11.2, R11.3).
     */
    private void refreshWelcomeCard(View v) {
        TextView welcome = v.findViewById(R.id.text_welcome);
        welcome.setText(resolveDisplayName());
        renderAvatar(v.findViewById(R.id.image_avatar_home));
    }

    /**
     * Lee el nombre de usuario y el nombre completo de la sesión local y resuelve
     * el nombre a mostrar dando prioridad al usuario (R7.2, R11.2, R11.3).
     *
     * <p>Se instancia {@code PreferencesManager} con el {@code ApplicationContext}
     * (mismo comportamiento que su constructor {@code @Inject}), evitando acoplar
     * el fragment a Hilt y manteniendo la lectura restringida a la sesión local.
     * La regla de prioridad vive en {@link DisplayName}, compartida con la
     * Pantalla_Perfil.
     */
    private String resolveDisplayName() {
        Context appContext = requireContext().getApplicationContext();
        PreferencesManager prefs = new PreferencesManager(appContext);
        return DisplayName.resolve(prefs.getUserUsername(), prefs.getUserName(), DEFAULT_NAME);
    }

    /**
     * Carga el avatar de la tarjeta de bienvenida desde la foto de perfil guardada
     * en almacenamiento interno ({@code filesDir/avatar_{userId}.jpg}, la misma que
     * escribe {@code PhotoStorage}). Si no hay usuario en sesión o el archivo no
     * existe, muestra el avatar por defecto sin error (coherente con la Pantalla_Perfil).
     */
    private void renderAvatar(ShapeableImageView avatar) {
        if (avatar == null) {
            return;
        }
        Context appContext = requireContext().getApplicationContext();
        String userId = new PreferencesManager(appContext).getUserId();
        if (!TextUtils.isEmpty(userId)) {
            // Misma ruta y nombre determinista que escribe PhotoStorage
            // (filesDir/avatar_{userId}.jpg), para compartir el archivo con Perfil.
            File file = new File(appContext.getFilesDir(), "avatar_" + userId + ".jpg");
            if (file.exists()) {
                // El nombre de archivo es determinista, así que el URI no cambia al
                // reemplazar la foto. Descartar el drawable actual fuerza a
                // ImageView a releer el bitmap y no servir la versión cacheada.
                avatar.setImageDrawable(null);
                avatar.setImageURI(Uri.fromFile(file));
                return;
            }
        }
        avatar.setImageResource(R.drawable.ic_avatar_placeholder);
    }
}
