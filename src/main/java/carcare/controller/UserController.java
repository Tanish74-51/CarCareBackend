package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.http.Sessions;
import CarCareBackend.src.main.java.carcare.model.User;
import CarCareBackend.src.main.java.carcare.service.UserService;
import CarCareBackend.src.main.java.carcare.util.Json;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public class UserController extends BaseController {

    private final UserService userService = new UserService();

    // POST /users   (public) register a new user   body: name, email, password, phoneNumber, address, city, state
    public Response register(Request request) {
        User created = userService.createUser(revertToJson(request.getBody(), User.class));
        return responseBuilder(201, created);
    }

    // POST /login   (public)   body: {"email": "...", "password": "..."}   ->   {"token": "<uuid>", "user": {...}}
    // The password is only used here. The client keeps the token and sends it in the X-Token header from then on.
    public Response login(Request request) {
        JsonNode body = Json.objectBody(request.getBody());
        JsonNode email = body.get("email");
        JsonNode password = body.get("password");
        if (email == null || !email.isTextual() || password == null || !password.isTextual()) {
            throw new IllegalArgumentException("Email and password are required");
        }

        User user = userService.login(email.asText(), password.asText());
        UUID token = Sessions.login(user.getId(), user.getRole());
        return responseBuilder(200, Map.of("token", token.toString(), "user", user));
    }

    // POST /logout
    public Response logout(Request request) {
        Sessions.logout(request.getToken());
        return responseBuilder(200, "Logged out");
    }

    // GET /me   the logged-in user (the id comes from the token, not from the url)
    public Response getMe(Request request) {
        return responseBuilder(200, userService.getUser(request.getUserId()));
    }

    public Response replaceMe(Request request) {
        User replaced = userService.replaceUser(request.getUserId(), revertToJson(request.getBody(), User.class));
        return responseBuilder(200, replaced);
    }

    public Response updateMe(Request request) {
        return responseBuilder(200, userService.updateUser(request.getUserId(), request.getBody()));
    }

    public Response deleteMe(Request request) {
        userService.deleteUser(request.getUserId());
        Sessions.logoutUser(request.getUserId());       // every token of this account, not just this one
        return responseBuilder(200, "User deleted successfully");
    }
}
