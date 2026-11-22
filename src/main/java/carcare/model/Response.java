package CarCareBackend.src.main.java.carcare.model;

import com.sun.net.httpserver.Headers;

public class Response{
    int statusCode;
    Headers headers;
    String body;

    public Response(int statusCode,Headers headers,String body){
        this.statusCode=statusCode;
        this.headers=headers;
        this.body=body;
    }

    public int getStatusCode(){
        return statusCode;
    }

    public Headers getHeaders(){
        return headers;
    }

    public String getBody(){
        return body;
    }
}
