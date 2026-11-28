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


    public Response register(Request request) {
        User created = userService.createUser(revertToJson(request.getBody(), User.class));
        return responseBuilder(201, created);
    }

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


    public Response logout(Request request) {
        Sessions.logout(request.getToken());
        return responseBuilder(200, "Logged out");
    }

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
        Sessions.logoutUser(request.getUserId());
        return responseBuilder(200, "User deleted successfully");
    }
}
