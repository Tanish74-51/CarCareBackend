package CarCareBackend.src.main.java.carcare.service;

import CarCareBackend.src.main.java.carcare.model.Vehicle;
import CarCareBackend.src.main.java.carcare.repository.UserRepository;
import CarCareBackend.src.main.java.carcare.repository.VehicleRepository;

import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class VehicleService extends BaseService {

    private final VehicleRepository vehicleRepository = new VehicleRepository();
    private final UserRepository userRepository = new UserRepository();

    private static final Pattern REGISTRATION_PATTERN = Pattern.compile(
            "([A-Z]{2}\\d{1,2}[A-Z]{1,3}\\d{1,4}|\\d{2}BH\\d{4}[A-Z]{1,2})"
    );

    private static final Set<String> FUEL_TYPES = Set.of(
            "PETROL", "DIESEL", "CNG", "LPG", "ELECTRIC", "HYBRID", "HYDROGEN"
    );

    private static final Set<String> VEHICLE_TYPES = Set.of(
            "CAR", "MOTORCYCLE", "SCOOTER", "TRUCK", "BUS", "SUV", "VAN"
    );

    // the owner can never be changed
    private static final Set<String> EDITABLE_FIELDS = Set.of(
            "registrationNumber", "manufacturer", "model", "manufacturingYear", "fuelType", "vehicleType"
    );

    public void validateVehicle(Vehicle vehicle) {
        require(vehicle.getOwnerId(), "Owner ID is required");

        if (userRepository.getUser(vehicle.getOwnerId()) == null) {
            throw new IllegalArgumentException("Owner does not exist");
        }

        String registration = requireString(vehicle.getRegistrationNumber(), "Registration number")
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        if (!REGISTRATION_PATTERN.matcher(registration).matches()) {
            throw new IllegalArgumentException("Invalid registration number");
        }
        vehicle.setRegistrationNumber(registration);

        Vehicle sameRegistration = vehicleRepository.getVehicleByRegistration(registration);
        if (sameRegistration != null && !sameRegistration.getId().equals(vehicle.getId())) {
            throw new IllegalStateException("A vehicle with this registration number already exists");
        }

        vehicle.setManufacturer(requireString(vehicle.getManufacturer(), "Manufacturer"));
        vehicle.setModel(requireString(vehicle.getModel(), "Model"));

        int year = vehicle.getManufacturingYear();
        if (year < 1886 || year > Year.now().getValue()) {
            throw new IllegalArgumentException("Invalid manufacturing year");
        }

        String fuelType = requireString(vehicle.getFuelType(), "Fuel type").toUpperCase(Locale.ROOT);
        if (!FUEL_TYPES.contains(fuelType)) {
            throw new IllegalArgumentException("Invalid fuel type");
        }
        vehicle.setFuelType(fuelType);

        String vehicleType = requireString(vehicle.getVehicleType(), "Vehicle type").toUpperCase(Locale.ROOT);
        if (!VEHICLE_TYPES.contains(vehicleType)) {
            throw new IllegalArgumentException("Invalid vehicle type");
        }
        vehicle.setVehicleType(vehicleType);
    }

    public List<Vehicle> getUserVehicles(UUID userId) {
        return vehicleRepository.getUserVehicles(userId);
    }

    // a vehicle is only visible to its owner
    public Vehicle getVehicle(UUID id, UUID userId) {
        Vehicle vehicle = found(vehicleRepository.getVehicle(id), "Vehicle not found");
        requireOwner(vehicle.getOwnerId(), userId);
        return vehicle;
    }

    // the owner is always the logged-in user, whatever the body says
    public Vehicle createVehicle(Vehicle vehicle, UUID userId) {
        vehicle.setId(UUID.randomUUID());
        vehicle.setOwnerId(userId);
        validateVehicle(vehicle);
        vehicleRepository.createVehicle(vehicle);
        return vehicle;
    }

    public Vehicle replaceVehicle(UUID id, Vehicle vehicle, UUID userId) {
        getVehicle(id, userId);

        vehicle.setId(id);
        vehicle.setOwnerId(userId);
        validateVehicle(vehicle);

        vehicleRepository.replaceVehicle(id, vehicle);
        return vehicle;
    }

    public Vehicle updateVehicle(UUID id, String body, UUID userId) {
        Vehicle updated = patch(getVehicle(id, userId), body, EDITABLE_FIELDS);

        validateVehicle(updated);

        vehicleRepository.replaceVehicle(id, updated);
        return updated;
    }

    public void deleteVehicle(UUID id, UUID userId) {
        getVehicle(id, userId);
        vehicleRepository.deleteVehicle(id);
    }
}

