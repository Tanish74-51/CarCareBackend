package CarCareBackend.src.main.java.carcare.service;

import CarCareBackend.src.main.java.carcare.model.Mechanic;
import CarCareBackend.src.main.java.carcare.repository.MechanicRepository;
import CarCareBackend.src.main.java.carcare.repository.ServiceCentreRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MechanicService extends BaseService {

    private final MechanicRepository mechanicRepository = new MechanicRepository();
    private final ServiceCentreRepository serviceCentreRepository = new ServiceCentreRepository();

    private static final Set<String> EDITABLE_FIELDS = Set.of(
            "name", "phoneNumber", "email", "yearsOfExperience", "serviceCentreId",
            "available", "acceptingRequests", "address", "city", "state", "postalCode"
    );

    public void validateMechanic(Mechanic mechanic) {
        mechanic.setName(requireString(mechanic.getName(), "Name"));
        mechanic.setPhoneNumber(requirePhone(mechanic.getPhoneNumber()));
        mechanic.setEmail(requireEmail(mechanic.getEmail()));

        if (mechanic.getYearsOfExperience() < 0 || mechanic.getYearsOfExperience() > 80) {
            throw new IllegalArgumentException("Years of experience must be between 0 and 80");
        }

        mechanic.setAddress(requireString(mechanic.getAddress(), "Address"));
        mechanic.setCity(requireString(mechanic.getCity(), "City"));
        mechanic.setState(requireString(mechanic.getState(), "State"));
        mechanic.setPostalCode(requireString(mechanic.getPostalCode(), "Postal code"));

        if (mechanic.getServiceCentreId() != null &&
                serviceCentreRepository.getServiceCentre(mechanic.getServiceCentreId()) == null) {
            throw new IllegalArgumentException("Service centre does not exist");
        }
    }

    public List<Mechanic> getAllMechanics() {
        return mechanicRepository.getAllMechanics();
    }

    public Mechanic getMechanic(UUID id) {
        return found(mechanicRepository.getMechanic(id), "Mechanic not found");
    }

    public Mechanic createMechanic(Mechanic mechanic) {
        mechanic.setId(UUID.randomUUID());
        mechanic.setBusyUntil(null);          // only the server decides when a mechanic is busy
        validateMechanic(mechanic);
        mechanicRepository.createMechanic(mechanic);
        return mechanic;
    }

    public Mechanic replaceMechanic(UUID id, Mechanic mechanic) {
        Mechanic existing = getMechanic(id);

        mechanic.setId(id);
        mechanic.setBusyUntil(existing.getBusyUntil());
        validateMechanic(mechanic);

        mechanicRepository.replaceMechanic(id, mechanic);
        return mechanic;
    }

    public Mechanic updateMechanic(UUID id, String body) {
        Mechanic updated = patch(getMechanic(id), body, EDITABLE_FIELDS);

        validateMechanic(updated);

        mechanicRepository.replaceMechanic(id, updated);
        return updated;
    }

    public void deleteMechanic(UUID id) {
        getMechanic(id);
        mechanicRepository.deleteMechanic(id);
    }
}
