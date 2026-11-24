package CarCareBackend.src.main.java.carcare.repository;

import CarCareBackend.src.main.java.carcare.model.Ping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PingRepository extends BaseRepository {

    private Ping map(ResultSet rs) throws SQLException {
        Ping ping = new Ping();
        ping.setId(uuid(rs, "id"));
        ping.setUserId(uuid(rs, "user_id"));
        ping.setVehicleId(uuid(rs, "vehicle_id"));
        ping.setTitle(rs.getString("title"));
        ping.setDescription(rs.getString("description"));
        ping.setStatus(rs.getString("status"));
        ping.setAddress(rs.getString("address"));
        ping.setCity(rs.getString("city"));
        ping.setState(rs.getString("state"));
        ping.setAcceptedBy(uuid(rs, "accepted_by"));
        ping.setAcceptedAt(time(rs, "accepted_at"));
        ping.setCompletedAt(time(rs, "completed_at"));
        ping.setCreatedAt(time(rs, "created_at"));
        ping.setUpdatedAt(time(rs, "updated_at"));
        return ping;
    }

    public List<Ping> getAllPings() {
        return query("SELECT * FROM pings", this::map);
    }

    public Ping getPing(UUID id) {
        return queryOne("SELECT * FROM pings WHERE id=?", this::map, id);
    }

    public void createPing(Ping ping) {
        execute("INSERT INTO pings (id, user_id, vehicle_id, title, description, status, address, city, state, accepted_by, accepted_at, completed_at, created_at, updated_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                ping.getId(), ping.getUserId(), ping.getVehicleId(), ping.getTitle(), ping.getDescription(), ping.getStatus(), ping.getAddress(), ping.getCity(), ping.getState(), ping.getAcceptedBy(), ping.getAcceptedAt(), ping.getCompletedAt(), ping.getCreatedAt(), ping.getUpdatedAt());
    }

    public void replacePing(UUID id, Ping ping) {
        execute("UPDATE pings SET user_id=?, vehicle_id=?, title=?, description=?, status=?, address=?, city=?, state=?, accepted_by=?, accepted_at=?, completed_at=?, created_at=?, updated_at=? WHERE id=?",
                ping.getUserId(), ping.getVehicleId(), ping.getTitle(), ping.getDescription(), ping.getStatus(), ping.getAddress(), ping.getCity(), ping.getState(), ping.getAcceptedBy(), ping.getAcceptedAt(), ping.getCompletedAt(), ping.getCreatedAt(), ping.getUpdatedAt(), id);
    }

    public void deletePing(UUID id) {
        execute("DELETE FROM pings WHERE id=?", id);
    }

    // pings I created plus pings where I am the helper, newest first
    public List<Ping> getPingsForUser(UUID userId) {
        return query("SELECT * FROM pings WHERE user_id=? OR accepted_by=? ORDER BY created_at DESC", this::map, userId, userId);
    }

    // Atomic: gives a still-waiting ping its helper. Returns false if another call already did it.
    public boolean claimPing(UUID id, UUID helperId, LocalDateTime now) {
        return execute("UPDATE pings SET accepted_by=?, accepted_at=?, status='ACCEPTED', updated_at=? "
                + "WHERE id=? AND status='ACTIVE'", helperId, now, now, id) == 1;
    }

    // pings a user is still helping with (other than one ping), used before freeing them
    public List<Ping> getActiveForHelper(UUID helperId, UUID excludedPingId) {
        return query("SELECT * FROM pings WHERE accepted_by=? AND id<>? AND status='ACCEPTED'", this::map, helperId, excludedPingId);
    }
}
