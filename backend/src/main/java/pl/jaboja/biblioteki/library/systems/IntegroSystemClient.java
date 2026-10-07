package pl.jaboja.biblioteki.library.systems;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.jaboja.biblioteki.config.HttpConfig;
import pl.jaboja.biblioteki.library.LibraryDefinition;
import pl.jaboja.biblioteki.library.LibrarySession;
import pl.jaboja.biblioteki.library.LibrarySystemClient;
import pl.jaboja.biblioteki.library.LibrarySystemException;
import pl.jaboja.biblioteki.loans.Loan;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Klient dla systemu OPAC Integro.
 * Implementacja interfejsu LibrarySystemClient dla systemu Integro.
 */
@Slf4j
@Component
public class IntegroSystemClient implements LibrarySystemClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    @Override
    public LibrarySession login(LibraryDefinition library, String username, String password) {
        var cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var client = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .connectTimeout(HttpConfig.TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

        try {
            // Integro używa zwykle formularza logowania POST
            // Dla WIMBP Gorzów: https://opac.wimbp.gorzow.pl/integro/login
            var loginUrl = library.getBaseUrl() + "/login";
            
            var body = "login=" + username + "&password=" + password + "&submit=Zaloguj";

            var request = HttpRequest.newBuilder(URI.create(loginUrl))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", HttpConfig.USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "pl-PL,pl;q=0.9")
                .timeout(HttpConfig.TIMEOUT)
                .build();

            var resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new LibrarySystemException("Login HTTP " + resp.statusCode() + " for " + library.name());
            }

            // Sprawdź czy logowanie się powiodło (np. obecność ciasteczka sesji)
            // Dla Integro, sukces logowania można zweryfikować przez sprawdzenie
            // czy odpowiedź nie zawiera formularza logowania
            String responseBody = resp.body();
            if (responseBody.contains("login") || responseBody.contains("Zaloguj")) {
                throw new LibrarySystemException("Invalid credentials for " + library.name());
            }

            // Dla Integro tokenem jest zwykle ciasteczko sesji
            // Na razie zwracamy pusty token, klient HTTP zachowa ciasteczka
            String sessionToken = "integro_session";
            log.debug("Login successful for {} ({})", username, library.name());
            return new LibrarySession(library, sessionToken, client);

        } catch (LibrarySystemException e) {
            throw e;
        } catch (Exception e) {
            throw new LibrarySystemException("Login failed for " + library.name() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Loan> fetchLoans(LibrarySession session) {
        var lib = session.library();
        // Integro zwykle ma endpoint dla wypożyczeń np. /patron/loans
        // Dla WIMBP Gorzów: https://opac.wimbp.gorzow.pl/integro/patron/loans
        var url = lib.getBaseUrl() + "/patron/loans";

        try {
            var request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("User-Agent", HttpConfig.USER_AGENT)
                .header("Accept", "application/json, text/html, */*")
                .header("Accept-Language", "pl-PL,pl;q=0.9")
                .timeout(HttpConfig.TIMEOUT)
                .build();

            var resp = session.client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() == 401) {
                throw new LibrarySystemException("Session expired (401) for " + lib.name());
            }
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new LibrarySystemException("fetchLoans HTTP " + resp.statusCode() + " for " + lib.name());
            }

            // Parsuj odpowiedź HTML lub JSON w zależności od formatu
            // Dla Integro odpowiedź może być HTML, więc szukamy danych w strukturze HTML
            String responseBody = resp.body();
            
            // Jeśli odpowiedź jest JSON
            if (responseBody.trim().startsWith("{")) {
                return parseJsonLoans(responseBody, lib);
            } else {
                // Parsowanie HTML - na razie zwracamy pustą listę
                // Pełna implementacja wymagałaby parsera HTML
                log.warn("HTML response parsing not yet implemented for Integro, returning empty loans for {}", lib.name());
                return List.of();
            }

        } catch (LibrarySystemException e) {
            throw e;
        } catch (Exception e) {
            throw new LibrarySystemException("fetchLoans failed for " + lib.name() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void renewLoan(LibrarySession session, String loanId) {
        var lib = session.library();
        // Integro zwykle ma endpoint dla prolongaty np. /patron/loans/renew
        // loanId to identyfikator wypożyczenia (może być string lub numeric)
        var url = lib.getBaseUrl() + "/patron/loans/renew";

        try {
            var request = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString("loan_id=" + loanId))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", HttpConfig.USER_AGENT)
                .header("Accept", "application/json, text/html, */*")
                .header("Accept-Language", "pl-PL,pl;q=0.9")
                .timeout(HttpConfig.TIMEOUT)
                .build();

            var resp = session.client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() == 401) {
                throw new LibrarySystemException("Session expired (401) for " + lib.name());
            }
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new LibrarySystemException("renewLoan HTTP " + resp.statusCode() + " for " + lib.name());
            }

            log.info("Successfully renewed loan {} for {}", loanId, lib.name());

        } catch (LibrarySystemException e) {
            throw e;
        } catch (Exception e) {
            throw new LibrarySystemException("renewLoan failed for " + lib.name() + ": " + e.getMessage(), e);
        }
    }

    // --- Parsowanie JSON ---

    private List<Loan> parseJsonLoans(String json, LibraryDefinition library) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode loanArray = root.path("loans");
            
            if (!loanArray.isArray()) {
                log.debug("No loans array in response for {}", library.name());
                return List.of();
            }

            List<Loan> result = new ArrayList<>();
            for (JsonNode node : loanArray) {
                parseLoan(node, library.name(), library.getDisplayName(), library.getLocation()).ifPresent(result::add);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse loans JSON for {}: {}", library.name(), e.getMessage());
            return List.of();
        }
    }

    private java.util.Optional<Loan> parseLoan(
        JsonNode node, String libraryId, String libraryName, String defaultLocation
    ) {
        String title = textOf(node, "title", "(brak tytułu)");
        String author = textOf(node, "author", "");
        String loanId = textOf(node, "id", java.util.UUID.randomUUID().toString());
        String dueDateStr = textOf(node, "dueDate", null);
        String renewStr = textOf(node, "renewable", null);
        String location = textOf(node, "location", defaultLocation);

        LocalDate dueDate = null;
        if (dueDateStr != null) {
            for (var fmt : DATE_FORMATTERS) {
                try {
                    dueDate = LocalDate.parse(dueDateStr, fmt);
                    break;
                } catch (DateTimeParseException ignored) {}
            }
        }

        Boolean renew = "Y".equals(renewStr) || "true".equalsIgnoreCase(renewStr) ? Boolean.TRUE
                      : "N".equals(renewStr) || "false".equalsIgnoreCase(renewStr) ? Boolean.FALSE
                      : null;

        return java.util.Optional.of(new Loan(
            libraryId + "_" + loanId,
            libraryId,
            libraryName,
            title,
            author,
            dueDate,
            location,
            renew
        ));
    }

    private static String textOf(JsonNode node, String field, String defaultValue) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return defaultValue;
        if (n.isArray() && !n.isEmpty()) return n.get(0).asText(defaultValue);
        return n.asText(defaultValue);
    }
}
