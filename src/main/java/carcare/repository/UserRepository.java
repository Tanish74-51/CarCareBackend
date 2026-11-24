package CarCareBackend.src.main.java.carcare.repository;

import CarCareBackend.src.main.java.carcare.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class UserRepository extends BaseRepository {

    // "free" = not already helping someone (busy_until empty or in the past); needs one parameter: now
    private static final String FREE = "(busy_until IS NULL OR busy_until<=?)";

    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(uuid(rs, "id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setAddress(rs.getString("address"));
        user.setCity(rs.getString("city"));
        user.setState(rs.getString("state"));
        String role = rs.getString("role");
        user.setRole(role == null ? User.USER : role);
        user.setPasswordHash(rs.getString("password_hash"));
        user.setBusyUntil(time(rs, "busy_until"));
        user.setCreatedAt(time(rs, "created_at"));
        user.setUpdatedAt(time(rs, "updated_at"));
        return user;
    }

    public List<User> getAllUsers() {
        return query("SELECT * FROM users", this::map);
    }

    public User getUser(UUID id) {
        return queryOne("SELECT * FROM users WHERE id=?", this::map, id);
    }

    public void createUser(User user) {
        execute("INSERT INTO users (id, name, email, phone_number, address, city, state, role, password_hash, created_at, updated_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                user.getId(), user.getName(), user.getEmail(), user.getPhoneNumber(), user.getAddress(), user.getCity(), user.getState(),
                user.getRole(), user.getPasswordHash(), user.getCreatedAt(), user.getUpdatedAt());
    }

    // the password, role and busy timer are deliberately not part of this UPDATE
    public void replaceUser(UUID id, User user) {
        execute("UPDATE users SET name=?, email=?, phone_number=?, address=?, city=?, state=?, created_at=?, updated_at=? WHERE id=?",
                user.getName(), user.getEmail(), user.getPhoneNumber(), user.getAddress(), user.getCity(), user.getState(), user.getCreatedAt(), user.getUpdatedAt(), id);
    }

    public void deleteUser(UUID id) {
        execute("DELETE FROM users WHERE id=?", id);
    }

    // emails are stored lower-case, so a plain comparison can use the UNIQUE index
    public User getUserByEmail(String email) {
        return queryOne("SELECT * FROM users WHERE email=?", this::map, email == null ? null : email.toLowerCase());
    }

    // how many other users live in this city and state (to tell "nobody lives here" from "everybody is busy")
    public int countOthersInCityAndState(String city, String state, UUID excludedUserId) {
        return query("SELECT id FROM users WHERE LOWER(city)=LOWER(?) AND LOWER(state)=LOWER(?) AND id<>?",
                rs -> uuid(rs, "id"), city, state, excludedUserId).size();
    }

    // other users in the same city and state who are not already busy helping somebody
    public List<User> getFreeUsersByCityAndState(String city, String state, UUID excludedUserId) {
        return query("SELECT * FROM users WHERE LOWER(city)=LOWER(?) AND LOWER(state)=LOWER(?) AND id<>? AND " + FREE,
                this::map, city, state, excludedUserId, LocalDateTime.now());
    }

    // Atomic: only one caller can win a free user, so two pings never get the same helper at once.
    public boolean markBusy(UUID id, LocalDateTime until) {
        return execute("UPDATE users SET busy_until=? WHERE id=? AND " + FREE, until, id, LocalDateTime.now()) == 1;
    }

    public void markFree(UUID id) {
        execute("UPDATE users SET busy_until=NULL WHERE id=?", id);
    }
}
