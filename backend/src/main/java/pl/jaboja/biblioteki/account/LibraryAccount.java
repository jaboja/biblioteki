package pl.jaboja.biblioteki.account;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import pl.jaboja.biblioteki.library.LibraryDefinition;

@Entity
@Table(name = "library_account",
       uniqueConstraints = @UniqueConstraint(columnNames = {"library", "username"}))
@Getter
@Setter
public class LibraryAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LibraryDefinition library;

    @NotBlank
    @Column(nullable = false)
    private String username;

    /**
     * Hasło przechowywane w bazie danych.
     * W środowisku produkcyjnym zastąp szyfrowaniem kolumny
     * (np. @ColumnTransformer z pgcrypto lub Jasypt).
     */
    @NotBlank
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;
}
