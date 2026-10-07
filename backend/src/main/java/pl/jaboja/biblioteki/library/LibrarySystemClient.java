package pl.jaboja.biblioteki.library;

import pl.jaboja.biblioteki.loans.Loan;

import java.util.List;

/**
 * Interfejs klienta systemu bibliotecznego (wzorzec Strategy).
 * Każda implementacja obsługuje inny system (Primo, Integro, itp.).
 */
public interface LibrarySystemClient {

    /**
     * Loguje się do systemu i zwraca aktywną sesję.
     *
     * @param library definicja biblioteki
     * @param username nazwa użytkownika (lub numer czytelnika)
     * @param password hasło (lub PIN)
     * @return sesja z tokenem i klientem HTTP
     * @throws LibrarySystemException jeśli logowanie się nie powiedzie
     */
    LibrarySession login(LibraryDefinition library, String username, String password);

    /**
     * Pobiera listę aktywnych wypożyczeń dla sesji.
     *
     * @param session aktywna sesja
     * @return lista wypożyczeń
     * @throws LibrarySystemException jeśli pobieranie się nie powiedzie
     */
    List<Loan> fetchLoans(LibrarySession session);

    /**
     * Przedłuża wypożyczenie.
     *
     * @param session aktywna sesja
     * @param loanId identyfikator wypożyczenia
     * @throws LibrarySystemException jeśli prolongata się nie powiedzie
     */
    void renewLoan(LibrarySession session, String loanId);
}
