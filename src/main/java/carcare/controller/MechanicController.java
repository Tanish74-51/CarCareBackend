package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.Mechanic;
import CarCareBackend.src.main.java.carcare.service.MechanicService;

import java.util.List;
import java.util.UUID;

public class MechanicController extends BaseController {

    private final MechanicService mechanicService = new MechanicService();

    public Response getAllMechanics(Request request) {
        List<Mechanic> data = mechanicService.getAllMechanics();
        return responseBuilder(200, data);
    }

    public Response getMechanic(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, mechanicService.getMechanic(id));
    }

    public Response createMechanic(Request request) {
        Mechanic created = mechanicService.createMechanic(revertToJson(request.getBody(), Mechanic.class));
        return responseBuilder(201, created);
    }

    public Response replaceMechanic(Request request) {
        UUID id = extractId(request);
        Mechanic replaced = mechanicService.replaceMechanic(id, revertToJson(request.getBody(), Mechanic.class));
        return responseBuilder(200, replaced);
    }

    public Response updateMechanic(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, mechanicService.updateMechanic(id, request.getBody()));
    }

    public Response deleteMechanic(Request request) {
        UUID id = extractId(request);
        mechanicService.deleteMechanic(id);
        return responseBuilder(200, "Mechanic deleted successfully");
    }
}
