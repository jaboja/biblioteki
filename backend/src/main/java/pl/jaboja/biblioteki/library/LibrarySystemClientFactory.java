package pl.jaboja.biblioteki.library;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fabryka klientów systemów bibliotecznych.
 * Tworzy odpowiednią implementację LibrarySystemClient na podstawie typu systemu.
 */
@Component
@RequiredArgsConstructor
public class LibrarySystemClientFactory {

    private final List<LibrarySystemClient> clients;

    /**
     * Zwraca klienta dla danego typu systemu.
     *
     * @param systemType typ systemu bibliotecznego
     * @return implementacja LibrarySystemClient
     * @throws IllegalArgumentException jeśli nie znaleziono klienta dla danego typu
     */
    public LibrarySystemClient getClient(LibrarySystemType systemType) {
        Map<LibrarySystemType, LibrarySystemClient> clientMap = clients.stream()
            .collect(Collectors.toMap(
                this::getSystemType,
                Function.identity()
            ));

        LibrarySystemClient client = clientMap.get(systemType);
        if (client == null) {
            throw new IllegalArgumentException("No LibrarySystemClient found for system type: " + systemType);
        }
        return client;
    }

    /**
     * Określa typ systemu na podstawie klasy implementacji.
     * Każda implementacja powinna obsługiwać dokładnie jeden typ systemu.
     */
    private LibrarySystemType getSystemType(LibrarySystemClient client) {
        if (client instanceof pl.jaboja.biblioteki.primo.PrimoSystemClient) {
            return LibrarySystemType.PRIMO;
        } else if (client instanceof pl.jaboja.biblioteki.integro.IntegroSystemClient) {
            return LibrarySystemType.INTEGRO;
        }
        throw new IllegalArgumentException("Unknown LibrarySystemClient implementation: " + client.getClass().getName());
    }

    /**
     * Zwraca klienta dla danej definicji biblioteki.
     *
     * @param library definicja biblioteki
     * @return implementacja LibrarySystemClient
     */
    public LibrarySystemClient getClient(LibraryDefinition library) {
        return getClient(library.getSystemType());
    }
}
