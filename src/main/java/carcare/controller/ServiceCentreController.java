package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.ServiceCentre;
import CarCareBackend.src.main.java.carcare.service.ServiceCentreService;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ServiceCentreController extends BaseController {

    private final ServiceCentreService serviceCentreService = new ServiceCentreService();

    public Response getAllServiceCentres(Request request) {
        List<ServiceCentre> data = serviceCentreService.getAllServiceCentres();
        return responseBuilder(200, data);
    }

    public Response getServiceCentre(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, serviceCentreService.getServiceCentre(id));
    }

    public Response createServiceCentre(Request request) {
        ServiceCentre created = serviceCentreService.createServiceCentre(revertToJson(request.getBody(), ServiceCentre.class));
        return responseBuilder(201, created);
    }

    public Response replaceServiceCentre(Request request) {
        UUID id = extractId(request);
        ServiceCentre replaced = serviceCentreService.replaceServiceCentre(id, revertToJson(request.getBody(), ServiceCentre.class));
        return responseBuilder(200, replaced);
    }

    public Response updateServiceCentre(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, serviceCentreService.updateServiceCentre(id, request.getBody()));
    }

    public Response deleteServiceCentre(Request request) {
        UUID id = extractId(request);
        serviceCentreService.deleteServiceCentre(id);
        return responseBuilder(200, "Service centre deleted successfully");
    }

    // GET /service-centres/nearby?city=Mohali[&state=Punjab]
    // The centres in that city only. Without ?city the logged-in user's own city is used.
    // Answers {"city", "state", "serviceCentres": [...], "message": "..."}; the list is empty when the city has none.
    public Response getNearby(Request request) {
        HashMap<String, String> parameters = extractParameters(request);
        return responseBuilder(200, serviceCentreService.findNearby(request.getUserId(), parameters.get("city"), parameters.get("state")));
    }
}