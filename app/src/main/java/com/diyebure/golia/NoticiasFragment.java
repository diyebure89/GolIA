package com.diyebure.golia;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.diyebure.golia.domain.news.LeagueNewsQuery;
import com.diyebure.golia.presentation.adapter.NewsAdapter;
import com.diyebure.golia.presentation.ui.common.BasePlaceholderFragment;
import com.diyebure.golia.presentation.ui.news.ArticleUiModel;
import com.diyebure.golia.presentation.ui.news.ChipLiga;
import com.diyebure.golia.presentation.ui.news.EstadoUi;
import com.diyebure.golia.presentation.ui.news.NoticiasViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment de la Pantalla_Noticias (football-news-feed, tarea 13.1).
 *
 * <p>Fina capa de vista: observa {@code LiveData<EstadoUi>} y
 * {@code LiveData<List<ChipLiga>>} desde {@link NoticiasViewModel} (obtenido vía Hilt) y
 * delega toda acción de usuario en el ViewModel, sin invocar API ni repositorio
 * directamente (R4.2). No retiene el ViewModel fuera del ciclo de vida de la vista y
 * observa con {@code getViewLifecycleOwner()} (R11.1).</p>
 *
 * <ul>
 *     <li>Renderiza la fila de chips desde el ViewModel (fuente de verdad de ligas),
 *     con {@code Chip_Todos} por defecto y selección única (R2.1, R2.2, R2.3). El tap en
 *     un chip se delega vía {@link NoticiasViewModel#onChipSelected(String)}.</li>
 *     <li>Pull-to-refresh vía {@link SwipeRefreshLayout} delegado a
 *     {@link NoticiasViewModel#onRefresh()} (R2.7).</li>
 *     <li>Icono de búsqueda que alterna el campo de texto (R8.1); el texto crudo se
 *     reenvía a {@link NoticiasViewModel#onSearchQueryChanged(String)} (el debounce vive
 *     en el ViewModel), y al cerrar se llama a {@link NoticiasViewModel#onSearchClosed()}.</li>
 *     <li>Tap en una tarjeta: abre la URL del artículo en el navegador externo vía
 *     {@code Intent.ACTION_VIEW} (R7.7); si no hay app capaz, muestra un aviso y no cambia
 *     la pantalla (R7.8).</li>
 *     <li>Traduce el {@link EstadoUi} a lista/estados/banners (R10.1).</li>
 * </ul>
 */
@AndroidEntryPoint
public class NoticiasFragment extends BasePlaceholderFragment {

    private NoticiasViewModel viewModel;
    private NewsAdapter adapter;

    private ChipGroup chipGroup;
    private Chip chipTodos;

    private EditText searchInput;
    private ImageButton searchButton;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressLoading;
    private TextView emptyLeagueView;
    private TextView emptySearchView;
    private TextView noticeView;
    private View errorView;
    private TextView errorText;
    private Button retryButton;

    /** Evita re-disparar la selección cuando el render marca el chip programáticamente. */
    private boolean suppressChipCallback;

    public NoticiasFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_noticias, container, false);
    }

    @Override
    protected void bindPlaceholderData() {
        View view = requireView();
        initViews(view);
        setupRecycler();
        setupSearch();
        setupSwipeRefresh();

        viewModel = new ViewModelProvider(this).get(NoticiasViewModel.class);
        observeChips();
        observeState();
    }

    private void initViews(View view) {
        chipGroup = view.findViewById(R.id.chip_group_leagues);
        chipTodos = view.findViewById(R.id.chip_todos);

        searchInput = view.findViewById(R.id.edit_search_news);
        searchButton = view.findViewById(R.id.btn_search);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        recyclerView = view.findViewById(R.id.recycler_news);
        progressLoading = view.findViewById(R.id.progress_loading);
        emptyLeagueView = view.findViewById(R.id.empty_league_view);
        emptySearchView = view.findViewById(R.id.empty_search_view);
        noticeView = view.findViewById(R.id.text_notice);
        errorView = view.findViewById(R.id.error_view);
        errorText = view.findViewById(R.id.text_error);
        retryButton = view.findViewById(R.id.btn_retry);
    }

    private void setupRecycler() {
        // Tap en tarjeta → abrir la URL del artículo en navegador externo (R7.7, R7.8).
        adapter = new NewsAdapter(this::openArticle);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    // ==================== Chips (R2.1, R2.2, R2.3) ====================

    /**
     * Observa la fila de chips derivada por el ViewModel (fuente de verdad de ligas) y la
     * reconstruye dentro del {@link ChipGroup}. El {@code Chip_Todos} declarado en el layout
     * se reutiliza como primera posición; el resto se añade dinámicamente. La selección se
     * delega en {@link NoticiasViewModel#onChipSelected(String)} (R2.2).
     */
    private void observeChips() {
        viewModel.getChips().observe(getViewLifecycleOwner(), this::renderChips);
    }

    private void renderChips(@Nullable List<ChipLiga> chips) {
        if (chips == null) {
            return;
        }
        suppressChipCallback = true;
        chipGroup.setOnCheckedStateChangeListener(null);

        // Reconstruir la fila: conservar Chip_Todos (índice 0) y regenerar el resto.
        chipGroup.removeAllViews();
        for (ChipLiga chip : chips) {
            Chip view = buildOrReuseChip(chip);
            chipGroup.addView(view);
            if (chip.selected) {
                view.setChecked(true);
                highlightChip(view, true);
            } else {
                highlightChip(view, false);
            }
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (suppressChipCallback) {
                return;
            }
            int checkedId = checkedIds.isEmpty() ? View.NO_ID : checkedIds.get(0);
            if (checkedId == View.NO_ID) {
                return;
            }
            Chip selected = group.findViewById(checkedId);
            if (selected == null) {
                return;
            }
            highlightChips(group, checkedId);
            Object key = selected.getTag();
            if (key instanceof String) {
                viewModel.onChipSelected((String) key);
            }
        });
        suppressChipCallback = false;
    }

    /**
     * Reutiliza el {@code Chip_Todos} del layout para el primer chip (clave "all") o crea un
     * chip nuevo con el mismo estilo para las ligas. La clave textual se guarda como tag para
     * delegarla al ViewModel en la selección.
     */
    private Chip buildOrReuseChip(ChipLiga model) {
        Chip chip;
        if (LeagueNewsQuery.KEY_ALL.equals(model.leagueKey) && chipTodos != null) {
            chip = chipTodos;
        } else {
            chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setClickable(true);
        }
        chip.setText(model.label);
        chip.setTag(model.leagueKey);
        // contentDescription no vacía en cada chip (R13.2).
        chip.setContentDescription(model.label);
        return chip;
    }

    private void highlightChips(ChipGroup group, int activeId) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip) {
                highlightChip((Chip) child, child.getId() == activeId);
            }
        }
    }

    /** Resalta el chip activo con {@code blue_end} y atenúa el resto (coherente con Partidos). */
    private void highlightChip(Chip chip, boolean active) {
        int bg = ContextCompat.getColor(requireContext(),
                active ? R.color.blue_end : R.color.blue_dark);
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(bg));
        chip.setTextColor(active
                ? ContextCompat.getColor(requireContext(), R.color.blue_dark)
                : Color.WHITE);
    }

    // ==================== Búsqueda (R8.1) ====================

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
                // El debounce de 300 ms y el corte de longitud viven en el ViewModel.
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
            // Al ocultar: limpiar el texto y restaurar la lista completa del chip (R8.8).
            searchInput.setText("");
            viewModel.onSearchClosed();
        }
    }

    // ==================== Pull-to-refresh (R2.7) ====================

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.blue_start),
                ContextCompat.getColor(requireContext(), R.color.blue_end));
        swipeRefresh.setOnRefreshListener(() -> viewModel.onRefresh());
    }

    // ==================== Apertura de artículo (R7.7, R7.8) ====================

    /**
     * Abre la URL del artículo en el navegador externo vía {@code Intent.ACTION_VIEW}
     * (R7.7). Si no hay ninguna app capaz de resolver el intent, muestra un aviso y no
     * cambia la pantalla (R7.8). No invoca API ni repositorio (R4.2).
     */
    private void openArticle(@NonNull ArticleUiModel article) {
        if (TextUtils.isEmpty(article.articleUrl)) {
            showNoBrowserMessage();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.articleUrl));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No hay navegador/app capaz: aviso y sin cambio de pantalla (R7.8).
            showNoBrowserMessage();
        }
    }

    private void showNoBrowserMessage() {
        if (getContext() == null) {
            return;
        }
        Toast.makeText(getContext(), R.string.error_abrir_noticia, Toast.LENGTH_SHORT).show();
    }

    // ==================== Observación de estado (R10.1) ====================

    private void observeState() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@Nullable EstadoUi state) {
        if (state == null) {
            return;
        }

        // Lista de artículos.
        adapter.submitList(state.items);

        boolean hasContent = !state.items.isEmpty();

        // Indicador de carga solo si aún no hay contenido visible (R9.1).
        if (state.loading && !hasContent) {
            progressLoading.setVisibility(View.VISIBLE);
        } else {
            progressLoading.setVisibility(View.GONE);
            stopRefreshing();
        }

        // Estados vacíos y error.
        boolean showLeagueEmpty = state.empty == EstadoUi.EmptyKind.LEAGUE && !hasContent;
        boolean showSearchEmpty = state.empty == EstadoUi.EmptyKind.SEARCH && !hasContent;
        boolean showError = state.banner == EstadoUi.Banner.ERROR && !hasContent;

        emptyLeagueView.setVisibility(showLeagueEmpty ? View.VISIBLE : View.GONE);
        emptySearchView.setVisibility(showSearchEmpty ? View.VISIBLE : View.GONE);
        errorView.setVisibility(showError ? View.VISIBLE : View.GONE);
        if (showError) {
            errorText.setText(R.string.error_noticias);
            retryButton.setOnClickListener(v -> viewModel.onRetry());
        }

        recyclerView.setVisibility(hasContent ? View.VISIBLE : View.GONE);

        // Banner de aviso junto al contenido (OFFLINE / QUOTA / ERROR con caché) (R10.1-R10.3).
        switch (state.banner) {
            case OFFLINE:
                showNotice(getString(R.string.notice_offline_noticias));
                break;
            case QUOTA:
                showNotice(getString(R.string.notice_quota_noticias));
                break;
            case ERROR:
                // Con contenido cacheado, el error se muestra como banner sin ocultar la lista.
                if (hasContent) {
                    showNotice(getString(R.string.error_noticias));
                } else {
                    hideNotice();
                }
                break;
            case NINGUNO:
            default:
                hideNotice();
                break;
        }
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
}
