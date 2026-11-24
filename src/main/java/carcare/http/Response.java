package CarCareBackend.src.main.java.carcare.http;

import CarCareBackend.src.main.java.carcare.util.Json;
import com.sun.net.httpserver.Headers;

import java.util.Map;

public class Response {
    private final int statusCode;
    private final Headers headers;
    private final String body;

    public Response(int statusCode, Headers headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public static Response json(int statusCode, Object data) {
        Headers headers = new Headers();
        headers.set("Content-Type", "application/json; charset=utf-8");
        return new Response(statusCode, headers, Json.toJson(data));
    }

    public static Response message(int statusCode, String message) {
        return json(statusCode, Map.of("message", message));
    }

    public static Response error(int statusCode, String message) {
        return json(statusCode, Map.of("error", message));
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Headers getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
