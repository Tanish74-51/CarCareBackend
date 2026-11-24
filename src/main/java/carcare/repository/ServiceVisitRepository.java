package CarCareBackend.src.main.java.carcare.repository;

import CarCareBackend.src.main.java.carcare.model.ServiceVisit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ServiceVisitRepository extends BaseRepository {

    private ServiceVisit map(ResultSet rs) throws SQLException {
        ServiceVisit serviceVisit = new ServiceVisit();
        serviceVisit.setId(uuid(rs, "id"));
        serviceVisit.setVehicleId(uuid(rs, "vehicle_id"));
        serviceVisit.setMechanicId(uuid(rs, "mechanic_id"));
        serviceVisit.setServiceCentreId(uuid(rs, "service_centre_id"));
        serviceVisit.setServiceType(rs.getString("service_type"));
        serviceVisit.setDescription(rs.getString("description"));
        serviceVisit.setStatus(rs.getString("status"));
        serviceVisit.setScheduledAt(time(rs, "scheduled_at"));
        serviceVisit.setCompletedAt(time(rs, "completed_at"));
        serviceVisit.setCost(rs.getDouble("cost"));
        serviceVisit.setNotes(rs.getString("notes"));
        serviceVisit.setCreatedAt(time(rs, "created_at"));
        serviceVisit.setUpdatedAt(time(rs, "updated_at"));
        return serviceVisit;
    }

    public List<ServiceVisit> getAllServiceVisits() {
        return query("SELECT * FROM service_visits", this::map);
    }

    // every booking made for the vehicles of one user, newest first
    public List<ServiceVisit> getServiceVisitsByUser(UUID userId) {
        return query("SELECT sv.* FROM service_visits sv JOIN vehicles v ON sv.vehicle_id=v.id "
                + "WHERE v.owner_id=? ORDER BY sv.created_at DESC", this::map, userId);
    }

    public ServiceVisit getServiceVisit(UUID id) {
        return queryOne("SELECT * FROM service_visits WHERE id=?", this::map, id);
    }

    public void createServiceVisit(ServiceVisit serviceVisit) {
        execute("INSERT INTO service_visits (id, vehicle_id, mechanic_id, service_centre_id, service_type, description, status, scheduled_at, completed_at, cost, notes, created_at, updated_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                serviceVisit.getId(), serviceVisit.getVehicleId(), serviceVisit.getMechanicId(), serviceVisit.getServiceCentreId(), serviceVisit.getServiceType(), serviceVisit.getDescription(), serviceVisit.getStatus(), serviceVisit.getScheduledAt(), serviceVisit.getCompletedAt(), serviceVisit.getCost(), serviceVisit.getNotes(), serviceVisit.getCreatedAt(), serviceVisit.getUpdatedAt());
    }

    public void replaceServiceVisit(UUID id, ServiceVisit serviceVisit) {
        execute("UPDATE service_visits SET vehicle_id=?, mechanic_id=?, service_centre_id=?, service_type=?, description=?, status=?, scheduled_at=?, completed_at=?, cost=?, notes=?, created_at=?, updated_at=? WHERE id=?",
                serviceVisit.getVehicleId(), serviceVisit.getMechanicId(), serviceVisit.getServiceCentreId(), serviceVisit.getServiceType(), serviceVisit.getDescription(), serviceVisit.getStatus(), serviceVisit.getScheduledAt(), serviceVisit.getCompletedAt(), serviceVisit.getCost(), serviceVisit.getNotes(), serviceVisit.getCreatedAt(), serviceVisit.getUpdatedAt(), id);
    }

    public void deleteServiceVisit(UUID id) {
        execute("DELETE FROM service_visits WHERE id=?", id);
    }
}
