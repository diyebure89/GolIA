package com.diyebure.golia.domain.security;

/**
 * Domain contract for deriving and verifying password credentials.
 * Kept in the domain layer so it stays independent of Room and Android APIs.
 */
public interface PasswordHasher {

    /** Deriva una credencial nueva (salt aleatorio) para la contraseña dada. */
    PasswordCredential hash(String plainPassword);

    /**
     * Recalcula el hash con los parámetros de la credencial y compara en
     * tiempo constante. No revela por qué falla.
     */
    boolean verify(PasswordCredential credential, String plainPassword);
}
