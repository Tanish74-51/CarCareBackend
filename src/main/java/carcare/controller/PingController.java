package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.Ping;
import CarCareBackend.src.main.java.carcare.service.PingService;
import CarCareBackend.src.main.java.carcare.service.PingService.PingDetails;

import java.util.UUID;

public class PingController extends BaseController {

    private final PingService pingService = new PingService();

    // GET /pings   the pings I created plus the pings where I am the helper
    public Response getMyPings(Request request) {
        return responseBuilder(200, pingService.getPings(request.getUserId()));
    }

    // creates the ping; a user from the same city and state is picked as the helper automatically
    public Response createPing(Request request) {
        PingDetails created = pingService.createPing(revertToJson(request.getBody(), Ping.class), request.getUserId());
        return responseBuilder(201, created);
    }

    public Response getPing(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, pingService.getPingDetails(id, request.getUserId()));
    }

    public Response updatePing(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, pingService.updatePing(id, request.getBody(), request.getUserId()));
    }

    // POST /pings/{id}/complete   (the user who asked or the helper who accepted; who is asking comes from the token)
    public Response completePing(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, pingService.completePing(id, request.getUserId()));
    }

    // DELETE /pings/{id}   (only the user who created it)
    public Response cancelPing(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, pingService.cancelPing(id, request.getUserId()));
    }

    // GET /pings/{id}/helper   which user accepted
    public Response getHelper(Request request) {
        UUID id = extractId(request);
        return responseBuilder(200, pingService.getHelper(id, request.getUserId()));
    }
}
