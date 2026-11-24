package CarCareBackend.src.main.java.carcare.repository;

import CarCareBackend.src.main.java.carcare.config.Database;
import CarCareBackend.src.main.java.carcare.util.Json;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** The small bit of JDBC every repository shares. */
public abstract class BaseRepository {

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    // first row, or null when nothing matches
    protected <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> result = query(sql, mapper, params);
        return result.isEmpty() ? null : result.get(0);
    }

    protected int execute(String sql, Object... params) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            // duplicate email / registration number, or a link to a record that does not exist
            throw new IllegalStateException(e.getErrorCode() == 1062
                    ? "This value is already in use" : "This record conflicts with other data");
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    private void bind(PreparedStatement statement, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value instanceof UUID) {
                value = value.toString();
            } else if (value instanceof LocalDateTime time) {
                value = Timestamp.valueOf(time);
            } else if (value instanceof List) {
                value = Json.toJson(value);
            }
            statement.setObject(i + 1, value);
        }
    }

    protected static UUID uuid(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : UUID.fromString(value);
    }

    protected static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    protected static List<String> list(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(Json.read(value, String[].class)));
    }
}
