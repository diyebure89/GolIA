package com.diyebure.golia;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.diyebure.golia.presentation.ui.common.Screen;
import com.diyebure.golia.util.DebugTools;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Host único de navegación tras el login. Aloja el {@code fragment_container} y
 * la {@link BottomNavigationView} ({@code bottom_navigation}) con los cinco
 * destinos definidos en {@link Screen}.
 *
 * <p>La estrategia de navegación es show/hide por etiqueta (tag): cada fragment
 * se añade una única vez con un tag estable y se alterna su visibilidad con
 * {@code hide()}/{@code show()} (nunca {@code replace()} ni
 * {@code addToBackStack()}), lo que preserva el estado de vista de cada sección
 * y reutiliza las instancias.</p>
 *
 * <p>El manejo del botón atrás se realiza mediante {@link OnBackPressedCallback}
 * (registrado en {@code setupBackCallback()}): desde un destino distinto de
 * Inicio, el botón atrás lleva a Inicio (R9.3); desde Inicio, se delega la
 * salida por defecto del sistema (R9.4).</p>
 */
public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED = "selected_menu_id";

    private BottomNavigationView bottomNavigation;
    private int selectedMenuId = R.id.nav_inicio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        // El ColorStateList se aplica desde el XML (itemIconTint/itemTextColor);
        // no se fija tint en código.
        bottomNavigation.setOnItemSelectedListener(this::onDestinationSelected);

        // Solo en builds debug: mantener pulsado "Perfil" exporta la base de
        // datos a la carpeta de Descargas para abrirla con apps como SQLite
        // Editor. En release, DebugTools es no-op y no se registra el listener.
        if (DebugTools.isEnabled()) {
            bottomNavigation.findViewById(R.id.nav_perfil).setOnLongClickListener(v -> {
                DebugTools.exportDatabase(this);
                return true;
            });
        }

        if (savedInstanceState == null) {
            // Inicio por defecto (R3.1/R3.2)
            selectedMenuId = R.id.nav_inicio;
            selectDestination(Screen.INICIO);
            bottomNavigation.setSelectedItemId(R.id.nav_inicio);
        } else {
            // Restauración por tag tras recreación (R6.1/R6.2/R6.3/R6.5)
            selectedMenuId = savedInstanceState.getInt(KEY_SELECTED, R.id.nav_inicio);
            restoreVisibleFragment(selectedMenuId);
            bottomNavigation.setSelectedItemId(selectedMenuId);
        }

        setupBackCallback();
    }

    /**
     * Listener del menú: cambia de destino aplicando show/hide. La reselección
     * del destino actual devuelve {@code true} sin recrear el fragment (R2.7/R9.1).
     */
    private boolean onDestinationSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == selectedMenuId) {
            // Reselección: conservar fragment y estado sin recargar.
            return true;
        }
        Screen target = Screen.fromMenuId(id);
        if (target == null) {
            return false;
        }
        selectDestination(target);
        // R2.6: exactamente un destino seleccionado a la vez.
        selectedMenuId = id;
        return true;
    }

    /**
     * Añade (si falta) el fragment del destino por su tag y alterna la
     * visibilidad ocultando el visible actual. Nunca usa {@code replace()}.
     */
    private void selectDestination(Screen target) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();

        // Oculta el visible actual (si lo hay y no es el propio destino).
        Fragment current = fm.findFragmentByTag(screenTagFromMenuId(selectedMenuId));
        Fragment dest = fm.findFragmentByTag(target.getTag());
        if (current != null && current != dest) {
            tx.hide(current);
        }

        // Muestra o crea el destino.
        if (dest == null) {
            // Primer acceso: se añade la instancia con su tag estable.
            tx.add(R.id.fragment_container, target.newFragment(), target.getTag());
        } else {
            // Acceso posterior: reutiliza la instancia existente (R2.8).
            tx.show(dest);
        }

        tx.commit();
    }

    /**
     * Restaura el fragment visible tras la recreación de la actividad,
     * localizándolo por su tag. Muestra el destino restaurado y oculta los
     * demás; recrea de forma defensiva un fragment esperado que no aparezca.
     */
    private void restoreVisibleFragment(int menuId) {
        FragmentManager fm = getSupportFragmentManager();
        Screen visible = Screen.fromMenuId(menuId);
        if (visible == null) {
            visible = Screen.INICIO;
        }

        FragmentTransaction tx = fm.beginTransaction();
        for (Screen s : Screen.values()) {
            Fragment f = fm.findFragmentByTag(s.getTag());
            if (s == visible) {
                if (f == null) {
                    // Caso defensivo: el fragment esperado no fue recreado.
                    tx.add(R.id.fragment_container, s.newFragment(), s.getTag());
                } else {
                    tx.show(f);
                }
            } else if (f != null) {
                tx.hide(f);
            }
        }
        tx.commit();
    }

    /** @return el tag estable del destino asociado al id de menú (fallback Inicio). */
    private String screenTagFromMenuId(int menuId) {
        Screen s = Screen.fromMenuId(menuId);
        return s != null ? s.getTag() : Screen.INICIO.getTag();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Solo se persiste el id del destino seleccionado (R6.3); el estado de
        // vista de cada fragment lo conserva el propio FragmentManager.
        outState.putInt(KEY_SELECTED, selectedMenuId);
    }

    /**
     * Registra el manejo del botón atrás mediante {@link OnBackPressedCallback}
     * (sustituye al {@code onBackPressed()} deprecado). Si el destino actual no
     * es Inicio, el botón atrás selecciona Inicio (R9.3); si ya es Inicio, se
     * deshabilita el callback y se delega la salida por defecto del sistema (R9.4).
     */
    private void setupBackCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (selectedMenuId != R.id.nav_inicio) {
                    // Desde un destino distinto de Inicio, volver a Inicio (R9.3).
                    bottomNavigation.setSelectedItemId(R.id.nav_inicio);
                } else {
                    // Desde Inicio, delegar la salida por defecto del sistema (R9.4).
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }
}
