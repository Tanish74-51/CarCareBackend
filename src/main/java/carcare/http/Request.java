package CarCareBackend.src.main.java.carcare.http;

import com.sun.net.httpserver.Headers;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Request {
    private final String method;
    private final String uri;
    private final Headers headers;
    private final String body;
    private final Map<String, String> pathParams;
    private final UUID userId;
    private final UUID token;

    public Request(String method, String uri, Headers headers, String body,
                   Map<String, String> pathParams, UUID userId, UUID token) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
        this.pathParams = pathParams;
        this.userId = userId;
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this==o) return true;
        if (o==null || getClass()!=o.getClass()) return false;
        Request other=(Request) o;
        return Objects.equals(uri, other.uri) && Objects.equals(userId, other.userId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(uri,userId);
    }

    public String getUri() {
        return uri;
    }
    public String getBody() {
        return body;
    }
    public String getMethod() {
        return method;
    }
    public Headers getHeaders() {
        return headers;
    }
    public Map<String, String> getPathParams() {
        return pathParams;
    }
    public UUID getUserId() {
        return userId;
    }
    public UUID getToken() {
        return token;
    }
}