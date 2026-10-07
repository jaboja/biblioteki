package pl.jaboja.biblioteki.library;

/**
 * Wyjątek rzucany przez klientów systemów bibliotecznych.
 */
public class LibrarySystemException extends RuntimeException {

    public LibrarySystemException(String message) {
        super(message);
    }

    public LibrarySystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
