package CarCareBackend.src.main.java.carcare.http;

import CarCareBackend.src.main.java.carcare.controller.*;
import CarCareBackend.src.main.java.carcare.model.Request;
import CarCareBackend.src.main.java.carcare.model.Response;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class Router implements HttpHandler {

    List<Route> routes=new ArrayList<>();
    private final MechanicController mechanicController=new MechanicController();
    private final VehicleController vehicleController=new VehicleController();
    private final ServiceCentreController serviceCentreController=new ServiceCentreController();
    private final ServiceVisitController serviceVisitController=new ServiceVisitController();
    private final AssistanceController assistanceController=new AssistanceController();
    private final PingController pingController=new PingController();

    public Router() {
        addRoute("GET", "/mechanics", mechanicController::getAllMechanics);
        addRoute("GET", "/mechanics/{id}", mechanicController::getMechanic);
        addRoute("POST", "/mechanics", mechanicController::createMechanic);
        addRoute("PUT", "/mechanics/{id}", mechanicController::replaceMechanic);
        addRoute("PATCH", "/mechanics/{id}", mechanicController::updateMechanic);
        addRoute("DELETE", "/mechanics/{id}", mechanicController::deleteMechanic);

        addRoute("GET", "/vehicles", vehicleController::getAllVehicles);
        addRoute("GET", "/vehicles/{id}", vehicleController::getVehicle);
        addRoute("POST", "/vehicles", vehicleController::createVehicle);
        addRoute("PUT", "/vehicles/{id}", vehicleController::replaceVehicle);
        addRoute("PATCH", "/vehicles/{id}", vehicleController::updateVehicle);
        addRoute("DELETE", "/vehicles/{id}", vehicleController::deleteVehicle);

        addRoute("GET", "/service-centres", serviceCentreController::getAllServiceCentres);
        addRoute("GET", "/service-centres/{id}", serviceCentreController::getServiceCentre);
        addRoute("POST", "/service-centres", serviceCentreController::createServiceCentre);
        addRoute("PUT", "/service-centres/{id}", serviceCentreController::replaceServiceCentre);
        addRoute("PATCH", "/service-centres/{id}", serviceCentreController::updateServiceCentre);

        addRoute("GET", "/service-visits", serviceVisitController::getAllServiceVisits);
        addRoute("GET", "/service-visits/{id}", serviceVisitController::getServiceVisit);
        addRoute("POST", "/service-visits", serviceVisitController::createServiceVisit);
        addRoute("PUT", "/service-visits/{id}", serviceVisitController::replaceServiceVisit);
        addRoute("PATCH", "/service-visits/{id}", serviceVisitController::updateServiceVisit);

        addRoute("POST", "/assistance-requests", assistanceController::createRequest);
        addRoute("GET", "/assistance-requests/{id}", assistanceController::getRequest);
        addRoute("PUT", "/assistance-requests/{id}", assistanceController::replaceRequest);
        addRoute("PATCH", "/assistance-requests/{id}", assistanceController::updateRequest);
        addRoute("DELETE", "/assistance-requests/{id}", assistanceController::cancelRequest);

        addRoute("POST", "/pings", pingController::createPing);
        addRoute("GET", "/pings/{id}", pingController::getPing);
        addRoute("GET", "/pings/{id}/recipients", pingController::getRecipients);
        addRoute("PUT", "/pings/{id}", pingController::replacePing);
        addRoute("PATCH", "/pings/{id}", pingController::updatePing);
        addRoute("DELETE", "/pings/{id}", pingController::deletePing);



    }

    private record Route(String method,String path,Handler handler){}

    public void addRoute(String method,String path,Handler handler){
        routes.add(new Route(method,path,handler));
    }

    public boolean pathComparer(String path1,String path2){
        String[]  path1Split=path1.split("/");
        String[] path2Split=path2.split("/");

        if (path1Split.length!=path2Split.length) return false;
        int length=path1Split.length;
        for (int i=0;i<length;i++){
            if (path1Split[i].startsWith("{") && path1Split[i].endsWith("}")) continue;
            if (!path1Split[i].equals(path2Split[i])) return false;
        }
        return true;
    }

    public void sendResponse(HttpExchange exchange,Response response) throws IOException{

        Headers responseHeaders=exchange.getResponseHeaders();
        responseHeaders.putAll(response.getHeaders());


        byte[] responseBytes=response.getBody().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(response.getStatusCode(), responseBytes.length);

        try (OutputStream outputStream=exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException{

        String requestMethod=exchange.getRequestMethod();
        String requestPath=exchange.getRequestURI().getPath();
        Headers requestHeaders=exchange.getRequestHeaders();
        String requestBody=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);

        boolean endpointFound=false;
        Response response=null;

        for (Route route:routes){
            String routeMethod=route.method();
            String routePath=route.path();

            if (routeMethod.equals(requestMethod) && pathComparer(routePath,requestPath)){
                try {
                    endpointFound=true;
                    response=route.handler().handle(new Request(requestMethod, requestPath, requestHeaders, requestBody));
                    if (response==null) throw new IllegalStateException("Controller returned null response");

                }
                catch (Exception e){
                    Headers headers=new Headers();
                    headers.set("Content-Type","application/json");
                    response=new Response(500,headers,"{\"error\":\"Internal server error\"}");
                }
                break;
            }
        }
        if (!endpointFound){
            Headers headers = new Headers();
            headers.set("Content-Type", "application/json");
            response=new Response(404,headers,"{\"error\":\"Endpoint not found\"}");
        }

        sendResponse(exchange,response);
    }
}

