package CarCareBackend.src.main.java.carcare.service;

import CarCareBackend.src.main.java.carcare.model.User;
import CarCareBackend.src.main.java.carcare.repository.UserRepository;
import CarCareBackend.src.main.java.carcare.util.Passwords;
import CarCareBackend.src.main.java.carcare.util.UnauthorizedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class UserService extends BaseService {

    private final UserRepository userRepository = new UserRepository();

    // role, password and the busy timer are not editable through PATCH
    private static final Set<String> EDITABLE_FIELDS = Set.of(
            "name", "email", "phoneNumber", "address", "city", "state"
    );

    // verified against when the email is unknown, so "wrong email" and "wrong password" take equally long
    private static final String DUMMY_HASH = Passwords.hash("not-a-real-password");

    public void validateUser(User user) {
        user.setName(requireString(user.getName(), "Name"));
        user.setEmail(requireEmail(user.getEmail()).toLowerCase(Locale.ROOT));
        user.setPhoneNumber(requirePhone(user.getPhoneNumber()));
        user.setAddress(requireString(user.getAddress(), "Address"));
        user.setCity(requireString(user.getCity(), "City"));
        user.setState(requireString(user.getState(), "State"));
    }

    private String requirePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (password.length() > 128) {
            throw new IllegalArgumentException("Password is too long");
        }
        return password;
    }

    private void checkEmailIsFree(String email, UUID ownId) {
        User other = userRepository.getUserByEmail(email);

        if (other != null && !other.getId().equals(ownId)) {
            throw new IllegalStateException("This email is already registered");
        }
    }

    public List<User> getAllUsers() {
        return userRepository.getAllUsers();
    }

    public User getUser(UUID id) {
        return found(userRepository.getUser(id), "User not found");
    }

    // registration: everybody who registers is a plain USER, whatever the body says
    public User createUser(User user) {
        LocalDateTime now = LocalDateTime.now();

        user.setId(UUID.randomUUID());
        user.setRole(User.USER);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        validateUser(user);
        String password = requirePassword(user.getPassword());
        checkEmailIsFree(user.getEmail(), user.getId());

        user.setPasswordHash(Passwords.hash(password));
        user.setPassword(null);

        userRepository.createUser(user);
        return user;
    }

    // email + password: the only time the password is used. The caller then hands out a token.
    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password are required");
        }
        User user = userRepository.getUserByEmail(email.trim().toLowerCase(Locale.ROOT));

        boolean ok = Passwords.verify(password, user == null ? DUMMY_HASH : user.getPasswordHash());
        if (user == null || !ok) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return user;
    }

    public User replaceUser(UUID id, User user) {
        User existing = getUser(id);

        user.setId(id);
        user.setRole(existing.getRole());
        user.setCreatedAt(existing.getCreatedAt());
        user.setUpdatedAt(LocalDateTime.now());
        user.setPassword(null);

        validateUser(user);
        checkEmailIsFree(user.getEmail(), id);

        userRepository.replaceUser(id, user);
        return user;
    }

    public User updateUser(UUID id, String body) {
        User updated = patch(getUser(id), body, EDITABLE_FIELDS);

        updated.setUpdatedAt(LocalDateTime.now());

        validateUser(updated);
        checkEmailIsFree(updated.getEmail(), id);

        userRepository.replaceUser(id, updated);
        return updated;
    }

    public void deleteUser(UUID id) {
        getUser(id);
        userRepository.deleteUser(id);
    }

    // The only way to get an admin: created at startup. Set ADMIN_EMAIL / ADMIN_PASSWORD to choose the
    // login; without ADMIN_PASSWORD a random one is generated and printed once.
    public void seedAdmin() {
        String email = System.getenv().getOrDefault("ADMIN_EMAIL", "admin@carcare.com").trim().toLowerCase(Locale.ROOT);
        if (userRepository.getUserByEmail(email) != null) {
            return;
        }

        String configured = System.getenv("ADMIN_PASSWORD");
        boolean generated = configured == null || configured.isBlank();
        String password = generated ? Passwords.randomPassword() : requirePassword(configured);

        User admin = new User();
        admin.setName("Administrator");
        admin.setEmail(email);
        admin.setPhoneNumber("0000000000");
        admin.setAddress("N/A");
        admin.setCity("N/A");
        admin.setState("N/A");
        admin.setRole(User.ADMIN);
        admin.setPasswordHash(Passwords.hash(password));
        userRepository.createUser(admin);

        System.out.println("Admin account created: " + email + (generated ? "  password: " + password + "  (shown once, note it down)" : ""));
    }
}