package CarCareBackend.src.main.java.carcare.controller;

import CarCareBackend.src.main.java.carcare.http.Request;
import CarCareBackend.src.main.java.carcare.http.Response;
import CarCareBackend.src.main.java.carcare.model.Assistance;
import CarCareBackend.src.main.java.carcare.service.AssistanceService;

public class AssistanceController extends BaseController{

    private final AssistanceService assistanceService=new AssistanceService();

    public Response createRequest(Request request) throws Exception{
        String body=request.getBody();
        Assistance assistance=revertToJson(body,Assistance.class);
        assistanceService.createAssistanceRequest(assistance);
        return responseBuilder(201,"Assistance request created successfully");
    }

    public Response getRequest(Request request) throws Exception{
        String id=extractId(request);
        Assistance assistance=assistanceService.getAssistanceRequest(id);
        return responseBuilder(200,assistance);
    }

    public Response replaceRequest(Request request) throws Exception{
        String id=extractId(request);
        String body=request.getBody();
        assistanceService.replaceAssistanceRequest(id,revertToJson(body,Assistance.class));
        return responseBuilder(200,"Assistance request replaced successfully");
    }

    public Response updateRequest(Request request) throws Exception{
        String id=extractId(request);
        String body=request.getBody();
        assistanceService.updateAssistanceRequest(id,body);
        return responseBuilder(200,"Assistance request updated successfully");
    }

    public Response cancelRequest(Request request) throws Exception{
        String id=extractId(request);
        assistanceService.deleteAssistanceRequest(id);
        return responseBuilder(200,"Assistance request cancelled successfully");
    }
}
