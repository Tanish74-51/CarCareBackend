package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.util.Json;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;

public class BaseController {

    public Response responseBuilder(int statusCode, Object data) {
        return Response.json(statusCode, data);
    }

    public Response responseBuilder(int statusCode, String message) {
        return Response.message(statusCode, message);
    }

    protected <T> T revertToJson(String json, Class<T> objClass) {
        return Json.read(json, objClass);
    }

    public UUID extractId(Request request) {
        return parseUUID(request.getPathParams().get("id"), "id");
    }

    public UUID parseUUID(String value, String name) {
        if (value == null || value.length() != 36) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }

    public HashMap<String, String> extractParameters(Request request) {
        HashMap<String, String> map = new HashMap<>();

        String uri = request.getUri();
        String[] split = uri.split("\\?", 2);

        if (split.length < 2) return map;

        String[] parameters = split[1].split("&");
        for (String parameter : parameters) {
            String[] keyValue = parameter.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    map.put(URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Malformed query string");
                }
            }
        }
        return map;
    }

    public String requireParameter(HashMap<String, String> parameters, String name) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}

