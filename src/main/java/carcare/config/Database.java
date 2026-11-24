package CarCareBackend.src.main.java.carcare.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String URL = env("DB_URL", "jdbc:mysql://localhost:3306/carcare?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true");
    private static final String USER = env("DB_USER", "root");
    private static final String PASSWORD = env("DB_PASSWORD", "");

    // order matters: a table must exist before another table points at it.
    // UNIQUE stops duplicate emails / registration numbers even if two requests arrive together,
    // and ON DELETE CASCADE removes a user's vehicles, requests, pings... when the user is deleted.
    private static final String[] SCHEMA = {
            "CREATE TABLE IF NOT EXISTS users (id VARCHAR(36) PRIMARY KEY, name VARCHAR(255), email VARCHAR(255) NOT NULL UNIQUE, phone_number VARCHAR(255), address VARCHAR(255), city VARCHAR(255), state VARCHAR(255), created_at DATETIME, updated_at DATETIME, "
                    + "password_hash VARCHAR(255), role VARCHAR(20) NOT NULL DEFAULT 'USER', busy_until DATETIME)",

            "CREATE TABLE IF NOT EXISTS service_centres (id VARCHAR(36) PRIMARY KEY, name VARCHAR(255), phone_number VARCHAR(255), email VARCHAR(255), address VARCHAR(255), city VARCHAR(255), state VARCHAR(255), postal_code VARCHAR(255), services VARCHAR(2000), vehicle_types VARCHAR(2000), emergency_service BOOLEAN, opening_time VARCHAR(255), closing_time VARCHAR(255), accepting_vehicles BOOLEAN, verified BOOLEAN, created_at DATETIME, updated_at DATETIME)",

            // busy_until: while it is in the future the mechanic is busy; once it passes they are free again
            "CREATE TABLE IF NOT EXISTS mechanics (id VARCHAR(36) PRIMARY KEY, name VARCHAR(255), phone_number VARCHAR(255), email VARCHAR(255), years_of_experience INT, service_centre_id VARCHAR(36), available BOOLEAN, accepting_requests BOOLEAN, busy_until DATETIME, address VARCHAR(255), city VARCHAR(255), state VARCHAR(255), postal_code VARCHAR(255), "
                    + "FOREIGN KEY (service_centre_id) REFERENCES service_centres(id) ON DELETE SET NULL)",

            "CREATE TABLE IF NOT EXISTS vehicles (id VARCHAR(36) PRIMARY KEY, owner_id VARCHAR(36) NOT NULL, registration_number VARCHAR(255) NOT NULL UNIQUE, manufacturer VARCHAR(255), model VARCHAR(255), manufacturing_year INT, fuel_type VARCHAR(255), vehicle_type VARCHAR(255), "
                    + "FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS service_visits (id VARCHAR(36) PRIMARY KEY, vehicle_id VARCHAR(36) NOT NULL, mechanic_id VARCHAR(36), service_centre_id VARCHAR(36) NOT NULL, service_type VARCHAR(255), description VARCHAR(2000), status VARCHAR(255), scheduled_at DATETIME, completed_at DATETIME, cost DOUBLE PRECISION, notes VARCHAR(2000), created_at DATETIME, updated_at DATETIME, "
                    + "FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (service_centre_id) REFERENCES service_centres(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (mechanic_id) REFERENCES mechanics(id) ON DELETE SET NULL)",

            "CREATE TABLE IF NOT EXISTS assistance_requests (id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL, vehicle_id VARCHAR(36) NOT NULL, mechanic_id VARCHAR(36), assistance_type VARCHAR(255), description VARCHAR(2000), status VARCHAR(255), address VARCHAR(255), city VARCHAR(255), state VARCHAR(255), requested_at DATETIME, accepted_at DATETIME, completed_at DATETIME, estimated_cost DOUBLE PRECISION, final_cost DOUBLE PRECISION, created_at DATETIME, updated_at DATETIME, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (mechanic_id) REFERENCES mechanics(id) ON DELETE SET NULL)",

            "CREATE TABLE IF NOT EXISTS pings (id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL, vehicle_id VARCHAR(36) NOT NULL, title VARCHAR(255), description VARCHAR(2000), status VARCHAR(255), address VARCHAR(255), city VARCHAR(255), state VARCHAR(255), accepted_by VARCHAR(36), accepted_at DATETIME, completed_at DATETIME, created_at DATETIME, updated_at DATETIME, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (accepted_by) REFERENCES users(id) ON DELETE SET NULL)"
    };

    // CREATE TABLE IF NOT EXISTS never changes a table that already exists, so a column added later
    // (like mechanics.busy_until) would be missing and every query touching it would fail with HTTP 500.
    // These are added automatically at startup when they are missing: {table, column, definition}.
    private static final String[][] COLUMNS = {
            {"users", "password_hash", "VARCHAR(255)"},
            {"users", "role", "VARCHAR(20) NOT NULL DEFAULT 'USER'"},
            {"users", "busy_until", "DATETIME"},
            {"mechanics", "available", "BOOLEAN DEFAULT TRUE"},
            {"mechanics", "accepting_requests", "BOOLEAN DEFAULT TRUE"},
            {"mechanics", "busy_until", "DATETIME"}
    };

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    // a new connection per call keeps things simple and thread-safe
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // creates the tables on first start
    public static void init() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : SCHEMA) {
                statement.execute(sql);
            }
            for (String[] column : COLUMNS) {
                addColumnIfMissing(connection, statement, column[0], column[1], column[2]);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialise database", e);
        }
    }

    private static void addColumnIfMissing(Connection connection, Statement statement,
                                           String table, String column, String definition) throws SQLException {
        String check = "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (java.sql.PreparedStatement ps = connection.prepareStatement(check)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                    System.out.println("Database updated: added " + table + "." + column);
                }
            }
        }
    }
}
