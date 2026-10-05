package pl.jaboja.biblioteki.account;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.jaboja.biblioteki.library.LibraryDefinition;

import java.util.List;
import java.util.Optional;

public interface LibraryAccountRepository extends JpaRepository<LibraryAccount, Long> {

    List<LibraryAccount> findByEnabledTrue();

    List<LibraryAccount> findByLibraryAndEnabledTrue(LibraryDefinition library);

    Optional<LibraryAccount> findByLibraryAndUsername(LibraryDefinition library, String username);
}
