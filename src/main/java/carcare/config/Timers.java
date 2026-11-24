package CarCareBackend.src.main.java.carcare.config;

/** How long a mechanic / ping helper is held after being picked. Override with env variables if needed. */
public class Timers {

    public static final long MECHANIC_BUSY_SECONDS = seconds("MECHANIC_BUSY_SECONDS", 30);
    public static final long HELPER_BUSY_SECONDS = seconds("HELPER_BUSY_SECONDS", 30);

    private static long seconds(String name, long fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
