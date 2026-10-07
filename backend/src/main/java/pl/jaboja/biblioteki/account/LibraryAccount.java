package pl.jaboja.biblioteki.account;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import pl.jaboja.biblioteki.auth.User;
import pl.jaboja.biblioteki.library.LibraryDefinition;

@Entity
@Table(name = "library_account",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "library", "username"}))
@Getter
@Setter
public class LibraryAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

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
