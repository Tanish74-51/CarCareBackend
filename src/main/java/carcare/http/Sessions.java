package CarCareBackend.src.main.java.carcare.http;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class Sessions {

    public record Session(UUID userId, String role, Instant expiresAt) {}

    private static final long TOKEN_HOURS = 12;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public static UUID login(UUID userId, String role) {
        UUID token=UUID.randomUUID();
        SESSIONS.put(token, new Session(userId, role, Instant.now().plusSeconds(TOKEN_HOURS * 3600)));
        return token;
    }

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

    public static void logoutUser(UUID userId) {
        SESSIONS.values().removeIf(session -> session.userId().equals(userId));
    }
}
