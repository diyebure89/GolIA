package com.diyebure.golia.presentation.ui.common;

/**
 * Navigation destinations for the bottom navigation bar.
 */
public enum Screen {
    HOME("Inicio", com.diyebure.golia.R.id.nav_home),
    MATCHES("Partidos", com.diyebure.golia.R.id.nav_matches),
    PREDICTIONS("Pronósticos", com.diyebure.golia.R.id.nav_predictions),
    PROFILE("Perfil", com.diyebure.golia.R.id.nav_profile);

    private final String title;
    private final int menuItemId;

    Screen(String title, int menuItemId) {
        this.title = title;
        this.menuItemId = menuItemId;
    }

    public String getTitle() {
        return title;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    /**
     * Returns the fragment class associated with this screen.
     */
    public Class<?> getFragmentClass() {
        switch (this) {
            case HOME:
                return com.diyebure.golia.InicioFragment.class;
            case MATCHES:
                return com.diyebure.golia.PartidosFragment.class;
            case PREDICTIONS:
                return com.diyebure.golia.RankingFragment.class;
            case PROFILE:
                return com.diyebure.golia.PerfilFragment.class;
            default:
                throw new IllegalArgumentException("Unknown screen: " + this);
        }
    }
}