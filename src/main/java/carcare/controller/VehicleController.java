package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.model.Request;
import CarCareBackend.src.main.java.carcare.model.Response;
import CarCareBackend.src.main.java.carcare.model.Vehicle;
import CarCareBackend.src.main.java.carcare.service.VehicleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.Headers;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class VehicleController extends BaseController{
    ObjectMapper mapper=new ObjectMapper();
    private final VehicleService vehicleService=new VehicleService();

    public VehicleController() {}

    public Response responseBuilder(List<Vehicle> data){
        Headers headers=new Headers();
        headers.add("Content-Type","application/json");

        return new Response(200,headers,convertToJson(data));
    }

    public String convertToJson(List<Vehicle> data){
        try {
            return mapper.writeValueAsString(data);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Response getAllVehicles(Request request) throws Exception {
        List<Vehicle> data=vehicleService.getAllVehicles();
        return responseBuilder(data);

    }

    public Response getVehicle(Request request) throws Exception {
        return null;
    }

    public Response createVehicle(Request request) throws Exception {
        return null;
    }

    public Response replaceVehicle(Request request) throws Exception {
        return null;
    }

    public Response updateVehicle(Request request) throws Exception {
        return null;
    }

    public Response deleteVehicle(Request request) throws Exception {
        return null;
    }
}


