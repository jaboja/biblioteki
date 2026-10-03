package pl.jaboja.biblioteki.primo;

public class PrimoException extends RuntimeException {
    public PrimoException(String message) { super(message); }
    public PrimoException(String message, Throwable cause) { super(message, cause); }
}
