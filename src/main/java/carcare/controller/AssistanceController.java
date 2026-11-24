package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.Assistance;
import CarCareBackend.src.main.java.carcare.service.AssistanceService;
import CarCareBackend.src.main.java.carcare.service.AssistanceService.AssistanceDetails;

import java.util.List;
import java.util.UUID;

public class AssistanceController extends BaseController {

    private final AssistanceService assistanceService = new AssistanceService();

    // GET /assistance-requests   only the requests of the logged-in user
    public Response getMyRequests(Request request) {
        List<Assistance> requests = assistanceService.getAssistanceRequests(request.getUserId());
        return responseBuilder(200, requests);
    }

    public Response getRequest(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, assistanceService.getAssistanceDetails(id, request.getUserId()));
    }

    // creates the request; a free mechanic from the user's city and state is picked automatically
    public Response createRequest(Request request) {
        AssistanceDetails created = assistanceService.createAssistanceRequest(
                revertToJson(request.getBody(), Assistance.class), request.getUserId()
        );
        return responseBuilder(201, created);
    }

    public Response replaceRequest(Request request) {
        UUID id = extractId(request);
        AssistanceDetails replaced = assistanceService.replaceAssistanceRequest(
                id, revertToJson(request.getBody(), Assistance.class), request.getUserId()
        );
        return responseBuilder(200, replaced);
    }

    public Response updateRequest(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, assistanceService.updateAssistanceRequest(id, request.getBody(), request.getUserId()));
    }

    public Response cancelRequest(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, assistanceService.cancelAssistanceRequest(id, request.getUserId()));
    }

    // POST /assistance-requests/{id}/complete   the customer says the job is done
    public Response completeRequest(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, assistanceService.completeAssistanceRequest(id, request.getUserId()));
    }

    // GET /assistance-requests/{id}/mechanic   which mechanic accepted
    public Response getMechanic(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, assistanceService.getMechanic(id, request.getUserId()));
    }

    // GET /admin/assistance-requests   (admin) every request, newest first
    public Response adminList(Request request) {
        return responseBuilder(200, assistanceService.getAllRequests());
    }

    // PATCH /admin/assistance-requests/{id}   (admin) status, estimatedCost, finalCost
    public Response adminUpdate(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, assistanceService.adminUpdate(id, request.getBody()));
    }
}
