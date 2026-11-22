package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.Ping;
import CarCareBackend.src.main.java.carcare.service.PingService;
import java.util.List;

public class PingController extends BaseController{

    private final PingService pingService=new PingService();

    public Response createPing(Request request) throws Exception{
        String body=request.getBody();
        Ping ping=revertToJson(body,Ping.class);
        pingService.createPing(ping);
        return responseBuilder(201,"Ping created successfully");
    }

    public Response getPing(Request request) throws Exception{
        String id=extractId(request);
        Ping ping=pingService.getPing(id);
        return responseBuilder(200,ping);
    }

    public Response getRecipients(Request request) throws Exception{
        return responseBuilder(501,"Recipient retrieval not implemented yet");
    }

    public Response replacePing(Request request) throws Exception{
        String id=extractId(request);
        String body=request.getBody();
        pingService.replacePing(id,revertToJson(body,Ping.class));
        return responseBuilder(200,"Ping replaced successfully");
    }

    public Response updatePing(Request request) throws Exception{
        String id=extractId(request);
        String body=request.getBody();
        pingService.updatePing(id,body);
        return responseBuilder(200,"Ping updated successfully");
    }

    public Response deletePing(Request request) throws Exception{
        String id=extractId(request);
        pingService.deletePing(id);
        return responseBuilder(200,"Ping deleted successfully");
    }
}