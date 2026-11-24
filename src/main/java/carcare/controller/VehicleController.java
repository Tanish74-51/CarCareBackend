package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.Vehicle;
import CarCareBackend.src.main.java.carcare.service.VehicleService;

import java.util.List;
import java.util.UUID;

public class VehicleController extends BaseController {

    private final VehicleService vehicleService = new VehicleService();

    // GET /vehicles   only the vehicles of the logged-in user
    public Response getMyVehicles(Request request) {
        List<Vehicle> data = vehicleService.getUserVehicles(request.getUserId());
        return responseBuilder(200, data);
    }

    public Response getVehicle(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, vehicleService.getVehicle(id, request.getUserId()));
    }

    public Response createVehicle(Request request) {
        Vehicle created = vehicleService.createVehicle(revertToJson(request.getBody(), Vehicle.class), request.getUserId());
        return responseBuilder(201, created);
    }

    public Response replaceVehicle(Request request) {
        UUID id = extractId(request);
        Vehicle replaced = vehicleService.replaceVehicle(id, revertToJson(request.getBody(), Vehicle.class), request.getUserId());
        return responseBuilder(200, replaced);
    }

    public Response updateVehicle(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, vehicleService.updateVehicle(id, request.getBody(), request.getUserId()));
    }

    public Response deleteVehicle(Request request) {
        UUID id = extractId(request);
        vehicleService.deleteVehicle(id, request.getUserId());
        return responseBuilder(200, "Vehicle deleted successfully");
    }
}

