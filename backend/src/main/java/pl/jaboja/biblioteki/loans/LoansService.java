package pl.jaboja.biblioteki.loans;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pl.jaboja.biblioteki.account.LibraryAccount;
import pl.jaboja.biblioteki.account.LibraryAccountRepository;
import pl.jaboja.biblioteki.auth.User;
import pl.jaboja.biblioteki.auth.UserRepository;
import pl.jaboja.biblioteki.config.CacheConfig;
import pl.jaboja.biblioteki.library.LibraryDefinition;
import pl.jaboja.biblioteki.library.LibrarySession;
import pl.jaboja.biblioteki.library.LibrarySystemClient;
import pl.jaboja.biblioteki.library.LibrarySystemClientFactory;
import pl.jaboja.biblioteki.library.LibrarySystemException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoansService {

    private final LibraryAccountRepository accountRepo;
    private final LibrarySystemClientFactory clientFactory;
    private final UserRepository userRepository;

    // Wirtualne wątki Java 21 – idealne do I/O-bound równoległych zadań HTTP
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Zwraca posortowaną listę wszystkich aktywnych wypożyczeń dla użytkownika.
     *
     * Klucz cache'a = userId – każdy użytkownik widzi tylko swoje dane. TTL = 10 minut (ustawiony w CacheConfig).
     *
     * Wywołanie @CacheEvict przed ponownym pobraniem obsługuje
     * endpoint /api/loans/refresh.
     */
    @Cacheable(value = CacheConfig.LOANS_CACHE, key = "#userId")
    public LoansResult fetchAll(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony: " + userId));
        
        List<LibraryAccount> accounts = accountRepo.findByUserAndEnabledTrue(user);
        if (accounts.isEmpty()) {
            return new LoansResult(List.of(), List.of());
        }

        // Pobierz równolegle – po jednym wirtualnym wątku na konto
        List<CompletableFuture<AccountResult>> futures = accounts.stream()
            .map(account -> CompletableFuture.supplyAsync(
                () -> fetchForAccount(account), executor))
            .toList();

        List<Loan> allLoans = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (var future : futures) {
            try {
                AccountResult result = future.join();
                allLoans.addAll(result.loans());
                if (result.error() != null) errors.add(result.error());
            } catch (Exception e) {
                errors.add("Niespodziewany błąd: " + e.getMessage());
                log.error("Niespodziewany błąd podczas łączenia future", e);
            }
        }

        Collections.sort(allLoans);
        return new LoansResult(allLoans, errors);
    }

    /** Evictuje cache i wywołuje fetchAll() na świeżo. */
    @CacheEvict(value = CacheConfig.LOANS_CACHE, key = "#userId")
    public LoansResult refresh(Long userId) {
        log.info("Cache evicted – refreshing loans for user {}", userId);
        return fetchAll(userId);
    }

    /**
     * Procesuje prolongatę pojedynczego wypożyczenia.
     * Znajduje konto powiązane z daną biblioteką i użytkownikiem i wykonuje renew.
     * 
     * @param userId ID użytkownika
     * @param loanId ID wypożyczenia w formacie "LIBRARYID_rawLoanId"
     * @throws LibrarySystemException gdy prolongata się nie powiedzie
     */
    public void renewLoan(Long userId, String loanId) {
        // Wyodrębnij libraryId z loanId (format: "LIBRARYID_rawLoanId")
        String libraryId = loanId.contains("_") ? 
            loanId.substring(0, loanId.indexOf("_")) : loanId;
        
        // Zamień libraryId string na LibraryDefinition enum
        LibraryDefinition library = null;
        try {
            library = LibraryDefinition.valueOf(libraryId);
        } catch (IllegalArgumentException e) {
            throw new LibrarySystemException("Nieznana biblioteka: " + libraryId);
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony: " + userId));
        
        // Znajdź aktywne konto dla tej biblioteki i użytkownika
        List<LibraryAccount> accounts = accountRepo.findByUserAndLibraryAndEnabledTrue(user, library);
        if (accounts.isEmpty()) {
            throw new LibrarySystemException("Brak aktywnego konta dla biblioteki: " + libraryId + " i użytkownika: " + userId);
        }
        
        // Użyj pierwszego aktywnego konta dla tej biblioteki
        LibraryAccount account = accounts.get(0);
        
        try {
            LibrarySystemClient client = clientFactory.getClient(library);
            LibrarySession session = client.login(
                account.getLibrary(), account.getUsername(), account.getPassword());
            client.renewLoan(session, loanId);
            // Po pomyślnej prolongacie odśwież cache
            refresh(userId);
            log.info("Pomyślnie przedłużono wypożyczenie {} przy użyciu konta {}", loanId, account.getUsername());
        } catch (LibrarySystemException e) {
            log.error("Błąd podczas przedłużania wypożyczenia {}: {}", loanId, e.getMessage());
            throw e;
        }
    }

    // --- Prywatne ---

    private AccountResult fetchForAccount(LibraryAccount account) {
        try {
            LibrarySystemClient client = clientFactory.getClient(account.getLibrary());
            LibrarySession session = client.login(
                account.getLibrary(), account.getUsername(), account.getPassword());
            List<Loan> loans = client.fetchLoans(session);
            return new AccountResult(loans, null);
        } catch (LibrarySystemException e) {
            log.warn("Błąd podczas pobierania wypożyczeń dla {} ({}): {}",
                account.getUsername(), account.getLibrary().name(), e.getMessage());
            return new AccountResult(List.of(),
                account.getLibrary().getDisplayName() + ": " + e.getMessage());
        }
    }

    // --- Pomocnicze rekordy (wewnętrzne) ---

    private record AccountResult(List<Loan> loans, String error) {}

    /** Publiczny wynik – lista pobranych + ewentualne błędy per biblioteka. */
    public record LoansResult(List<Loan> loans, List<String> errors) {}
}
