package CarCareBackend.src.main.java.carcare.model;

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

}
