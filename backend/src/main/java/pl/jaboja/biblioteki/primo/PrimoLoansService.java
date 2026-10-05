package pl.jaboja.biblioteki.primo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.jaboja.biblioteki.loans.Loan;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PrimoLoansService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
        "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.4 Safari/605.1.15";

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    );

    /**
     * Pobiera aktywne wypożyczenia dla zalogowanej sesji.
     */
    public List<Loan> fetchLoans(PrimoSession session) {
        var lib = session.library();
        var url = lib.getBaseUrl()
            + "/primaws/rest/priv/myaccount/loans"
            + "?lang=pl&bulk=50&offset=1&type=active"
            + "&vid=" + lib.getVid()
            + "&inst_code=" + lib.getInstCode();

        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .GET()
            // Primo oczekuje tokenu w cudzysłowach w Authorization
            .header("Authorization",    "Bearer \"" + session.jwt() + "\"")
            .header("Accept",           "application/json, text/plain, */*")
            .header("Accept-Language",  "pl-PL,pl;q=0.9")
            .header("User-Agent",       USER_AGENT)
            .header("Sec-Fetch-Site",   "same-origin")
            .header("Sec-Fetch-Mode",   "cors")
            .header("Sec-Fetch-Dest",   "empty")
            .timeout(Duration.ofSeconds(15));
        if (lib.isNde()) {
            reqBuilder.header("is-nde", "true");
        }

        try {
            var resp = session.client().send(reqBuilder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() == 401) {
                throw new PrimoException("JWT expired (401) for " + lib.name());
            }
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new PrimoException("fetchLoans HTTP " + resp.statusCode() + " for " + lib.name());
            }

            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode loanArray = root.path("data").path("loans").path("loan");
            if (!loanArray.isArray()) {
                log.debug("No loans array in response for {}", lib.name());
                return List.of();
            }

            List<Loan> result = new ArrayList<>();
            for (JsonNode node : loanArray) {
                parseLoan(node, lib.name(), lib.name(), lib.getLocation()).ifPresent(result::add);
            }
            log.debug("Fetched {} loan(s) for {}", result.size(), lib.name());
            return result;

        } catch (PrimoException e) {
            throw e;
        } catch (Exception e) {
            throw new PrimoException("fetchLoans failed for " + lib.name() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Przetwarza prolongatę pojedynczego wypożyczenia.
     * Loan ID ma format "LIBRARYID_rawLoanId" – wyodrębniamy rawLoanId.
     */
    public void renewLoan(PrimoSession session, String loanId) {
        var lib = session.library();
        
        // loanID ma postać "LIBRARYID_rawLoanId" – wyodrębnij rawLoanId
        String rawId = loanId.contains("_") ? 
            loanId.substring(loanId.indexOf("_") + 1) : loanId;

        var url = lib.getBaseUrl()
            + "/primaws/rest/priv/myaccount/renew_loans"
            + "?lang=pl";

        try {
            var reqBuilder = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString("{\"id\":\"" + rawId + "\"}"))
                .header("Authorization",    "Bearer \"" + session.jwt() + "\"")
                .header("Content-Type",     "application/json;charset=utf-8")
                .header("Accept",           "application/json, text/plain, */*")
                .header("Accept-Language",  "pl-PL,pl;q=0.9")
                .header("User-Agent",       USER_AGENT)
                .header("Sec-Fetch-Site",   "same-origin")
                .header("Sec-Fetch-Mode",   "cors")
                .header("Sec-Fetch-Dest",   "empty")
                .timeout(Duration.ofSeconds(15));
            
            if (lib.isNde()) {
                reqBuilder.header("is-nde", "true");
            }

            var resp = session.client().send(reqBuilder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() == 401) {
                throw new PrimoException("JWT expired (401) for " + lib.name());
            }
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new PrimoException("renewLoan HTTP " + resp.statusCode() + " for " + lib.name());
            }

            log.info("Successfully renewed loan {} for {}", rawId, lib.name());

        } catch (PrimoException e) {
            throw e;
        } catch (Exception e) {
            throw new PrimoException("renewLoan failed for " + lib.name() + ": " + e.getMessage(), e);
        }
    }

    // --- Parsowanie ---

    private java.util.Optional<Loan> parseLoan(
        JsonNode node, String libraryId, String libraryName, String defaultLocation
    ) {
        String title   = textOf(node, "title", "(brak tytułu)");
        String author  = textOf(node, "author", "");
        String loanId  = textOf(node, "loanid", textOf(node, "id", java.util.UUID.randomUUID().toString()));
        String dueDateStr = textOf(node, "duedate", null);
        String renewStr   = textOf(node, "renew", null);
        String location   = stripPrefix(
            textOf(node, "mainlocationname", defaultLocation)
        );

        LocalDate dueDate = null;
        if (dueDateStr != null) {
            for (var fmt : DATE_FORMATTERS) {
                try {
                    // niektóre formattery mają datę + czas – bierzemy tylko datę
                    dueDate = LocalDate.parse(dueDateStr.length() > 10
                        ? dueDateStr.substring(0, 10) : dueDateStr, fmt);
                    break;
                } catch (DateTimeParseException ignored) {}
            }
        }

        Boolean renew = "Y".equals(renewStr) ? Boolean.TRUE
                      : "N".equals(renewStr) ? Boolean.FALSE
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
        // Primo zwraca pola jako tablice jednoelementowe lub stringi
        if (n.isArray() && n.size() > 0) return n.get(0).asText(defaultValue);
        return n.asText(defaultValue);
    }

    private static String stripPrefix(String location) {
        if (location != null && location.contains(" - ")) {
            return location.substring(location.indexOf(" - ") + 3);
        }
        return location;
    }
}
