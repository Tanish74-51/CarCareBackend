package CarCareBackend.src.main.java.carcare.service;

import CarCareBackend.src.main.java.carcare.model.ServiceCentre;
import CarCareBackend.src.main.java.carcare.repository.ServiceCentreRepository;
import CarCareBackend.src.main.java.carcare.model.User;
import CarCareBackend.src.main.java.carcare.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;




import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ServiceCentreService extends BaseService {

    private final ServiceCentreRepository serviceCentreRepository = new ServiceCentreRepository();
    private final UserRepository userRepository = new UserRepository();

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private static final Set<String> EDITABLE_FIELDS = Set.of(
            "name", "phoneNumber", "email", "address", "city", "state", "postalCode",
            "services", "vehicleTypes", "emergencyService", "openingTime", "closingTime",
            "acceptingVehicles", "verified"
    );

    public void validateServiceCentre(ServiceCentre centre) {
        centre.setName(requireString(centre.getName(), "Name"));
        centre.setPhoneNumber(requirePhone(centre.getPhoneNumber()));
        centre.setEmail(requireEmail(centre.getEmail()));

        centre.setAddress(requireString(centre.getAddress(), "Address"));
        centre.setCity(requireString(centre.getCity(), "City"));
        centre.setState(requireString(centre.getState(), "State"));
        centre.setPostalCode(requireString(centre.getPostalCode(), "Postal code"));

        if (centre.getServices() == null || centre.getServices().isEmpty()) {
            throw new IllegalArgumentException("At least one service is required");
        }

        if (centre.getVehicleTypes() == null || centre.getVehicleTypes().isEmpty()) {
            throw new IllegalArgumentException("At least one vehicle type is required");
        }

        List<String> services = new ArrayList<>();
        for (String service : centre.getServices()) {
            services.add(requireString(service, "Service"));
        }
        centre.setServices(services);

        List<String> vehicleTypes = new ArrayList<>();
        for (String type : centre.getVehicleTypes()) {
            vehicleTypes.add(requireString(type, "Vehicle type"));
        }
        centre.setVehicleTypes(vehicleTypes);

        centre.setOpeningTime(parseTime(centre.getOpeningTime(), "Opening time"));
        centre.setClosingTime(parseTime(centre.getClosingTime(), "Closing time"));
    }

    private String parseTime(String value, String fieldName) {
        String trimmed = requireString(value, fieldName);
        try {
            return LocalTime.parse(trimmed, HH_MM).format(HH_MM);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must use HH:mm format");
        }
    }

    public List<ServiceCentre> getAllServiceCentres() {
        return serviceCentreRepository.getAllServiceCentres();
    }

    public ServiceCentre getServiceCentre(UUID id) {
        return found(serviceCentreRepository.getServiceCentre(id), "Service centre not found");
    }

    public ServiceCentre createServiceCentre(ServiceCentre centre) {
        LocalDateTime now = LocalDateTime.now();

        centre.setId(UUID.randomUUID());
        centre.setCreatedAt(now);
        centre.setUpdatedAt(now);

        validateServiceCentre(centre);
        checkNotDuplicate(centre);

        serviceCentreRepository.createServiceCentre(centre);
        return centre;
    }

    public ServiceCentre replaceServiceCentre(UUID id, ServiceCentre centre) {
        ServiceCentre existing = getServiceCentre(id);

        centre.setId(id);
        centre.setCreatedAt(existing.getCreatedAt());
        centre.setUpdatedAt(LocalDateTime.now());

        validateServiceCentre(centre);
        checkNotDuplicate(centre);

        serviceCentreRepository.replaceServiceCentre(id, centre);
        return centre;
    }

    public ServiceCentre updateServiceCentre(UUID id, String body) {
        ServiceCentre updated = patch(getServiceCentre(id), body, EDITABLE_FIELDS);

        updated.setUpdatedAt(LocalDateTime.now());

        validateServiceCentre(updated);
        checkNotDuplicate(updated);

        serviceCentreRepository.replaceServiceCentre(id, updated);
        return updated;
    }

    public void deleteServiceCentre(UUID id) {
        getServiceCentre(id);
        serviceCentreRepository.deleteServiceCentre(id);
    }

    // what the client sees when it looks for centres: where it looked, what it found and a ready-to-show message
    public record NearbyCentres(String city, String state, List<ServiceCentre> serviceCentres, String message) {}

    // Step 1 of booking a service: the centres in ONE city. No fallback to "somewhere else in the state":
    // if the city has none, the answer says so.
    // city / state are optional: a missing city means the logged-in user's own city, a missing state their own state.
    public NearbyCentres findNearby(UUID userId, String city, String state) {
        User user = found(userRepository.getUser(userId), "User not found");

        String cleanCity = requireString(city == null || city.isBlank() ? user.getCity() : city, "City");
        String cleanState = requireString(state == null || state.isBlank() ? user.getState() : state, "State");

        List<ServiceCentre> centres = serviceCentreRepository.getServiceCentresByCityAndState(cleanCity, cleanState);
        String message = centres.isEmpty()
                ? "There are no service centres in " + cleanCity + ", " + cleanState + "."
                : "Found " + centres.size() + " service centre" + (centres.size() == 1 ? "" : "s") + " in " + cleanCity + ", " + cleanState + ".";
        return new NearbyCentres(cleanCity, cleanState, centres, message);
    }

    // the same name at the same address in the same city is the same centre
    private void checkNotDuplicate(ServiceCentre centre) {
        ServiceCentre same = serviceCentreRepository.findSame(centre.getName(), centre.getAddress(), centre.getCity(), centre.getState());
        if (same != null && !same.getId().equals(centre.getId())) {
            throw new IllegalStateException("This service centre already exists");
        }
    }
}