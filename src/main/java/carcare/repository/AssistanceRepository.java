package CarCareBackend.src.main.java.carcare.repository;

import CarCareBackend.src.main.java.carcare.model.Assistance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AssistanceRepository extends BaseRepository {

    private Assistance map(ResultSet rs) throws SQLException {
        Assistance assistance = new Assistance();
        assistance.setId(uuid(rs, "id"));
        assistance.setUserId(uuid(rs, "user_id"));
        assistance.setVehicleId(uuid(rs, "vehicle_id"));
        assistance.setMechanicId(uuid(rs, "mechanic_id"));
        assistance.setAssistanceType(rs.getString("assistance_type"));
        assistance.setDescription(rs.getString("description"));
        assistance.setStatus(rs.getString("status"));
        assistance.setAddress(rs.getString("address"));
        assistance.setCity(rs.getString("city"));
        assistance.setState(rs.getString("state"));
        assistance.setRequestedAt(time(rs, "requested_at"));
        assistance.setAcceptedAt(time(rs, "accepted_at"));
        assistance.setCompletedAt(time(rs, "completed_at"));
        assistance.setEstimatedCost(rs.getDouble("estimated_cost"));
        assistance.setFinalCost(rs.getDouble("final_cost"));
        assistance.setCreatedAt(time(rs, "created_at"));
        assistance.setUpdatedAt(time(rs, "updated_at"));
        return assistance;
    }

    public List<Assistance> getAllAssistanceRequests() {
        return query("SELECT * FROM assistance_requests ORDER BY created_at DESC", this::map);
    }

    public Assistance getAssistanceRequest(UUID id) {
        return queryOne("SELECT * FROM assistance_requests WHERE id=?", this::map, id);
    }

    public void createAssistanceRequest(Assistance assistance) {
        execute("INSERT INTO assistance_requests (id, user_id, vehicle_id, mechanic_id, assistance_type, description, status, address, city, state, requested_at, accepted_at, completed_at, estimated_cost, final_cost, created_at, updated_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                assistance.getId(), assistance.getUserId(), assistance.getVehicleId(), assistance.getMechanicId(), assistance.getAssistanceType(), assistance.getDescription(), assistance.getStatus(), assistance.getAddress(), assistance.getCity(), assistance.getState(), assistance.getRequestedAt(), assistance.getAcceptedAt(), assistance.getCompletedAt(), assistance.getEstimatedCost(), assistance.getFinalCost(), assistance.getCreatedAt(), assistance.getUpdatedAt());
    }

    public void replaceAssistanceRequest(UUID id, Assistance assistance) {
        execute("UPDATE assistance_requests SET user_id=?, vehicle_id=?, mechanic_id=?, assistance_type=?, description=?, status=?, address=?, city=?, state=?, requested_at=?, accepted_at=?, completed_at=?, estimated_cost=?, final_cost=?, created_at=?, updated_at=? WHERE id=?",
                assistance.getUserId(), assistance.getVehicleId(), assistance.getMechanicId(), assistance.getAssistanceType(), assistance.getDescription(), assistance.getStatus(), assistance.getAddress(), assistance.getCity(), assistance.getState(), assistance.getRequestedAt(), assistance.getAcceptedAt(), assistance.getCompletedAt(), assistance.getEstimatedCost(), assistance.getFinalCost(), assistance.getCreatedAt(), assistance.getUpdatedAt(), id);
    }

    public void deleteAssistanceRequest(UUID id) {
        execute("DELETE FROM assistance_requests WHERE id=?", id);
    }

    // the requests of one user, newest first
    public List<Assistance> getAssistanceRequestsByUser(UUID userId) {
        return query("SELECT * FROM assistance_requests WHERE user_id=? ORDER BY created_at DESC", this::map, userId);
    }

    // Atomic: gives a still-waiting request its mechanic. Returns false if another call already did it.
    public boolean claimAssistanceRequest(UUID id, UUID mechanicId, LocalDateTime now) {
        return execute("UPDATE assistance_requests SET mechanic_id=?, status='ON_THE_WAY', accepted_at=?, updated_at=? "
                + "WHERE id=? AND status='REQUESTED'", mechanicId, now, now, id) == 1;
    }

    // jobs a mechanic is still working on (other than one request), used before freeing them
    public List<Assistance> getActiveForMechanic(UUID mechanicId, UUID excludedRequestId) {
        return query("SELECT * FROM assistance_requests WHERE mechanic_id=? AND id<>? AND status IN ('ON_THE_WAY','IN_PROGRESS')",
                this::map, mechanicId, excludedRequestId);
    }
}
