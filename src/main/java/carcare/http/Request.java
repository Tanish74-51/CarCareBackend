package CarCareBackend.src.main.java.carcare.http;

import com.sun.net.httpserver.Headers;

import java.util.Map;
import java.util.UUID;

public class Request {
    private final String method;
    private final String uri;            // full uri INCLUDING the query string
    private final Headers headers;
    private final String body;
    private final Map<String, String> pathParams;   // e.g. {id} from /mechanics/{id}
    private final UUID userId;           // who is logged in (null on public routes)
    private final UUID token;            // the token that was sent (null on public routes)

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