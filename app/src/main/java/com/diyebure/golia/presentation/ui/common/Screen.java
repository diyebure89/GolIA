package com.diyebure.golia.presentation.ui.common;

import androidx.fragment.app.Fragment;

import com.diyebure.golia.InicioFragment;
import com.diyebure.golia.NoticiasFragment;
import com.diyebure.golia.PartidosFragment;
import com.diyebure.golia.PerfilFragment;
import com.diyebure.golia.R;
import com.diyebure.golia.RankingFragment;

/**
 * Destinos de la barra de navegación inferior. Cada destino conoce su id de
 * menú, una etiqueta estable para el {@code FragmentManager} y una fábrica de
 * fragments (sin reflexión).
 */
public enum Screen {
    INICIO(R.id.nav_inicio,     "fragment_inicio",    InicioFragment::new),
    PARTIDOS(R.id.nav_partidos, "fragment_partidos",  PartidosFragment::new),
    NOTICIAS(R.id.nav_noticias, "fragment_noticias",  NoticiasFragment::new),
    RANKING(R.id.nav_ranking,   "fragment_ranking",   RankingFragment::new),
    PERFIL(R.id.nav_perfil,     "fragment_perfil",    PerfilFragment::new);

    /** Fábrica de fragments sin reflexión. */
    public interface FragmentFactory {
        Fragment create();
    }

    private final int menuItemId;
    private final String tag;
    private final FragmentFactory factory;

    Screen(int menuItemId, String tag, FragmentFactory factory) {
        this.menuItemId = menuItemId;
        this.tag = tag;
        this.factory = factory;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public String getTag() {
        return tag;
    }

    public Fragment newFragment() {
        return factory.create();
    }

    /**
     * @return el {@link Screen} asociado al id de menú, o {@code null} si el id
     *     no corresponde a ningún destino conocido.
     */
    public static Screen fromMenuId(int menuItemId) {
        for (Screen s : values()) {
            if (s.menuItemId == menuItemId) {
                return s;
            }
        }
        return null;
    }

    /** @return el destino por defecto (Inicio). */
    public static Screen defaultScreen() {
        return INICIO;
    }
}
