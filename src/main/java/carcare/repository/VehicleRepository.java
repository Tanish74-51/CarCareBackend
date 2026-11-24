package CarCareBackend.src.main.java.carcare.repository;

import CarCareBackend.src.main.java.carcare.model.Vehicle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class VehicleRepository extends BaseRepository {

    private Vehicle map(ResultSet rs) throws SQLException {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(uuid(rs, "id"));
        vehicle.setOwnerId(uuid(rs, "owner_id"));
        vehicle.setRegistrationNumber(rs.getString("registration_number"));
        vehicle.setManufacturer(rs.getString("manufacturer"));
        vehicle.setModel(rs.getString("model"));
        vehicle.setManufacturingYear(rs.getInt("manufacturing_year"));
        vehicle.setFuelType(rs.getString("fuel_type"));
        vehicle.setVehicleType(rs.getString("vehicle_type"));
        return vehicle;
    }

    public List<Vehicle> getAllVehicles() {
        return query("SELECT * FROM vehicles", this::map);
    }

    public Vehicle getVehicle(UUID id) {
        return queryOne("SELECT * FROM vehicles WHERE id=?", this::map, id);
    }

    public void createVehicle(Vehicle vehicle) {
        execute("INSERT INTO vehicles (id, owner_id, registration_number, manufacturer, model, manufacturing_year, fuel_type, vehicle_type) "
                        + "VALUES (?,?,?,?,?,?,?,?)",
                vehicle.getId(), vehicle.getOwnerId(), vehicle.getRegistrationNumber(), vehicle.getManufacturer(), vehicle.getModel(), vehicle.getManufacturingYear(), vehicle.getFuelType(), vehicle.getVehicleType());
    }

    public void replaceVehicle(UUID id, Vehicle vehicle) {
        execute("UPDATE vehicles SET owner_id=?, registration_number=?, manufacturer=?, model=?, manufacturing_year=?, fuel_type=?, vehicle_type=? WHERE id=?",
                vehicle.getOwnerId(), vehicle.getRegistrationNumber(), vehicle.getManufacturer(), vehicle.getModel(), vehicle.getManufacturingYear(), vehicle.getFuelType(), vehicle.getVehicleType(), id);
    }

    public void deleteVehicle(UUID id) {
        execute("DELETE FROM vehicles WHERE id=?", id);
    }

    public List<Vehicle> getUserVehicles(UUID ownerId) {
        return query("SELECT * FROM vehicles WHERE owner_id=?", this::map, ownerId);
    }

    public Vehicle getVehicleByRegistration(String registrationNumber) {
        return queryOne("SELECT * FROM vehicles WHERE registration_number=?", this::map, registrationNumber);
    }
}
