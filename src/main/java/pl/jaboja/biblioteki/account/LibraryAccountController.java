package pl.jaboja.biblioteki.account;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.jaboja.biblioteki.library.LibraryDefinition;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class LibraryAccountController {

    private final LibraryAccountRepository repo;

    // --- DTO ---

    record AccountRequest(
        @NotBlank LibraryDefinition library,
        @NotBlank String username,
        @NotBlank String password,
        boolean enabled
    ) {}

    record AccountResponse(
        Long id,
        String library,
        String libraryDisplayName,
        String username,
        boolean enabled
    ) {
        static AccountResponse from(LibraryAccount a) {
            return new AccountResponse(
                a.getId(),
                a.getLibrary().name(),
                a.getLibrary().getDisplayName(),
                a.getUsername(),
                a.isEnabled()
            );
        }
    }

    // --- Endpointy ---

    /** GET /api/accounts – lista wszystkich kont (bez haseł) */
    @GetMapping
    public List<AccountResponse> list() {
        return repo.findAll().stream().map(AccountResponse::from).toList();
    }

    /** POST /api/accounts – dodaj konto */
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

    /** PATCH /api/accounts/{id} – aktualizuj (np. zmień hasło lub włącz/wyłącz) */
    @PatchMapping("/{id}")
    public AccountResponse update(@PathVariable Long id, @RequestBody AccountRequest req) {
        var account = repo.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Account not found: " + id));
        if (req.username() != null)  account.setUsername(req.username());
        if (req.password() != null && !req.password().isBlank()) account.setPassword(req.password());
        account.setEnabled(req.enabled());
        return AccountResponse.from(repo.save(account));
    }

    /** DELETE /api/accounts/{id} */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }

    record LibraryResponse(String id, String name, String location) {}

    /** GET /api/libraries – lista dostępnych definicji bibliotek */
    @GetMapping("/libraries")
    public List<LibraryResponse> libraries() {
        return List.of(LibraryDefinition.values()).stream()
            .map(l -> new LibraryResponse(l.name(), l.getDisplayName(), l.getLocation()))
            .toList();
    }
}
