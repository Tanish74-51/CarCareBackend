package CarCareBackend.src.main.java.carcare.util;

/** Wrong email/password or missing login: the router turns this into HTTP 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
