package com.diyebure.golia.util;

/**
 * Resuelve el nombre a mostrar al usuario dando prioridad al nombre de usuario
 * sobre el nombre completo.
 *
 * <p>Regla única y compartida por la Pantalla_Perfil y la bienvenida de Inicio:
 * si hay un nombre de usuario no vacío se usa ese; en su defecto se usa el
 * nombre completo; y si ninguno de los dos está disponible se usa un valor por
 * defecto.
 *
 * <p>Método estático puro (sin dependencias de Android) para poder testearse en
 * JVM sin instrumentación.
 */
public final class DisplayName {

    private DisplayName() {
        // no-op: utility class
    }

    /**
     * Resuelve el nombre a mostrar con prioridad al nombre de usuario.
     *
     * @param username    nombre de usuario (puede ser {@code null} o vacío)
     * @param fullName    nombre completo (puede ser {@code null} o vacío)
     * @param defaultName valor por defecto cuando no hay usuario ni nombre
     * @return el username si no es vacío; en su defecto el fullName; y si ambos
     *         están vacíos, {@code defaultName}
     */
    public static String resolve(String username, String fullName, String defaultName) {
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }
        return defaultName;
    }
}
