package pl.jaboja.biblioteki.loans;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/**
 * DTO wypożyczenia zwracany przez REST API.
 * Nie jest encją JPA – dane nie są persystowane (cache in-memory).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Loan(
    String id,
    String libraryId,
    String libraryName,
    String title,
    String author,
    LocalDate dueDate,
    String location,
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
