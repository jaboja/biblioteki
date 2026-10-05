package pl.jaboja.biblioteki.loans;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * DTO wypożyczenia zwracany przez REST API.
 * Nie jest encją JPA – dane nie są persystowane (cache in-memory).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Wypożyczenie z biblioteki")
public record Loan(
    @Schema(description = "Unikalny identyfikator wypożyczenia")
    String id,
    @Schema(description = "Identyfikator biblioteki")
    String libraryId,
    @Schema(description = "Nazwa biblioteki")
    String libraryName,
    @Schema(description = "Tytuł publikacji")
    String title,
    @Schema(description = "Autor publikacji")
    String author,
    @Schema(description = "Data zwrotu", example = "2026-12-31")
    LocalDate dueDate,
    @Schema(description = "Lokalizacja wypożyczenia")
    String location,
    @Schema(description = "Czy wypożyczenie można przedłużyć")
    Boolean renewable    // null = nieznany status
) implements Comparable<Loan> {

    /** Sortowanie: najwcześniejszy termin zwrotu pierwszy, bez daty na końcu. */
    @Override
    public int compareTo(Loan other) {
        if (this.dueDate == null && other.dueDate == null) return this.title.compareTo(other.title);
        if (this.dueDate == null) return 1;
        if (other.dueDate == null) return -1;
        int cmp = this.dueDate.compareTo(other.dueDate);
        return cmp != 0 ? cmp : this.title.compareTo(other.title);
    }
}
