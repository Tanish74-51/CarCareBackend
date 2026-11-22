package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.ServiceVisit;
import CarCareBackend.src.main.java.carcare.service.ServiceVisitService;
import java.util.List;

public class ServiceVisitController extends BaseController{

    private final ServiceVisitService serviceVisitService=new ServiceVisitService();

    public Response getAllServiceVisits(Request request) throws Exception{
        List<ServiceVisit> data=serviceVisitService.getAllServiceVisits();
        return responseBuilder(200,data);
    }

    public Response getServiceVisit(Request request) throws Exception {
        String id=extractId(request);
        ServiceVisit serviceVisit=serviceVisitService.getServiceVisit(id);
        return responseBuilder(200,serviceVisit);
    }

    public Response createServiceVisit(Request request) throws Exception {
        String body=request.getBody();
        ServiceVisit serviceVisit=revertToJson(body,ServiceVisit.class);
        serviceVisitService.createServiceVisit(serviceVisit);
        return responseBuilder(201,"Service visit registered successfully");
    }

    public Response replaceServiceVisit(Request request) throws Exception {
        String id=extractId(request);
        String body=request.getBody();
        serviceVisitService.replaceServiceVisit(id,revertToJson(body,ServiceVisit.class));
        return responseBuilder(200,"Service visit replaced successfully");
    }

    public Response updateServiceVisit(Request request) throws Exception {
        String id=extractId(request);
        String body=request.getBody();
        serviceVisitService.updateServiceVisit(id,body);
        return responseBuilder(200,"Service visit updated successfully");
    }

    public Response deleteServiceVisit(Request request) throws Exception {
        String id=extractId(request);
        serviceVisitService.deleteServiceVisit(id);
        return responseBuilder(200,"Service visit deleted successfully");
    }
}