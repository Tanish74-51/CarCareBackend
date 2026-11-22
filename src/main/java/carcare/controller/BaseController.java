package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.model.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.Headers;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseController {

    ObjectMapper mapper=new ObjectMapper();


    public Response responseBuilder(Object data) {
        Headers headers = new Headers();
        headers.set("Content-Type", "application/json");
        return new Response(200, headers, convertToJson(data));
    }

    public String convertToJson(Object data) {
        try {
            return mapper.writeValueAsString(data);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
