package com.diyebure.golia;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.diyebure.golia.presentation.adapter.MatchesAdapter;
import com.diyebure.golia.presentation.ui.common.BasePlaceholderFragment;
import com.diyebure.golia.presentation.ui.partidos.ChipHorario;
import com.diyebure.golia.presentation.ui.partidos.PartidosUiState;
import com.diyebure.golia.presentation.ui.partidos.PartidosViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment de la Pantalla_Partidos.
 *
 * <p>Fina capa de vista: observa {@link PartidosUiState} desde
 * {@link PartidosViewModel} (obtenido vía Hilt) y delega toda acción de usuario
 * en el ViewModel (Requisitos 12.1, 12.2, 12.3). No contiene lógica de datos ni
 * un adapter interno; reutiliza {@link MatchesAdapter} (ListAdapter + DiffUtil).</p>
 *
 * <ul>
 *     <li>Chips de horario con selección única, "Hoy" por defecto y resaltado del
 *     activo con {@code blue_end} (Requisitos 3.1, 3.2, 3.3, 3.9).</li>
 *     <li>Búsqueda con {@link TextWatcher} que reenvía el texto crudo al ViewModel
 *     (el debounce de 300&nbsp;ms vive en el ViewModel) (Requisitos 10.1, 10.5).</li>
 *     <li>Pull-to-refresh vía {@link SwipeRefreshLayout} (Requisito 7.2).</li>
 *     <li>Ciclo de polling en vivo ligado a onResume/onPause (Requisitos 7B.1, 7B.2).</li>
 * </ul>
 */
@AndroidEntryPoint
public class PartidosFragment extends BasePlaceholderFragment {

    private PartidosViewModel viewModel;
    private MatchesAdapter adapter;

    private ChipGroup chipGroup;
    private Chip chipAyer;
    private Chip chipHoy;
    private Chip chipManana;
    private Chip chipEstaSemana;

    private EditText searchInput;
    private ImageButton searchButton;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressLoading;
    private TextView emptyView;
    private TextView noticeView;
    private View errorView;
    private TextView errorText;
    private Button retryButton;

    /** Evita re-disparar filtrado cuando el resaltado modifica el estado del chip. */
    private boolean suppressChipCallback;

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
    protected void bindPlaceholderData() {
        View view = requireView();
        initViews(view);
        setupRecycler();
        setupChips();
        setupSearch();
        setupSwipeRefresh();

        viewModel = new ViewModelProvider(this).get(PartidosViewModel.class);
        observeState();
    }

    private void initViews(View view) {
        chipGroup = view.findViewById(R.id.chip_group_filters);
        chipAyer = view.findViewById(R.id.chip_ayer);
        chipHoy = view.findViewById(R.id.chip_hoy);
        chipManana = view.findViewById(R.id.chip_manana);
        chipEstaSemana = view.findViewById(R.id.chip_esta_semana);

        searchInput = view.findViewById(R.id.edit_search);
        searchButton = view.findViewById(R.id.btn_search);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        recyclerView = view.findViewById(R.id.recycler_matches);
        progressLoading = view.findViewById(R.id.progress_loading);
        emptyView = view.findViewById(R.id.empty_view);
        noticeView = view.findViewById(R.id.text_notice);
        errorView = view.findViewById(R.id.error_view);
        errorText = view.findViewById(R.id.text_error);
        retryButton = view.findViewById(R.id.btn_retry);
    }

    private void setupRecycler() {
        adapter = new MatchesAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    // ==================== Chips (Requisitos 3.1, 3.2, 3.3, 3.9) ====================

    private void setupChips() {
        // "Hoy" seleccionado por defecto (Requisito 3.3).
        chipHoy.setChecked(true);
        highlightActiveChip(R.id.chip_hoy);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (suppressChipCallback) {
                return;
            }
            int checkedId = checkedIds.isEmpty() ? View.NO_ID : checkedIds.get(0);
            if (checkedId == View.NO_ID) {
                // singleSelection no debería dejar sin selección; re-marcamos "Hoy".
                chipHoy.setChecked(true);
                return;
            }
            highlightActiveChip(checkedId);
            viewModel.onChipSelected(chipFromId(checkedId));
        });
    }

    private ChipHorario chipFromId(int checkedId) {
        if (checkedId == R.id.chip_ayer) {
            return ChipHorario.AYER;
        }
        if (checkedId == R.id.chip_manana) {
            return ChipHorario.MANANA;
        }
        if (checkedId == R.id.chip_esta_semana) {
            return ChipHorario.ESTA_SEMANA;
        }
        return ChipHorario.HOY;
    }

    /** Resalta el chip activo con {@code blue_end} y atenúa el resto (Requisito 3.9). */
    private void highlightActiveChip(int activeId) {
        int active = ContextCompat.getColor(requireContext(), R.color.blue_end);
        int inactive = ContextCompat.getColor(requireContext(), R.color.blue_dark);
        applyChipColor(chipAyer, chipAyer.getId() == activeId ? active : inactive);
        applyChipColor(chipHoy, chipHoy.getId() == activeId ? active : inactive);
        applyChipColor(chipManana, chipManana.getId() == activeId ? active : inactive);
        applyChipColor(chipEstaSemana, chipEstaSemana.getId() == activeId ? active : inactive);
    }

    private void applyChipColor(Chip chip, int backgroundColor) {
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(backgroundColor));
        boolean isActive = backgroundColor == ContextCompat.getColor(requireContext(), R.color.blue_end);
        chip.setTextColor(isActive
                ? ContextCompat.getColor(requireContext(), R.color.blue_dark)
                : Color.WHITE);
    }

    // ==================== Búsqueda (Requisitos 10.1, 10.5) ====================

    private void setupSearch() {
        searchButton.setOnClickListener(v -> toggleSearch());

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // El debounce de 300 ms vive en el ViewModel; aquí se reenvía crudo.
                viewModel.onSearchQueryChanged(s == null ? "" : s.toString());
            }
        });
    }

    private void toggleSearch() {
        boolean nowVisible = searchInput.getVisibility() != View.VISIBLE;
        searchInput.setVisibility(nowVisible ? View.VISIBLE : View.GONE);
        if (nowVisible) {
            searchInput.requestFocus();
        } else {
            // Al ocultar la búsqueda se limpia el texto y se restaura la lista del chip.
            searchInput.setText("");
        }
    }

    // ==================== Pull-to-refresh (Requisito 7.2) ====================

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.blue_start),
                ContextCompat.getColor(requireContext(), R.color.blue_end));
        swipeRefresh.setOnRefreshListener(() -> viewModel.onRefresh());
    }

    // ==================== Observación de estado ====================

    private void observeState() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(PartidosUiState state) {
        if (state instanceof PartidosUiState.Loading) {
            renderLoading();
        } else if (state instanceof PartidosUiState.Content) {
            renderContent((PartidosUiState.Content) state);
        } else if (state instanceof PartidosUiState.Empty) {
            renderEmpty((PartidosUiState.Empty) state);
        } else if (state instanceof PartidosUiState.Error) {
            renderError((PartidosUiState.Error) state);
        }
    }

    private void renderLoading() {
        // Indicador de carga solo si aún no hay contenido visible (Requisito 9.1).
        boolean hasContent = adapter.getItemCount() > 0;
        progressLoading.setVisibility(hasContent ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        recyclerView.setVisibility(hasContent ? View.VISIBLE : View.GONE);
    }

    private void renderContent(PartidosUiState.Content content) {
        stopRefreshing();
        progressLoading.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        adapter.submitList(content.matches);

        // Banners de aviso: offline (R11.1) / cuota (R7.7).
        if (content.quotaNotice) {
            showNotice(getString(R.string.notice_quota));
        } else if (content.offlineNotice) {
            showNotice(getString(R.string.notice_offline));
        } else {
            hideNotice();
        }
    }

    private void renderEmpty(PartidosUiState.Empty empty) {
        stopRefreshing();
        progressLoading.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        hideNotice();

        // Mensaje específico de filtro (R9.2) vs búsqueda (R10.7).
        int messageRes = empty.type == PartidosUiState.Empty.Type.SEARCH
                ? R.string.empty_search
                : R.string.empty_filter;
        emptyView.setText(messageRes);
        emptyView.setVisibility(View.VISIBLE);
    }

    private void renderError(PartidosUiState.Error error) {
        stopRefreshing();
        progressLoading.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        hideNotice();

        errorText.setText(error.message);
        // La acción de reintentar solo se ofrece si el error es reintentable (R9.3, R9.4).
        if (error.retryable) {
            retryButton.setVisibility(View.VISIBLE);
            retryButton.setOnClickListener(v -> viewModel.onRetry());
        } else {
            retryButton.setVisibility(View.GONE);
            retryButton.setOnClickListener(null);
        }
        errorView.setVisibility(View.VISIBLE);
    }

    private void showNotice(String message) {
        noticeView.setText(message);
        noticeView.setVisibility(View.VISIBLE);
    }

    private void hideNotice() {
        noticeView.setVisibility(View.GONE);
    }

    private void stopRefreshing() {
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
    }

    // ==================== Ciclo de vida del polling (Requisitos 7B.1, 7B.2) ====================

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.startLivePolling();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (viewModel != null) {
            viewModel.stopLivePolling();
        }
    }
}
