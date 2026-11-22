package CarCareBackend.src.main.java.carcare.http;

import com.sun.net.httpserver.Headers;

public class Request {
    private String method;
    private String uri;
    private Headers headers;
    private String body;

    public Request(String method, String uri, Headers headers, String body) {
        this.method=method;
        this.uri=uri;
        this.headers=headers;
        this.body=body;
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

}
