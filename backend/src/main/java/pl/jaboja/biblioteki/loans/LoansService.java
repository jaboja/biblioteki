package pl.jaboja.biblioteki.loans;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pl.jaboja.biblioteki.account.LibraryAccount;
import pl.jaboja.biblioteki.account.LibraryAccountRepository;
import pl.jaboja.biblioteki.config.CacheConfig;
import pl.jaboja.biblioteki.primo.PrimoAuthService;
import pl.jaboja.biblioteki.primo.PrimoException;
import pl.jaboja.biblioteki.primo.PrimoLoansService;

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
    private final PrimoAuthService authService;
    private final PrimoLoansService loansService;

    // Wirtualne wątki Java 21 – idealne do I/O-bound równoległych żądań HTTP
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Zwraca posortowaną listę wszystkich aktywnych wypożyczeń.
     *
     * Klucz cache'a = stały string "all" – wszyscy użytkownicy usługi
     * widzą te same dane.  TTL = 10 minut (ustawiony w CacheConfig).
     *
     * Wywołanie @CacheEvict przed ponownym pobraniem obsługuje
     * endpoint /api/loans/refresh.
     */
    @Cacheable(value = CacheConfig.LOANS_CACHE, key = "'all'")
    public LoansResult fetchAll() {
        List<LibraryAccount> accounts = accountRepo.findByEnabledTrue();
        if (accounts.isEmpty()) {
            return new LoansResult(List.of(), List.of());
        }

        // Pobierz równolegle – po jednym wirtualnym wątku na konto
        List<CompletableFuture<AccountResult>> futures = accounts.stream()
            .map(account -> CompletableFuture.supplyAsync(
                () -> fetchForAccount(account), executor))
            .toList();

        List<Loan> allLoans   = new ArrayList<>();
        List<String> errors   = new ArrayList<>();

        for (var future : futures) {
            try {
                AccountResult result = future.join();
                allLoans.addAll(result.loans());
                if (result.error() != null) errors.add(result.error());
            } catch (Exception e) {
                errors.add("Unexpected error: " + e.getMessage());
                log.error("Unexpected error joining future", e);
            }
        }

        Collections.sort(allLoans);
        return new LoansResult(allLoans, errors);
    }

    /** Evictuje cache i wywołuje fetchAll() na świeżo. */
    @CacheEvict(value = CacheConfig.LOANS_CACHE, key = "'all'")
    public LoansResult refresh() {
        log.info("Cache evicted – refreshing loans");
        return fetchAll();
    }

    // --- Prywatne ---

    private AccountResult fetchForAccount(LibraryAccount account) {
        try {
            var session = authService.login(
                account.getLibrary(), account.getUsername(), account.getPassword());
            var loans = loansService.fetchLoans(session);
            return new AccountResult(loans, null);
        } catch (PrimoException e) {
            log.warn("Failed to fetch loans for {} ({}): {}",
                account.getUsername(), account.getLibrary().name(), e.getMessage());
            return new AccountResult(List.of(),
                account.getLibrary().getDisplayName() + ": " + e.getMessage());
        }
    }

    // --- Pomocnicze rekordy (wewnętrzne) ---

    private record AccountResult(List<Loan> loans, String error) {}

    /** Publiczny wynik – lista pożyczonych + ewentualne błędy per biblioteka. */
    public record LoansResult(List<Loan> loans, List<String> errors) {}
}
