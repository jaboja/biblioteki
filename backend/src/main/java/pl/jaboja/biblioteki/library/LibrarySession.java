package pl.jaboja.biblioteki.library;

import java.net.http.HttpClient;

/**
 * Sesja do systemu bibliotecznego.
 * Zawiera token autentykacyjny i klient HTTP do komunikacji z API.
 */
public record LibrarySession(
    LibraryDefinition library,
    String token,
    HttpClient client
) {
}
