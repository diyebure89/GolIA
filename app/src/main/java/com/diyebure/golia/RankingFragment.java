package com.diyebure.golia;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.diyebure.golia.domain.model.Ranking_Entry;
import com.diyebure.golia.domain.model.Ranking_Period;
import com.diyebure.golia.presentation.adapter.RankingAdapter;
import com.diyebure.golia.presentation.ui.ranking.RankingUiState;
import com.diyebure.golia.presentation.ui.ranking.RankingViewModel;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment de la Pantalla_Ranking (ranking-screen, tarea 8.3).
 *
 * <p>Reemplaza el placeholder anterior: fina capa de vista que observa
 * {@link RankingUiState} desde {@link RankingViewModel} (obtenido vía Hilt) e
 * infla {@code fragment_ranking.xml}. No accede a repositorios ni a la base de
 * datos; delega toda la lógica en el ViewModel (Requisitos 1.1, 7.1).</p>
 *
 * <ul>
 *     <li>Renderiza los cuatro estados: Loading / Content / Empty / Error
 *     (Requisitos 1.4, 1.5, 1.8, 7.1, 7.2, 7.3, 7.4).</li>
 *     <li>Pobla el podio (1.º centro destacado, 2.º izq, 3.º der) mostrando solo
 *     los puestos disponibles si hay menos de tres (Requisitos 3.1, 3.3).</li>
 *     <li>Enlaza la lista ordenada mediante {@link RankingAdapter}
 *     (Requisitos 5.1, 6.5).</li>
 *     <li>Cambio de chip de periodo -> {@link RankingViewModel#selectPeriod}
 *     (Requisito 4.3).</li>
 *     <li>Ciclo en vivo ligado a onResume/onPause: recálculo + suscripción al
 *     {@code LiveRefreshScheduler} mientras está visible (Requisitos 6.7, 6.8,
 *     7.2). La suscripción al scheduler la gestiona internamente el ViewModel.</li>
 *     <li>Navegación a login ante el evento correspondiente (Requisito 1.7).</li>
 * </ul>
 */
@AndroidEntryPoint
public class RankingFragment extends Fragment {

    private RankingViewModel viewModel;
    private RankingAdapter adapter;

    // Encabezado / lista / estados.
    private RecyclerView recyclerView;
    private ProgressBar progressLoading;
    private TextView emptyView;
    private View errorView;
    private TextView errorText;
    private Button retryButton;

    // Selector de periodo.
    private ChipGroup chipGroupPeriod;

    // Podio: bloques 2.º (izq), 1.º (centro), 3.º (der).
    private View podiumSecond;
    private ImageView podiumAvatar2;
    private TextView podiumName2;
    private TextView podiumPoints2;

    private View podiumFirst;
    private ImageView podiumAvatar1;
    private TextView podiumName1;
    private TextView podiumPoints1;

    private View podiumThird;
    private ImageView podiumAvatar3;
    private TextView podiumName3;
    private TextView podiumPoints3;

    /** Evita re-disparar {@code selectPeriod} cuando el estado del chip se ajusta por código. */
    private boolean suppressChipCallback;

    public RankingFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ranking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecycler();
        setupChips();

        viewModel = new ViewModelProvider(this).get(RankingViewModel.class);
        observeState();
        // La carga inicial la dispara onResume() -> onScreenResumed(), que además
        // activa el ciclo en vivo mientras la pantalla es visible (Requisitos 1.4, 7.2).
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_ranking);
        progressLoading = view.findViewById(R.id.progress_loading);
        emptyView = view.findViewById(R.id.empty_view);
        errorView = view.findViewById(R.id.error_view);
        errorText = view.findViewById(R.id.text_error);
        retryButton = view.findViewById(R.id.btn_retry);

        chipGroupPeriod = view.findViewById(R.id.chip_group_period);

        // Botón de historial: abre la misma pantalla que la confirmación de
        // pronóstico (HistorialPronosticosActivity), sin extras.
        view.findViewById(R.id.button_ranking_historial)
                .setOnClickListener(v -> navigateToHistorial());

        podiumSecond = view.findViewById(R.id.podium_second);
        podiumAvatar2 = view.findViewById(R.id.image_podium_avatar_2);
        podiumName2 = view.findViewById(R.id.text_podium_name_2);
        podiumPoints2 = view.findViewById(R.id.text_podium_points_2);

        podiumFirst = view.findViewById(R.id.podium_first);
        podiumAvatar1 = view.findViewById(R.id.image_podium_avatar_1);
        podiumName1 = view.findViewById(R.id.text_podium_name_1);
        podiumPoints1 = view.findViewById(R.id.text_podium_points_1);

        podiumThird = view.findViewById(R.id.podium_third);
        podiumAvatar3 = view.findViewById(R.id.image_podium_avatar_3);
        podiumName3 = view.findViewById(R.id.text_podium_name_3);
        podiumPoints3 = view.findViewById(R.id.text_podium_points_3);
    }

    private void setupRecycler() {
        adapter = new RankingAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Abre {@link HistorialPronosticosActivity}, la misma pantalla de historial
     * de pronósticos que lanza la confirmación de pronóstico. No requiere extras:
     * la actividad resuelve el usuario desde su propia sesión.
     */
    private void navigateToHistorial() {
        startActivity(new Intent(requireContext(), HistorialPronosticosActivity.class));
    }

    // ==================== Selector de periodo (Requisito 4.3) ====================

    private void setupChips() {
        chipGroupPeriod.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (suppressChipCallback) {
                return;
            }
            int checkedId = checkedIds.isEmpty() ? View.NO_ID : checkedIds.get(0);
            if (checkedId == View.NO_ID) {
                // singleSelection no debería dejar sin selección; re-marcamos Semanal.
                selectChipForPeriod(Ranking_Period.SEMANAL);
                return;
            }
            viewModel.selectPeriod(periodFromChipId(checkedId));
        });
    }

    private Ranking_Period periodFromChipId(int checkedId) {
        if (checkedId == R.id.chip_mensual) {
            return Ranking_Period.MENSUAL;
        }
        if (checkedId == R.id.chip_total) {
            return Ranking_Period.TOTAL;
        }
        return Ranking_Period.SEMANAL;
    }

    private void selectChipForPeriod(Ranking_Period period) {
        int chipId;
        if (period == Ranking_Period.MENSUAL) {
            chipId = R.id.chip_mensual;
        } else if (period == Ranking_Period.TOTAL) {
            chipId = R.id.chip_total;
        } else {
            chipId = R.id.chip_semanal;
        }
        suppressChipCallback = true;
        chipGroupPeriod.check(chipId);
        suppressChipCallback = false;
    }

    // ==================== Observación de estado ====================

    private void observeState() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);

        // Evento de un solo disparo: navegar a login sin sesión (R1.7).
        viewModel.getNavigateToLogin().observe(getViewLifecycleOwner(), event -> {
            if (event == null) {
                return;
            }
            Boolean go = event.getContentIfNotHandled();
            if (Boolean.TRUE.equals(go)) {
                navigateToLogin();
            }
        });

        // Refleja el periodo retenido en los chips tras rotación (R7.7).
        selectChipForPeriod(viewModel.getSelectedPeriod());
    }

    private void render(RankingUiState state) {
        if (state instanceof RankingUiState.Loading) {
            renderLoading();
        } else if (state instanceof RankingUiState.Content) {
            renderContent((RankingUiState.Content) state);
        } else if (state instanceof RankingUiState.Empty) {
            renderEmpty();
        } else if (state instanceof RankingUiState.Error) {
            renderError((RankingUiState.Error) state);
        }
    }

    private void renderLoading() {
        // Indicador de carga solo si aún no hay contenido visible (evita parpadeo en vivo).
        boolean hasContent = adapter.getItemCount() > 0;
        progressLoading.setVisibility(hasContent ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        recyclerView.setVisibility(hasContent ? View.VISIBLE : View.GONE);
    }

    private void renderContent(RankingUiState.Content content) {
        progressLoading.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        bindPodium(content.podium);
        adapter.submitList(content.list);
    }

    private void renderEmpty() {
        progressLoading.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    private void renderError(RankingUiState.Error error) {
        progressLoading.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        errorText.setText(error.message);
        retryButton.setOnClickListener(v -> viewModel.load());
        errorView.setVisibility(View.VISIBLE);
    }

    // ==================== Podio (Requisitos 3.1, 3.3) ====================

    /**
     * Pobla los tres bloques del podio a partir de las entradas top del ranking:
     * 1.º al centro (destacado), 2.º a la izquierda y 3.º a la derecha. Muestra
     * solo los puestos disponibles cuando hay menos de tres participantes.
     */
    private void bindPodium(@NonNull List<Ranking_Entry> podium) {
        Ranking_Entry first = podium.size() > 0 ? podium.get(0) : null;
        Ranking_Entry second = podium.size() > 1 ? podium.get(1) : null;
        Ranking_Entry third = podium.size() > 2 ? podium.get(2) : null;

        bindPodiumBlock(podiumFirst, podiumAvatar1, podiumName1, podiumPoints1, first);
        bindPodiumBlock(podiumSecond, podiumAvatar2, podiumName2, podiumPoints2, second);
        bindPodiumBlock(podiumThird, podiumAvatar3, podiumName3, podiumPoints3, third);
    }

    private void bindPodiumBlock(View block, ImageView avatar, TextView name,
                                 TextView points, @Nullable Ranking_Entry entry) {
        if (entry == null) {
            // Puesto no disponible: se oculta el bloque completo (menos de 3 participantes).
            block.setVisibility(View.INVISIBLE);
            return;
        }
        block.setVisibility(View.VISIBLE);
        name.setText(entry.getDisplayName());
        String formattedPoints = NumberFormat.getIntegerInstance(Locale.getDefault())
                .format(entry.getPoints());
        points.setText(getString(R.string.ranking_puntos_formato, formattedPoints));
        bindAvatar(avatar, entry.getAvatarRef());
    }

    /**
     * Resuelve el {@code avatarRef} y lo carga en {@code target}, degradando al
     * placeholder cuando es nulo, apunta a un archivo inexistente/no decodificable
     * o a un recurso drawable no resoluble (R5.4). Misma convención que
     * {@link RankingAdapter}.
     */
    private static void bindAvatar(@NonNull ImageView target, @Nullable String avatarRef) {
        if (TextUtils.isEmpty(avatarRef)) {
            target.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }

        File file = new File(avatarRef);
        if (file.isAbsolute() && file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(avatarRef);
            if (bitmap != null) {
                target.setImageBitmap(bitmap);
                return;
            }
        }

        int resId = target.getContext().getResources().getIdentifier(
                avatarRef, "drawable", target.getContext().getPackageName());
        if (resId != 0) {
            target.setImageResource(resId);
            return;
        }

        target.setImageResource(R.drawable.ic_avatar_placeholder);
    }

    // ==================== Navegación (Requisito 1.7) ====================

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // ==================== Ciclo en vivo (Requisitos 6.7, 6.8, 7.2) ====================

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            // Recalcula y activa la suscripción al LiveRefreshScheduler mientras visible.
            viewModel.onScreenResumed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (viewModel != null) {
            // Detiene el ciclo en vivo al dejar de ser visible.
            viewModel.onScreenPaused();
        }
    }
}
