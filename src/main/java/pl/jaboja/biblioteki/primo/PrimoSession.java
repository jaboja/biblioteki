package pl.jaboja.biblioteki.primo;

import pl.jaboja.biblioteki.library.LibraryDefinition;

import java.net.http.HttpClient;

/**
 * Wynik logowania: definicja biblioteki, token JWT i klient HTTP
 * z ciasteczkami sesji.  Nie jest serializowany ani cache'owany –
 * używany jednorazowo do fetchLoans i potem porzucany.
 */
public record PrimoSession(
    LibraryDefinition library,
    String jwt,
    HttpClient client
) {}
