package CarCareBackend.src.main.java.carcare.service;

import CarCareBackend.src.main.java.carcare.model.Ping;
import CarCareBackend.src.main.java.carcare.model.User;
import CarCareBackend.src.main.java.carcare.model.Vehicle;
import CarCareBackend.src.main.java.carcare.repository.PingRepository;
import CarCareBackend.src.main.java.carcare.repository.UserRepository;
import CarCareBackend.src.main.java.carcare.repository.VehicleRepository;

import java.time.LocalDateTime;


import CarCareBackend.src.main.java.carcare.config.Timers;
import CarCareBackend.src.main.java.carcare.model.Ping;
import CarCareBackend.src.main.java.carcare.model.User;
import CarCareBackend.src.main.java.carcare.model.Vehicle;
import CarCareBackend.src.main.java.carcare.repository.PingRepository;
import CarCareBackend.src.main.java.carcare.repository.UserRepository;
import CarCareBackend.src.main.java.carcare.repository.VehicleRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PingService extends BaseService {

    private final PingRepository pingRepository = new PingRepository();
    private final UserRepository userRepository = new UserRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();

    private static final String ACTIVE = "ACTIVE";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";

    // status, helper, owner and timestamps can never be changed by the client
    private static final Set<String> EDITABLE_FIELDS = Set.of("title", "description", "address");

    // what the client sees for "who accepted my ping"
    public record Helper(UUID id, String name, String phoneNumber) {}

    // what the client sees: the ping, the helper (only shown to the person who asked) and a ready-to-show message
    public record PingDetails(Ping ping, Helper helper, String message) {}

    private PingDetails details(Ping ping, UUID viewerId) {
        boolean isOwner = viewerId.equals(ping.getUserId());
        String place = ping.getCity() + ", " + ping.getState();

        Helper helper = null;
        if (isOwner && ping.getAcceptedBy() != null) {
            User user = userRepository.getUser(ping.getAcceptedBy());
            if (user != null) {
                helper = new Helper(user.getId(), user.getName(), user.getPhoneNumber());
            }
        }

        String message = switch (ping.getStatus()) {
            case ACTIVE -> userRepository.countOthersInCityAndState(ping.getCity(), ping.getState(), ping.getUserId()) == 0
                    ? "There is no other user registered in " + place + ", so nobody can help yet."
                    : "Everyone in " + place + " is busy helping someone else right now. Check again in a moment.";
            case ACCEPTED -> isOwner ? "A helper has accepted your ping" : "You are helping with this ping";
            case COMPLETED -> "Ping completed";
            case CANCELLED -> "Ping cancelled";
            default -> "";
        };
        return new PingDetails(ping, helper, message);
    }

    public void validatePing(Ping ping) {
        require(ping.getUserId(), "User ID is required");
        require(ping.getVehicleId(), "Vehicle ID is required");

        if (userRepository.getUser(ping.getUserId()) == null) {
            throw new IllegalArgumentException("User does not exist");
        }

        Vehicle vehicle = vehicleRepository.getVehicle(ping.getVehicleId());
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle does not exist");
        }
        if (!vehicle.getOwnerId().equals(ping.getUserId())) {
            throw new IllegalArgumentException("Vehicle does not belong to this user");
        }

        ping.setTitle(requireString(ping.getTitle(), "Title"));
        ping.setDescription(requireText(ping.getDescription(), "Description"));
        ping.setAddress(requireString(ping.getAddress(), "Address"));
        ping.setCity(requireString(ping.getCity(), "City"));
        ping.setState(requireString(ping.getState(), "State"));
    }

    // pings I created plus pings where I am the helper
    public List<Ping> getPings(UUID userId) {
        return pingRepository.getPingsForUser(userId);
    }

    // a ping can only be seen by the user who created it and by the helper who accepted it
    public Ping getPing(UUID id, UUID userId) {
        Ping ping = found(pingRepository.getPing(id), "Ping not found");

        if (!userId.equals(ping.getUserId()) && !userId.equals(ping.getAcceptedBy())) {
            throw new SecurityException("This ping belongs to other users");
        }
        return ping;
    }

    public PingDetails getPingDetails(UUID id, UUID userId) {
        Ping ping = getPing(id, userId);

        // nobody was free when the ping was made: look again now (a busy helper may be free by now)
        if (ping.getStatus().equals(ACTIVE) && userId.equals(ping.getUserId())) {
            User helper = pickHelper(ping);
            if (helper != null) {
                if (!pingRepository.claimPing(id, helper.getId(), LocalDateTime.now())) {
                    userRepository.markFree(helper.getId());       // another call matched this ping first
                }
                ping = pingRepository.getPing(id);
            }
        }
        return details(ping, userId);
    }

    // creates the ping and immediately picks a free user from the same city and state as the helper
    public PingDetails createPing(Ping ping, UUID userId) {
        LocalDateTime now = LocalDateTime.now();

        ping.setId(UUID.randomUUID());
        ping.setUserId(userId);
        ping.setStatus(ACTIVE);
        ping.setAcceptedBy(null);
        ping.setAcceptedAt(null);
        ping.setCompletedAt(null);
        ping.setCreatedAt(now);
        ping.setUpdatedAt(now);

        // city and state always come from the user's registration, the client doesn't send them
        User owner = userRepository.getUser(userId);
        if (owner == null) {
            throw new IllegalArgumentException("User does not exist");
        }
        ping.setCity(owner.getCity());
        ping.setState(owner.getState());

        validatePing(ping);

        User helper = pickHelper(ping);
        if (helper != null) {
            ping.setAcceptedBy(helper.getId());
            ping.setAcceptedAt(now);
            ping.setStatus(ACCEPTED);
        }
        // nobody free in that city/state: the ping stays ACTIVE and is matched later when it is checked again

        try {
            pingRepository.createPing(ping);
        } catch (RuntimeException e) {
            if (helper != null) {
                userRepository.markFree(helper.getId());
            }
            throw e;
        }
        return details(ping, userId);
    }

    // Picks a random other user from the same city and state who is not already helping someone, and holds
    // them for HELPER_BUSY_SECONDS (30). markBusy only succeeds for one caller, so nobody gets two pings at once.
    private User pickHelper(Ping ping) {
        List<User> free = userRepository.getFreeUsersByCityAndState(ping.getCity(), ping.getState(), ping.getUserId());
        Collections.shuffle(free, random);

        LocalDateTime busyUntil = LocalDateTime.now().plusSeconds(Timers.HELPER_BUSY_SECONDS);
        for (User user : free) {
            if (userRepository.markBusy(user.getId(), busyUntil)) {
                return user;
            }
        }
        return null;
    }

    // the ping is over: free the helper now, unless they have since accepted another ping
    private void releaseHelper(Ping ping) {
        UUID helperId = ping.getAcceptedBy();
        if (helperId != null && pingRepository.getActiveForHelper(helperId, ping.getId()).isEmpty()) {
            userRepository.markFree(helperId);
        }
    }

    // only the user who created the ping can edit it
    public PingDetails updatePing(UUID id, String json, UUID userId) {
        Ping existing = getPing(id, userId);
        requireOwner(existing.getUserId(), userId);
        checkNotClosed(existing);

        Ping updated = patch(existing, json, EDITABLE_FIELDS);

        updated.setUpdatedAt(LocalDateTime.now());
        validatePing(updated);

        pingRepository.replacePing(id, updated);
        return details(updated, userId);
    }

    // either side can mark it done: the user who asked for help ("my problem is solved") or the helper who accepted.
    // getPing already refuses anybody else.
    public PingDetails completePing(UUID id, UUID userId) {
        Ping ping = getPing(id, userId);

        if (ping.getStatus().equals(ACTIVE)) {
            throw new IllegalStateException("Nobody has accepted this ping yet, so it cannot be completed. Cancel it instead.");
        }

        if (!ping.getStatus().equals(ACCEPTED)) {
            throw new IllegalStateException("This ping is " + ping.getStatus() + " and cannot be completed");
        }

        ping.setStatus(COMPLETED);
        ping.setCompletedAt(LocalDateTime.now());
        ping.setUpdatedAt(LocalDateTime.now());

        pingRepository.replacePing(id, ping);
        releaseHelper(ping);
        return details(ping, userId);
    }

    // only the user who created the ping can cancel it
    public PingDetails cancelPing(UUID id, UUID userId) {
        Ping ping = getPing(id, userId);
        requireOwner(ping.getUserId(), userId);

        checkNotClosed(ping);

        ping.setStatus(CANCELLED);
        ping.setUpdatedAt(LocalDateTime.now());

        pingRepository.replacePing(id, ping);
        releaseHelper(ping);
        return details(ping, userId);
    }

    // shows which user accepted the ping (only to the user who created it)
    public Helper getHelper(UUID id, UUID userId) {
        Ping ping = getPing(id, userId);
        requireOwner(ping.getUserId(), userId);

        if (ping.getAcceptedBy() == null) {
            throw new IllegalStateException("No one has accepted this ping yet");
        }

        User helper = found(userRepository.getUser(ping.getAcceptedBy()), "Helper not found");

        return new Helper(helper.getId(), helper.getName(), helper.getPhoneNumber());
    }

    private void checkNotClosed(Ping ping) {
        if (ping.getStatus().equals(COMPLETED) || ping.getStatus().equals(CANCELLED)) {
            throw new IllegalStateException("This ping is " + ping.getStatus() + " and cannot be changed");
        }
    }
}