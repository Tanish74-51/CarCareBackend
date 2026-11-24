package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.ServiceVisit;
import CarCareBackend.src.main.java.carcare.service.ServiceVisitService;
import CarCareBackend.src.main.java.carcare.service.ServiceVisitService.VisitDetails;

import java.util.List;
import java.util.UUID;

public class ServiceVisitController extends BaseController {

    private final ServiceVisitService serviceVisitService = new ServiceVisitService();

    // GET /service-visits   the bookings of the logged-in user
    public Response getMyServiceVisits(Request request) {
        List<VisitDetails> data = serviceVisitService.getServiceVisits(request.getUserId());
        return responseBuilder(200, data);
    }

    public Response getServiceVisit(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, serviceVisitService.getServiceVisitDetails(id, request.getUserId()));
    }

    // the mechanic is picked by the server
    public Response createServiceVisit(Request request) {
        VisitDetails created = serviceVisitService.createServiceVisit(
                revertToJson(request.getBody(), ServiceVisit.class), request.getUserId());
        return responseBuilder(201, created);
    }

    public Response replaceServiceVisit(Request request) {
        UUID id = extractId(request);
        VisitDetails replaced = serviceVisitService.replaceServiceVisit(
                id, revertToJson(request.getBody(), ServiceVisit.class), request.getUserId());
        return responseBuilder(200, replaced);
    }

    public Response updateServiceVisit(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, serviceVisitService.updateServiceVisit(id, request.getBody(), request.getUserId()));
    }

    public Response deleteServiceVisit(Request request) {
        UUID id = extractId(request);
        serviceVisitService.deleteServiceVisit(id, request.getUserId());
        return responseBuilder(200, "Service visit deleted successfully");
    }

    // GET /admin/service-visits   (admin) every booking
    public Response adminList(Request request) {
        return responseBuilder(200, serviceVisitService.getAllServiceVisits());
    }

    // PATCH /admin/service-visits/{id}   (admin) status, cost, notes
    public Response adminUpdate(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, serviceVisitService.adminUpdate(id, request.getBody()));
    }
}
