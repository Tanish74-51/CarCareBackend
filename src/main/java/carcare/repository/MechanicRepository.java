package CarCareBackend.src.main.java.carcare.repository;

import CarCareBackend.src.main.java.carcare.model.Mechanic;

import java.sql.ResultSet;



import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MechanicRepository extends BaseRepository {

    // "free" = switched on by the mechanic AND not busy (busy_until is empty or already in the past).
    // It needs one parameter: the current time.
    private static final String FREE = "available=TRUE AND accepting_requests=TRUE AND (busy_until IS NULL OR busy_until<=?)";

    private Mechanic map(ResultSet rs) throws SQLException {
        Mechanic mechanic = new Mechanic();
        mechanic.setId(uuid(rs, "id"));
        mechanic.setName(rs.getString("name"));
        mechanic.setPhoneNumber(rs.getString("phone_number"));
        mechanic.setEmail(rs.getString("email"));
        mechanic.setYearsOfExperience(rs.getInt("years_of_experience"));
        mechanic.setServiceCentreId(uuid(rs, "service_centre_id"));
        mechanic.setAvailable(rs.getBoolean("available"));
        mechanic.setAcceptingRequests(rs.getBoolean("accepting_requests"));
        mechanic.setBusyUntil(time(rs, "busy_until"));
        mechanic.setAddress(rs.getString("address"));
        mechanic.setCity(rs.getString("city"));
        mechanic.setState(rs.getString("state"));
        mechanic.setPostalCode(rs.getString("postal_code"));
        return mechanic;
    }

    public List<Mechanic> getAllMechanics() {
        return query("SELECT * FROM mechanics", this::map);
    }

    public Mechanic getMechanic(UUID id) {
        return queryOne("SELECT * FROM mechanics WHERE id=?", this::map, id);
    }

    public void createMechanic(Mechanic mechanic) {
        execute("INSERT INTO mechanics (id, name, phone_number, email, years_of_experience, service_centre_id, available, accepting_requests, busy_until, address, city, state, postal_code) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                mechanic.getId(), mechanic.getName(), mechanic.getPhoneNumber(), mechanic.getEmail(), mechanic.getYearsOfExperience(), mechanic.getServiceCentreId(), mechanic.isAvailable(), mechanic.isAcceptingRequests(), mechanic.getBusyUntil(), mechanic.getAddress(), mechanic.getCity(), mechanic.getState(), mechanic.getPostalCode());
    }

    public void replaceMechanic(UUID id, Mechanic mechanic) {
        execute("UPDATE mechanics SET name=?, phone_number=?, email=?, years_of_experience=?, service_centre_id=?, available=?, accepting_requests=?, address=?, city=?, state=?, postal_code=? WHERE id=?",
                mechanic.getName(), mechanic.getPhoneNumber(), mechanic.getEmail(), mechanic.getYearsOfExperience(), mechanic.getServiceCentreId(), mechanic.isAvailable(), mechanic.isAcceptingRequests(), mechanic.getAddress(), mechanic.getCity(), mechanic.getState(), mechanic.getPostalCode(), id);
    }

    public void deleteMechanic(UUID id) {
        execute("DELETE FROM mechanics WHERE id=?", id);
    }

    // free mechanics in the same city and state (roadside assistance)
    public List<Mechanic> getFreeMechanics(String city, String state) {
        return query("SELECT * FROM mechanics WHERE LOWER(city)=LOWER(?) AND LOWER(state)=LOWER(?) AND " + FREE,
                this::map, city, state, LocalDateTime.now());
    }

    // free mechanics who work at one service centre (service visits)
    public List<Mechanic> getFreeMechanicsAtCentre(UUID serviceCentreId) {
        return query("SELECT * FROM mechanics WHERE service_centre_id=? AND " + FREE,
                this::map, serviceCentreId, LocalDateTime.now());
    }

    // Tries to mark a mechanic busy until the given time. The "WHERE ... free" part makes it atomic:
    // if two requests pick the same mechanic at once, only one UPDATE changes a row (returns true).
    public boolean markBusy(UUID id, LocalDateTime until) {
        return execute("UPDATE mechanics SET busy_until=? WHERE id=? AND " + FREE, until, id, LocalDateTime.now()) == 1;
    }

    // job finished or cancelled: the mechanic is free straight away, no need to wait for the timeout
    public void markFree(UUID id) {
        execute("UPDATE mechanics SET busy_until=NULL WHERE id=?", id);
    }

    // how many mechanics exist in a city (free or not), to tell "none here" from "all busy"
    public int countInCity(String city, String state) {
        return query("SELECT id FROM mechanics WHERE LOWER(city)=LOWER(?) AND LOWER(state)=LOWER(?)",
                rs -> uuid(rs, "id"), city, state).size();
    }

    public int countAtCentre(UUID serviceCentreId) {
        return query("SELECT id FROM mechanics WHERE service_centre_id=?", rs -> uuid(rs, "id"), serviceCentreId).size();
    }
}
