package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.ServiceCenter;
import CarCareBackend.src.main.java.carcare.service.ServiceCentreService;
import java.util.List;

public class ServiceCentreController extends BaseController{

    private final ServiceCentreService serviceCentreService=new ServiceCentreService();

    public Response getAllServiceCentres(Request request) throws Exception{
        List<ServiceCenter> data=serviceCentreService.getAllServiceCentres();
        return responseBuilder(200,data);
    }

    public Response getServiceCentre(Request request) throws Exception {
        String id=extractId(request);
        ServiceCenter serviceCenter=serviceCentreService.getServiceCentre(id);
        return responseBuilder(200,serviceCenter);
    }

    public Response createServiceCentre(Request request) throws Exception {
        String body=request.getBody();
        ServiceCenter serviceCenter=revertToJson(body,ServiceCenter.class);
        serviceCentreService.createServiceCentre(serviceCenter);
        return responseBuilder(201,"Service centre registered successfully");
    }

    public Response replaceServiceCentre(Request request) throws Exception {
        String id=extractId(request);
        String body=request.getBody();
        serviceCentreService.replaceServiceCentre(id,revertToJson(body,ServiceCenter.class));
        return responseBuilder(200,"Service centre replaced successfully");
    }

    public Response updateServiceCentre(Request request) throws Exception {
        String id=extractId(request);
        String body=request.getBody();
        serviceCentreService.updateServiceCentre(id,body);
        return responseBuilder(200,"Service centre updated successfully");
    }

    public Response deleteServiceCentre(Request request) throws Exception {
        String id=extractId(request);
        serviceCentreService.deleteServiceCentre(id);
        return responseBuilder(200,"Service centre deleted successfully");
    }
}