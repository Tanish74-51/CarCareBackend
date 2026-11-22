package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.Vehicle;
import CarCareBackend.src.main.java.carcare.service.VehicleService;
import java.util.List;

public class VehicleController extends BaseController{

    private final VehicleService vehicleService=new VehicleService();

    public Response getAllVehicles(Request request) throws Exception{
        List<Vehicle> data=vehicleService.getAllVehicles();
        return responseBuilder(200,data);
    }

    public Response getVehicle(Request request) throws Exception {
        String id=extractId(request);
        Vehicle vehicle=vehicleService.getVehicle(id);
        return responseBuilder(200,vehicle);
    }

    public Response createVehicle(Request request) throws Exception {
        String body=request.getBody();
        Vehicle vehicle=revertToJson(body,Vehicle.class);
        vehicleService.createVehicle(vehicle);
        return responseBuilder(201,"Vehicle registered successfully");

    }

    public Response replaceVehicle(Request request) throws Exception {
        String id=extractId(request);
        String body=request.getBody();
        vehicleService.replaceVehicle(id,revertToJson(body,Vehicle.class));
        return responseBuilder(200,"Vehicle replaced successfully");
    }

    public Response updateVehicle(Request request) throws Exception {
        String id=extractId(request);
        String body=request.getBody();
        vehicleService.updateVehicle(id,body);
        return responseBuilder(200,"Vehicle updated successfully");
    }

    public Response deleteVehicle(Request request) throws Exception {
        String id=extractId(request);
        vehicleService.deleteVehicle(id);
        return responseBuilder(200,"Vehicle deleted successfully");
    }
}


