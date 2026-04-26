package tn.esprit.spring.baladna.transport.exception;

public class DuplicateRouteException extends RuntimeException {
    public DuplicateRouteException(String message) {
        super(message);
    }
}