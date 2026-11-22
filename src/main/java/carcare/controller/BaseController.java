package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.Headers;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseController {

    ObjectMapper mapper=new ObjectMapper();


    public Response responseBuilder(int statusCode,Object data) {
        Headers headers=new Headers();
        headers.set("Content-Type", "application/json");
        return new Response(statusCode,headers,convertToJson(data));
    }

    public Response responseBuilder(int statusCode,String message) {
        Headers headers=new Headers();
        headers.set("Content-Type", "application/json");
        return new Response(statusCode,headers,message);
    }



    public String convertToJson(Object data) {
        try {
            return mapper.writeValueAsString(data);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T revertToJson(String json, Class<T> objClass) throws JsonProcessingException {
        return mapper.readValue(json,objClass);
    }

    public String extractId(Request request){
        String path=request.getUri().toString();
        path=path.substring(1);
        String[] split=path.split("/");
        return split[split.length-1];
    }
}
