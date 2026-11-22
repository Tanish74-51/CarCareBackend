package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.Mechanic;
import CarCareBackend.src.main.java.carcare.service.MechanicService;
import java.util.List;

public class MechanicController extends BaseController{

    private final MechanicService mechanicService=new MechanicService();

    public Response getAllMechanics(Request request) throws Exception{
        List<Mechanic> data=mechanicService.getAllMechanics();
        return responseBuilder(200,data);
    }

    public Response getMechanic(Request request) throws Exception {
        String id=extractId(request);
        Mechanic mechanic=mechanicService.getMechanic(id);
        return responseBuilder(200,mechanic);
    }

    public Response createMechanic(Request request) throws Exception {
        String body=request.getBody();
        Mechanic mechanic=revertToJson(body,Mechanic.class);
        mechanicService.createMechanic(mechanic);
        return responseBuilder(201,"Mechanic registered successfully");
    }

    public Response replaceMechanic(Request request) throws Exception {
        String id=extractId(request);
        String body=request.getBody();
        mechanicService.replaceMechanic(id,revertToJson(body,Mechanic.class));
        return responseBuilder(200,"Mechanic replaced successfully");
    }

    public Response updateMechanic(Request request) throws Exception {
        String id=extractId(request);
        String body=request.getBody();
        mechanicService.updateMechanic(id,body);
        return responseBuilder(200,"Mechanic updated successfully");
    }

    public Response deleteMechanic(Request request) throws Exception {
        String id=extractId(request);
        mechanicService.deleteMechanic(id);
        return responseBuilder(200,"Mechanic deleted successfully");
    }
}
