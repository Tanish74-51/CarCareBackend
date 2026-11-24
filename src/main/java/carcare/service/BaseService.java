package CarCareBackend.src.main.java.carcare.service;

import CarCareBackend.src.main.java.carcare.util.Json;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class BaseService {

    protected static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    protected static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{10,15}$"
    );

    protected static final Random random = new Random();

    // each of these returns the cleaned (trimmed) value, or throws IllegalArgumentException -> HTTP 400

    protected String requireString(String value, String fieldName) {
        return requireString(value, fieldName, 255);
    }

    protected String requireText(String value, String fieldName) {
        return requireString(value, fieldName, 2000);
    }

    private String requireString(String value, String fieldName, int max) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(fieldName + " is too long");
        }
        return trimmed;
    }

    protected String optionalText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireString(value, fieldName, 2000);
    }

    protected String requireEmail(String email) {
        String trimmed = requireString(email, "Email");

        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email address");
        }
        return trimmed;
    }

    protected String requirePhone(String phoneNumber) {
        String trimmed = requireString(phoneNumber, "Phone number");

        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid phone number");
        }
        return trimmed;
    }

    protected void requireNonNegative(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }

    protected <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // a missing record is a 404, not a 400
    protected <T> T found(T value, String message) {
        if (value == null) {
            throw new NoSuchElementException(message);
        }
        return value;
    }

    protected UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid UUID");
        }
    }

    // PATCH: only the fields listed in "allowed" may appear in the body; they are applied onto "existing"
    protected <T> T patch(T existing, String body, Set<String> allowed) {
        JsonNode changes = Json.objectBody(body);
        changes.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException("Field cannot be modified (or does not exist): " + field);
            }
        });
        return Json.merge(existing, changes);
    }

    // a user may only touch their own records: anything else is HTTP 403
    protected void requireOwner(UUID ownerId, UUID userId) {
        if (ownerId == null || !ownerId.equals(userId)) {
            throw new SecurityException("This record belongs to another user");
        }
    }
}

