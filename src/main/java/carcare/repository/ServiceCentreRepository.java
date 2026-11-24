package CarCareBackend.src.main.java.carcare.repository;

import CarCareBackend.src.main.java.carcare.model.ServiceCentre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ServiceCentreRepository extends BaseRepository {

    private ServiceCentre map(ResultSet rs) throws SQLException {
        ServiceCentre serviceCentre = new ServiceCentre();
        serviceCentre.setId(uuid(rs, "id"));
        serviceCentre.setName(rs.getString("name"));
        serviceCentre.setPhoneNumber(rs.getString("phone_number"));
        serviceCentre.setEmail(rs.getString("email"));
        serviceCentre.setAddress(rs.getString("address"));
        serviceCentre.setCity(rs.getString("city"));
        serviceCentre.setState(rs.getString("state"));
        serviceCentre.setPostalCode(rs.getString("postal_code"));
        serviceCentre.setServices(list(rs, "services"));
        serviceCentre.setVehicleTypes(list(rs, "vehicle_types"));
        serviceCentre.setEmergencyService(rs.getBoolean("emergency_service"));
        serviceCentre.setOpeningTime(rs.getString("opening_time"));
        serviceCentre.setClosingTime(rs.getString("closing_time"));
        serviceCentre.setAcceptingVehicles(rs.getBoolean("accepting_vehicles"));
        serviceCentre.setVerified(rs.getBoolean("verified"));
        serviceCentre.setCreatedAt(time(rs, "created_at"));
        serviceCentre.setUpdatedAt(time(rs, "updated_at"));
        return serviceCentre;
    }

    public List<ServiceCentre> getAllServiceCentres() {
        return query("SELECT * FROM service_centres", this::map);
    }

    public ServiceCentre getServiceCentre(UUID id) {
        return queryOne("SELECT * FROM service_centres WHERE id=?", this::map, id);
    }

    public void createServiceCentre(ServiceCentre serviceCentre) {
        execute("INSERT INTO service_centres (id, name, phone_number, email, address, city, state, postal_code, services, vehicle_types, emergency_service, opening_time, closing_time, accepting_vehicles, verified, created_at, updated_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                serviceCentre.getId(), serviceCentre.getName(), serviceCentre.getPhoneNumber(), serviceCentre.getEmail(), serviceCentre.getAddress(), serviceCentre.getCity(), serviceCentre.getState(), serviceCentre.getPostalCode(), serviceCentre.getServices(), serviceCentre.getVehicleTypes(), serviceCentre.isEmergencyService(), serviceCentre.getOpeningTime(), serviceCentre.getClosingTime(), serviceCentre.isAcceptingVehicles(), serviceCentre.isVerified(), serviceCentre.getCreatedAt(), serviceCentre.getUpdatedAt());
    }

    public void replaceServiceCentre(UUID id, ServiceCentre serviceCentre) {
        execute("UPDATE service_centres SET name=?, phone_number=?, email=?, address=?, city=?, state=?, postal_code=?, services=?, vehicle_types=?, emergency_service=?, opening_time=?, closing_time=?, accepting_vehicles=?, verified=?, created_at=?, updated_at=? WHERE id=?",
                serviceCentre.getName(), serviceCentre.getPhoneNumber(), serviceCentre.getEmail(), serviceCentre.getAddress(), serviceCentre.getCity(), serviceCentre.getState(), serviceCentre.getPostalCode(), serviceCentre.getServices(), serviceCentre.getVehicleTypes(), serviceCentre.isEmergencyService(), serviceCentre.getOpeningTime(), serviceCentre.getClosingTime(), serviceCentre.isAcceptingVehicles(), serviceCentre.isVerified(), serviceCentre.getCreatedAt(), serviceCentre.getUpdatedAt(), id);
    }

    public void deleteServiceCentre(UUID id) {
        execute("DELETE FROM service_centres WHERE id=?", id);
    }

    // fallback when the city has no centres: every centre in the state
    public List<ServiceCentre> getServiceCentresByState(String state) {
        return query("SELECT * FROM service_centres WHERE LOWER(state)=LOWER(?)", this::map, state);
    }

    // step 1 of booking a service: every centre in the user's city and state
    public List<ServiceCentre> getServiceCentresByCityAndState(String city, String state) {
        return query("SELECT * FROM service_centres WHERE LOWER(city)=LOWER(?) AND LOWER(state)=LOWER(?)",
                this::map, city, state);
    }

    // the same name at the same address is the same centre
    public ServiceCentre findSame(String name, String address, String city, String state) {
        return queryOne("SELECT * FROM service_centres WHERE LOWER(name)=LOWER(?) AND LOWER(address)=LOWER(?) "
                + "AND LOWER(city)=LOWER(?) AND LOWER(state)=LOWER(?)", this::map, name, address, city, state);
    }
}

