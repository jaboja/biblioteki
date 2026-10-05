package pl.jaboja.biblioteki.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.jaboja.biblioteki.library.LibraryDefinition;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Konta biblioteki", description = "Zarządzanie kontami dostępu do bibliotek")
public class LibraryAccountController {

    private final LibraryAccountRepository repo;

    // --- DTO ---

    @Schema(description = "Żądanie utworzenia lub aktualizacji konta")
    public record AccountRequest(
        @Schema(description = "Definicja biblioteki", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LibraryDefinition library,
        @Schema(description = "Nazwa użytkownika", requiredMode = Schema.RequiredMode.REQUIRED, example = "jan.kowalski")
        @NotBlank String username,
        @Schema(description = "Hasło", requiredMode = Schema.RequiredMode.REQUIRED, example = "tajne123")
        @NotBlank String password,
        @Schema(description = "Czy konto jest aktywne", defaultValue = "true")
        boolean enabled
    ) {}

    // --- Endpointy ---

    @Operation(
        summary = "Lista wszystkich kont",
        description = "Zwraca listę wszystkich skonfigurowanych kont dostępu do bibliotek (bez haseł)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lista kont",
                content = @Content(schema = @Schema(implementation = AccountResponse.class)))
        }
    )
    @GetMapping
    public List<AccountResponse> list() {
        return repo.findAll().stream().map(AccountResponse::from).toList();
    }

    @Operation(
        summary = "Dodaj nowe konto",
        description = "Tworzy nowe konto dostępu do biblioteki",
        responses = {
            @ApiResponse(responseCode = "201", description = "Konto utworzone",
                content = @Content(schema = @Schema(implementation = AccountResponse.class)))
        }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@RequestBody @Valid AccountRequest req) {
        var account = new LibraryAccount();
        account.setLibrary(req.library());
        account.setUsername(req.username());
        account.setPassword(req.password());
        account.setEnabled(req.enabled());
        return AccountResponse.from(repo.save(account));
    }

    @Operation(
        summary = "Aktualizuj konto",
        description = "Aktualizuje istniejące konto (można zmienić hasło, nazwę użytkownika lub status aktywności)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Konto zaktualizowane",
                content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Konto nie znalezione")
        }
    )
    @PatchMapping("/{id}")
    public AccountResponse update(@PathVariable Long id, @RequestBody AccountRequest req) {
        var account = repo.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Account not found: " + id));
        if (req.username() != null)  account.setUsername(req.username());
        if (req.password() != null && !req.password().isBlank()) account.setPassword(req.password());
        account.setEnabled(req.enabled());
        return AccountResponse.from(repo.save(account));
    }

    @Operation(
        summary = "Usuń konto",
        description = "Usuwa konto o podanym ID",
        responses = {
            @ApiResponse(responseCode = "204", description = "Konto usunięte"),
            @ApiResponse(responseCode = "404", description = "Konto nie znalezione")
        }
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }

    @Operation(
        summary = "Lista dostępnych bibliotek",
        description = "Zwraca listę wszystkich dostępnych definicji bibliotek",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lista bibliotek",
                content = @Content(schema = @Schema(implementation = LibraryResponse.class)))
        }
    )
    @GetMapping("/libraries")
    public List<LibraryResponse> libraries() {
        return Stream.of(LibraryDefinition.values())
            .map(l -> new LibraryResponse(l.name(), l.getDisplayName(), l.getLocation()))
            .toList();
    }
}
