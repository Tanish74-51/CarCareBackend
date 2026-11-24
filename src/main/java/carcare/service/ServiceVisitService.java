package CarCareBackend.src.main.java.carcare.service;

import CarCareBackend.src.main.java.carcare.config.Timers;
import CarCareBackend.src.main.java.carcare.model.Mechanic;
import CarCareBackend.src.main.java.carcare.model.ServiceCentre;
import CarCareBackend.src.main.java.carcare.model.ServiceVisit;
import CarCareBackend.src.main.java.carcare.model.Vehicle;
import CarCareBackend.src.main.java.carcare.repository.MechanicRepository;
import CarCareBackend.src.main.java.carcare.repository.ServiceCentreRepository;
import CarCareBackend.src.main.java.carcare.repository.ServiceVisitRepository;
import CarCareBackend.src.main.java.carcare.repository.VehicleRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ServiceVisitService extends BaseService {

    private final ServiceVisitRepository serviceVisitRepository = new ServiceVisitRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();
    private final ServiceCentreRepository serviceCentreRepository = new ServiceCentreRepository();
    private final MechanicRepository mechanicRepository = new MechanicRepository();

    private static final Set<String> STATUSES = Set.of(
            "SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
    );

    // the only allowed moves for an admin (a customer can only cancel a booking that has not started)
    private static final Map<String, Set<String>> NEXT_STATUS = Map.of(
            "SCHEDULED", Set.of("IN_PROGRESS", "CANCELLED"),
            "IN_PROGRESS", Set.of("COMPLETED", "CANCELLED")
    );

    // what the CUSTOMER may edit; "status" is only accepted as CANCELLED, and the cost is not theirs to set
    private static final Set<String> USER_FIELDS = Set.of(
            "serviceCentreId", "serviceType", "description", "status", "scheduledAt", "notes"
    );

    // what an ADMIN may change: move the job forward and set what it cost
    private static final Set<String> ADMIN_FIELDS = Set.of("status", "cost", "notes");

    // what the client sees for "which mechanic got my booking"
    public record AssignedMechanic(UUID id, String name, String phoneNumber) {}

    // what the client sees: the booking, the service centre it was booked at and the mechanic that was picked
    public record VisitDetails(ServiceVisit visit, ServiceCentre serviceCentre, AssignedMechanic mechanic) {}

    private VisitDetails details(ServiceVisit visit) {
        ServiceCentre centre = serviceCentreRepository.getServiceCentre(visit.getServiceCentreId());

        AssignedMechanic assigned = null;
        if (visit.getMechanicId() != null) {
            Mechanic mechanic = mechanicRepository.getMechanic(visit.getMechanicId());
            if (mechanic != null) {
                assigned = new AssignedMechanic(mechanic.getId(), mechanic.getName(), mechanic.getPhoneNumber());
            }
        }
        return new VisitDetails(visit, centre, assigned);
    }

    public void validateServiceVisit(ServiceVisit visit) {
        require(visit.getVehicleId(), "Vehicle ID is required");
        require(visit.getServiceCentreId(), "Service centre ID is required");

        if (vehicleRepository.getVehicle(visit.getVehicleId()) == null) {
            throw new IllegalArgumentException("Vehicle does not exist");
        }

        if (serviceCentreRepository.getServiceCentre(visit.getServiceCentreId()) == null) {
            throw new IllegalArgumentException("Service centre does not exist");
        }

        if (visit.getMechanicId() != null &&
                mechanicRepository.getMechanic(visit.getMechanicId()) == null) {
            throw new IllegalArgumentException("Mechanic does not exist");
        }

        visit.setServiceType(requireString(visit.getServiceType(), "Service type"));
        visit.setDescription(optionalText(visit.getDescription(), "Description"));
        visit.setNotes(optionalText(visit.getNotes(), "Notes"));

        String status = requireString(visit.getStatus(), "Status").toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid service visit status");
        }
        visit.setStatus(status);

        require(visit.getScheduledAt(), "Scheduled date is required");
        requireNonNegative(visit.getCost(), "Cost");
    }

    // the chosen time must not be in the past and must fall inside the centre's opening hours
    private void checkSchedule(ServiceVisit visit, ServiceCentre centre) {
        LocalDateTime at = visit.getScheduledAt();
        if (at.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("That date and time is in the past");
        }

        LocalTime open;
        LocalTime close;
        try {
            open = LocalTime.parse(centre.getOpeningTime());
            close = LocalTime.parse(centre.getClosingTime());
        } catch (DateTimeParseException | NullPointerException e) {
            return;       // the centre has no usable opening hours saved: nothing to check against
        }

        LocalTime time = at.toLocalTime();
        boolean isOpen = open.isBefore(close)
                ? !time.isBefore(open) && time.isBefore(close)
                : !time.isBefore(open) || time.isBefore(close);          // a centre open past midnight
        if (!isOpen) {
            throw new IllegalArgumentException(centre.getName() + " is open " + open + "-" + close
                    + ". Pick a time within those hours.");
        }
    }

    // the vehicle must exist and belong to the logged-in user
    private void requireOwnVehicle(UUID vehicleId, UUID userId) {
        Vehicle vehicle = vehicleRepository.getVehicle(require(vehicleId, "Vehicle ID is required"));
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle does not exist");
        }
        requireOwner(vehicle.getOwnerId(), userId);
    }

    // a booking is only visible to the owner of the vehicle it was made for
    private ServiceVisit owned(UUID id, UUID userId) {
        ServiceVisit visit = found(serviceVisitRepository.getServiceVisit(id), "Service visit not found");
        requireOwnVehicle(visit.getVehicleId(), userId);
        return visit;
    }

    // The server picks the mechanic: any free mechanic who works at the chosen service centre. The mechanic is
    // held for MECHANIC_BUSY_SECONDS (30) so the same person is not handed to two bookings (or a roadside job) at once.
    private Mechanic assignMechanic(ServiceVisit visit) {
        UUID centreId = require(visit.getServiceCentreId(), "Service centre ID is required");

        ServiceCentre centre = serviceCentreRepository.getServiceCentre(centreId);
        if (centre == null) {
            throw new IllegalArgumentException("Service centre does not exist");
        }
        if (!centre.isAcceptingVehicles()) {
            throw new IllegalStateException(centre.getName() + " is not accepting vehicles right now");
        }

        List<Mechanic> free = mechanicRepository.getFreeMechanicsAtCentre(centreId);
        Collections.shuffle(free, random);

        LocalDateTime busyUntil = LocalDateTime.now().plusSeconds(Timers.MECHANIC_BUSY_SECONDS);
        for (Mechanic mechanic : free) {
            if (mechanicRepository.markBusy(mechanic.getId(), busyUntil)) {
                visit.setMechanicId(mechanic.getId());
                return mechanic;
            }
        }

        throw new IllegalStateException(mechanicRepository.countAtCentre(centreId) == 0
                ? centre.getName() + " has no mechanics registered yet"
                : "No mechanic is free at " + centre.getName() + " right now. Try again in a moment.");
    }

    // only the bookings of the logged-in user's vehicles
    public List<VisitDetails> getServiceVisits(UUID userId) {
        List<VisitDetails> result = new ArrayList<>();
        for (ServiceVisit visit : serviceVisitRepository.getServiceVisitsByUser(userId)) {
            result.add(details(visit));
        }
        return result;
    }

    public VisitDetails getServiceVisitDetails(UUID id, UUID userId) {
        return details(owned(id, userId));
    }

    // the client sends vehicleId, serviceCentreId and serviceType; the booking time defaults to now
    // and the mechanic is picked by the server
    public VisitDetails createServiceVisit(ServiceVisit visit, UUID userId) {
        LocalDateTime now = LocalDateTime.now();

        visit.setId(UUID.randomUUID());
        visit.setStatus("SCHEDULED");
        visit.setMechanicId(null);
        visit.setCompletedAt(null);
        visit.setCost(0);
        visit.setCreatedAt(now);
        visit.setUpdatedAt(now);
        if (visit.getScheduledAt() == null) {
            visit.setScheduledAt(now);
        }

        requireOwnVehicle(visit.getVehicleId(), userId);
        validateServiceVisit(visit);                       // everything that can be wrong is checked BEFORE a mechanic is held
        checkSchedule(visit, serviceCentreRepository.getServiceCentre(visit.getServiceCentreId()));

        Mechanic mechanic = assignMechanic(visit);
        try {
            serviceVisitRepository.createServiceVisit(visit);
        } catch (RuntimeException e) {
            mechanicRepository.markFree(mechanic.getId());
            throw e;
        }
        return details(visit);
    }

    public VisitDetails replaceServiceVisit(UUID id, ServiceVisit visit, UUID userId) {
        ServiceVisit existing = owned(id, userId);
        checkNotClosed(existing);

        UUID oldCentre = existing.getServiceCentreId();
        LocalDateTime oldTime = existing.getScheduledAt();

        // only the editable details are replaced; the vehicle, status, cost and timestamps stay as they were
        existing.setServiceCentreId(visit.getServiceCentreId());
        existing.setServiceType(visit.getServiceType());
        existing.setDescription(visit.getDescription());
        existing.setScheduledAt(visit.getScheduledAt());
        existing.setNotes(visit.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());

        validateServiceVisit(existing);
        return saveChanged(id, existing, oldCentre, oldTime);
    }

    // PATCH by the customer
    public VisitDetails updateServiceVisit(UUID id, String body, UUID userId) {
        ServiceVisit existing = owned(id, userId);
        checkNotClosed(existing);

        UUID oldCentre = existing.getServiceCentreId();     // patch() changes "existing" in place
        LocalDateTime oldTime = existing.getScheduledAt();
        String oldStatus = existing.getStatus();
        ServiceVisit updated = patch(existing, body, USER_FIELDS);

        updated.setUpdatedAt(LocalDateTime.now());
        validateServiceVisit(updated);

        if (!updated.getStatus().equals(oldStatus)) {
            if (!updated.getStatus().equals("CANCELLED") || !oldStatus.equals("SCHEDULED")) {
                throw new IllegalStateException("You can only cancel a booking that has not started yet");
            }
        }
        return saveChanged(id, updated, oldCentre, oldTime);
    }

    // re-checks the time / centre only when one of them actually changed, and picks a new mechanic if the centre changed
    private VisitDetails saveChanged(UUID id, ServiceVisit visit, UUID oldCentre, LocalDateTime oldTime) {
        boolean centreChanged = !Objects.equals(oldCentre, visit.getServiceCentreId());
        boolean timeChanged = !Objects.equals(oldTime, visit.getScheduledAt());

        if ((centreChanged || timeChanged) && !visit.getStatus().equals("CANCELLED")) {
            checkSchedule(visit, serviceCentreRepository.getServiceCentre(visit.getServiceCentreId()));
        }
        if (centreChanged && !visit.getStatus().equals("CANCELLED")) {
            assignMechanic(visit);
        }

        serviceVisitRepository.replaceServiceVisit(id, visit);
        return details(visit);
    }

    public void deleteServiceVisit(UUID id, UUID userId) {
        owned(id, userId);
        serviceVisitRepository.deleteServiceVisit(id);
    }

    // ---- admin ----

    public List<VisitDetails> getAllServiceVisits() {
        List<VisitDetails> result = new ArrayList<>();
        for (ServiceVisit visit : serviceVisitRepository.getAllServiceVisits()) {
            result.add(details(visit));
        }
        return result;
    }

    // PATCH by an admin: status (SCHEDULED -> IN_PROGRESS -> COMPLETED), cost and notes
    public VisitDetails adminUpdate(UUID id, String body) {
        ServiceVisit existing = found(serviceVisitRepository.getServiceVisit(id), "Service visit not found");
        checkNotClosed(existing);

        String oldStatus = existing.getStatus();
        ServiceVisit updated = patch(existing, body, ADMIN_FIELDS);

        updated.setUpdatedAt(LocalDateTime.now());
        validateServiceVisit(updated);

        String newStatus = updated.getStatus();
        if (!newStatus.equals(oldStatus) && !NEXT_STATUS.getOrDefault(oldStatus, Set.of()).contains(newStatus)) {
            throw new IllegalStateException("A booking cannot go from " + oldStatus + " to " + newStatus);
        }
        if (updated.getCost() > 0 && !newStatus.equals("IN_PROGRESS") && !newStatus.equals("COMPLETED")) {
            throw new IllegalStateException("The cost can only be set once the work has started");
        }
        if (newStatus.equals("COMPLETED")) {
            updated.setCompletedAt(LocalDateTime.now());
        }

        serviceVisitRepository.replaceServiceVisit(id, updated);
        return details(updated);
    }

    private void checkNotClosed(ServiceVisit visit) {
        if (visit.getStatus().equals("COMPLETED") || visit.getStatus().equals("CANCELLED")) {
            throw new IllegalStateException("This service visit is " + visit.getStatus() + " and cannot be changed");
        }
    }
}