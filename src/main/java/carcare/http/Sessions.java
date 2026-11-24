package CarCareBackend.src.main.java.carcare.http;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logging in (email + password, once) gives back a random UUID token; from then on the client only
 * sends that token. This map remembers who the token belongs to, their role, and when it expires.
 * It lives in memory, so restarting the server logs everybody out.
 */
public class Sessions {

    public record Session(UUID userId, String role, Instant expiresAt) {}

    private static final long TOKEN_HOURS = 12;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public static UUID login(UUID userId, String role) {
        UUID token = UUID.randomUUID();
        SESSIONS.put(token, new Session(userId, role, Instant.now().plusSeconds(TOKEN_HOURS * 3600)));
        return token;
    }

    // the session behind a token, or null when the token is missing / unknown / expired
    public static Session get(UUID token) {
        if (token == null) return null;
        Session session = SESSIONS.get(token);
        if (session != null && session.expiresAt().isBefore(Instant.now())) {
            SESSIONS.remove(token);
            return null;
        }
        return session;
    }

    public static void logout(UUID token) {
        if (token != null) SESSIONS.remove(token);
    }

    // every token of one user (used when the account is deleted)
    public static void logoutUser(UUID userId) {
        SESSIONS.values().removeIf(session -> session.userId().equals(userId));
    }
}
