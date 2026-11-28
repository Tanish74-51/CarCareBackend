package CarCareBackend.src.main.java.carcare.service;

import CarCareBackend.src.main.java.carcare.config.Timers;
import CarCareBackend.src.main.java.carcare.model.Assistance;
import CarCareBackend.src.main.java.carcare.model.Mechanic;
import CarCareBackend.src.main.java.carcare.model.User;
import CarCareBackend.src.main.java.carcare.model.Vehicle;
import CarCareBackend.src.main.java.carcare.repository.AssistanceRepository;
import CarCareBackend.src.main.java.carcare.repository.MechanicRepository;
import CarCareBackend.src.main.java.carcare.repository.UserRepository;
import CarCareBackend.src.main.java.carcare.repository.VehicleRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AssistanceService extends BaseService {

    private final AssistanceRepository assistanceRepository = new AssistanceRepository();
    private final MechanicRepository mechanicRepository = new MechanicRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();
    private final UserRepository userRepository = new UserRepository();

    private static final Set<String> ASSISTANCE_TYPES = Set.of(
            "BREAKDOWN", "FLAT_TIRE", "BATTERY_FAILURE", "FUEL_DELIVERY",
            "ENGINE_FAILURE", "ACCIDENT", "TOWING", "OTHER"
    );

    private static final Set<String> STATUSES = Set.of(
            "REQUESTED", "ON_THE_WAY", "IN_PROGRESS", "COMPLETED", "CANCELLED"
    );

    private static final Map<String, Set<String>> NEXT_STATUS = Map.of(
            "REQUESTED", Set.of("CANCELLED"),
            "ON_THE_WAY", Set.of("IN_PROGRESS", "CANCELLED"),
            "IN_PROGRESS", Set.of("COMPLETED", "CANCELLED")
    );

    private static final Set<String> USER_FIELDS = Set.of("assistanceType", "description", "address");

    private static final Set<String> ADMIN_FIELDS = Set.of("status", "estimatedCost", "finalCost");

    public record AssignedMechanic(UUID id, String name, String phoneNumber) {}


    public record AssistanceDetails(Assistance request, AssignedMechanic mechanic, String message) {}

    private AssistanceDetails details(Assistance assistance) {
        AssignedMechanic assigned = null;
        if (assistance.getMechanicId() != null) {
            Mechanic mechanic = mechanicRepository.getMechanic(assistance.getMechanicId());
            if (mechanic != null) {
                assigned = new AssignedMechanic(mechanic.getId(), mechanic.getName(), mechanic.getPhoneNumber());
            }
        }

        String place = assistance.getCity() + ", " + assistance.getState();
        String message = switch (assistance.getStatus()) {
            case "REQUESTED" -> mechanicRepository.countInCity(assistance.getCity(), assistance.getState()) == 0
                    ? "There are no mechanics registered in " + place + ", so nobody can help yet."
                    : "All mechanics in " + place + " are busy right now. Check again in a moment.";
            case "ON_THE_WAY" -> "Your mechanic is on the way";
            case "IN_PROGRESS" -> "Your mechanic is working on it";
            case "COMPLETED" -> "Assistance completed";
            case "CANCELLED" -> "Request cancelled";
            default -> "";
        };
        return new AssistanceDetails(assistance, assigned, message);
    }

    public void validateAssistance(Assistance assistance) {
        require(assistance.getUserId(), "User ID is required");
        require(assistance.getVehicleId(), "Vehicle ID is required");

        if (userRepository.getUser(assistance.getUserId()) == null) {
            throw new IllegalArgumentException("User does not exist");
        }

        Vehicle vehicle = vehicleRepository.getVehicle(assistance.getVehicleId());
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle does not exist");
        }
        if (!vehicle.getOwnerId().equals(assistance.getUserId())) {
            throw new IllegalArgumentException("Vehicle does not belong to this user");
        }

        String type = requireString(assistance.getAssistanceType(), "Assistance type").toUpperCase(Locale.ROOT);
        if (!ASSISTANCE_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid assistance type");
        }
        assistance.setAssistanceType(type);

        String status = requireString(assistance.getStatus(), "Status").toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid assistance status");
        }
        assistance.setStatus(status);

        assistance.setDescription(requireText(assistance.getDescription(), "Description"));
        assistance.setAddress(requireString(assistance.getAddress(), "Address"));
        assistance.setCity(requireString(assistance.getCity(), "City"));
        assistance.setState(requireString(assistance.getState(), "State"));

        requireNonNegative(assistance.getEstimatedCost(), "Estimated cost");
        requireNonNegative(assistance.getFinalCost(), "Final cost");
    }

    private Assistance owned(UUID id, UUID userId) {
        Assistance assistance = found(assistanceRepository.getAssistanceRequest(id), "Assistance request not found");
        requireOwner(assistance.getUserId(), userId);
        return assistance;
    }

    public List<Assistance> getAssistanceRequests(UUID userId) {
        return assistanceRepository.getAssistanceRequestsByUser(userId);
    }

    public AssistanceDetails getAssistanceDetails(UUID id, UUID userId) {
        Assistance assistance = owned(id, userId);

        if (assistance.getStatus().equals("REQUESTED")) {
            Mechanic mechanic = pickMechanic(assistance.getCity(), assistance.getState());
            if (mechanic != null) {
                if (!assistanceRepository.claimAssistanceRequest(id, mechanic.getId(), LocalDateTime.now())) {
                    mechanicRepository.markFree(mechanic.getId());
                }
                assistance = assistanceRepository.getAssistanceRequest(id);
            }
        }
        return details(assistance);
    }

    public AssistanceDetails createAssistanceRequest(Assistance assistance, UUID userId) {
        LocalDateTime now = LocalDateTime.now();

        assistance.setId(UUID.randomUUID());
        assistance.setUserId(userId);
        assistance.setStatus("REQUESTED");
        assistance.setMechanicId(null);
        assistance.setAcceptedAt(null);
        assistance.setCompletedAt(null);
        assistance.setEstimatedCost(0);
        assistance.setFinalCost(0);
        assistance.setRequestedAt(now);
        assistance.setCreatedAt(now);
        assistance.setUpdatedAt(now);

        User user = userRepository.getUser(userId);
        if (user == null) {
            throw new IllegalArgumentException("User does not exist");
        }
        assistance.setCity(user.getCity());
        assistance.setState(user.getState());

        validateAssistance(assistance);

        Mechanic mechanic = pickMechanic(assistance.getCity(), assistance.getState());
        if (mechanic != null) {
            assistance.setMechanicId(mechanic.getId());
            assistance.setAcceptedAt(now);
            assistance.setStatus("ON_THE_WAY");
        }

        try {
            assistanceRepository.createAssistanceRequest(assistance);
        } catch (RuntimeException e) {
            if (mechanic != null) {
                mechanicRepository.markFree(mechanic.getId());
            }
            throw e;
        }
        return details(assistance);
    }

    private Mechanic pickMechanic(String city, String state) {
        List<Mechanic> free = mechanicRepository.getFreeMechanics(city, state);
        Collections.shuffle(free, random);

        LocalDateTime busyUntil = LocalDateTime.now().plusSeconds(Timers.MECHANIC_BUSY_SECONDS);
        for (Mechanic mechanic : free) {
            if (mechanicRepository.markBusy(mechanic.getId(), busyUntil)) {
                return mechanic;
            }
        }
        return null;
    }

    private void releaseMechanic(Assistance assistance) {
        UUID mechanicId = assistance.getMechanicId();
        if (mechanicId != null && assistanceRepository.getActiveForMechanic(mechanicId, assistance.getId()).isEmpty()) {
            mechanicRepository.markFree(mechanicId);
        }
    }

    public AssistanceDetails replaceAssistanceRequest(UUID id, Assistance assistance, UUID userId) {
        Assistance existing = owned(id, userId);
        checkNotClosed(existing);

        existing.setVehicleId(assistance.getVehicleId());
        existing.setAssistanceType(assistance.getAssistanceType());
        existing.setDescription(assistance.getDescription());
        existing.setAddress(assistance.getAddress());
        existing.setUpdatedAt(LocalDateTime.now());

        validateAssistance(existing);

        assistanceRepository.replaceAssistanceRequest(id, existing);
        return details(existing);
    }

    public AssistanceDetails updateAssistanceRequest(UUID id, String body, UUID userId) {
        Assistance existing = owned(id, userId);
        checkNotClosed(existing);

        Assistance updated = patch(existing, body, USER_FIELDS);
        updated.setUpdatedAt(LocalDateTime.now());

        validateAssistance(updated);

        assistanceRepository.replaceAssistanceRequest(id, updated);
        return details(updated);
    }

    public AssistanceDetails cancelAssistanceRequest(UUID id, UUID userId) {
        Assistance existing = owned(id, userId);
        checkNotClosed(existing);

        existing.setStatus("CANCELLED");
        existing.setUpdatedAt(LocalDateTime.now());

        assistanceRepository.replaceAssistanceRequest(id, existing);
        releaseMechanic(existing);
        return details(existing);
    }

    public AssistanceDetails completeAssistanceRequest(UUID id, UUID userId) {
        Assistance existing = owned(id, userId);
        checkNotClosed(existing);

        String status = existing.getStatus();
        if (!status.equals("ON_THE_WAY") && !status.equals("IN_PROGRESS")) {
            throw new IllegalStateException("No mechanic has been assigned yet, so this request cannot be completed. Cancel it instead.");
        }

        LocalDateTime now = LocalDateTime.now();
        existing.setStatus("COMPLETED");
        existing.setCompletedAt(now);
        existing.setUpdatedAt(now);

        assistanceRepository.replaceAssistanceRequest(id, existing);
        releaseMechanic(existing);
        return details(existing);
    }



    public List<Assistance> getAllRequests() {
        return assistanceRepository.getAllAssistanceRequests();
    }

    // PATCH by an admin: moves the job forward (ON_THE_WAY -> IN_PROGRESS -> COMPLETED) and sets the costs
    public AssistanceDetails adminUpdate(UUID id, String body) {
        Assistance existing = found(assistanceRepository.getAssistanceRequest(id), "Assistance request not found");
        checkNotClosed(existing);

        String oldStatus = existing.getStatus();       // patch() changes "existing" in place, so remember it first
        Assistance updated = patch(existing, body, ADMIN_FIELDS);

        validateAssistance(updated);

        String newStatus = updated.getStatus();
        if (!newStatus.equals(oldStatus) && !NEXT_STATUS.getOrDefault(oldStatus, Set.of()).contains(newStatus)) {
            throw new IllegalStateException("A request cannot go from " + oldStatus + " to " + newStatus);
        }
        if (updated.getFinalCost() > 0 && !newStatus.equals("IN_PROGRESS") && !newStatus.equals("COMPLETED")) {
            throw new IllegalStateException("The final cost can only be set once the work has started");
        }

        if (newStatus.equals("COMPLETED")) {
            updated.setCompletedAt(LocalDateTime.now());
        }
        updated.setUpdatedAt(LocalDateTime.now());

        assistanceRepository.replaceAssistanceRequest(id, updated);

        if (newStatus.equals("COMPLETED") || newStatus.equals("CANCELLED")) {
            releaseMechanic(updated);
        }
        return details(updated);
    }

    // shows which mechanic accepted the request
    public AssignedMechanic getMechanic(UUID id, UUID userId) {
        Assistance assistance = owned(id, userId);

        if (assistance.getMechanicId() == null) {
            throw new IllegalStateException("No mechanic has accepted this request yet");
        }

        Mechanic mechanic = found(mechanicRepository.getMechanic(assistance.getMechanicId()), "Mechanic not found");

        return new AssignedMechanic(mechanic.getId(), mechanic.getName(), mechanic.getPhoneNumber());
    }

    private void checkNotClosed(Assistance assistance) {
        if (assistance.getStatus().equals("COMPLETED") || assistance.getStatus().equals("CANCELLED")) {
            throw new IllegalStateException("This request is " + assistance.getStatus() + " and cannot be changed");
        }
    }
}
