package CarCareBackend.src.main.java.carcare.http;

import CarCareBackend.src.main.java.carcare.controller.*;
import CarCareBackend.src.main.java.carcare.model.User;
import CarCareBackend.src.main.java.carcare.util.UnauthorizedException;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Router implements HttpHandler {

    private static final Logger logger = Logger.getLogger(Router.class.getName());
    private static final int MAX_BODY_BYTES = 1_048_576;   // 1 MB
    private static final String TOKEN_HEADER = "X-Token";

    private final List<Route> routes = new ArrayList<>();
    private final UserController userController = new UserController();
    private final MechanicController mechanicController = new MechanicController();
    private final VehicleController vehicleController = new VehicleController();
    private final ServiceCentreController serviceCentreController = new ServiceCentreController();
    private final ServiceVisitController serviceVisitController = new ServiceVisitController();
    private final AssistanceController assistanceController = new AssistanceController();
    private final PingController pingController = new PingController();

    public Router() {
        // NOTE: routes are checked in this order, so fixed paths (like /service-centres/nearby)
        // must be listed BEFORE the {id} version of the same path.
        // addPublicRoute = no login needed, addRoute = the X-Token header is required.

        addPublicRoute("POST", "/users", userController::register);
        addPublicRoute("POST", "/login", userController::login);
        addRoute("POST", "/logout", userController::logout);

        // "me" = the logged-in user, so nobody ever needs to know their own UUID
        addRoute("GET", "/me", userController::getMe);
        addRoute("PUT", "/me", userController::replaceMe);
        addRoute("PATCH", "/me", userController::updateMe);
        addRoute("DELETE", "/me", userController::deleteMe);

        addAdminRoute("GET", "/mechanics", mechanicController::getAllMechanics);
        addAdminRoute("GET", "/mechanics/{id}", mechanicController::getMechanic);
        addAdminRoute("POST", "/mechanics", mechanicController::createMechanic);
        addAdminRoute("PUT", "/mechanics/{id}", mechanicController::replaceMechanic);
        addAdminRoute("PATCH", "/mechanics/{id}", mechanicController::updateMechanic);
        addAdminRoute("DELETE", "/mechanics/{id}", mechanicController::deleteMechanic);

        addRoute("GET", "/vehicles", vehicleController::getMyVehicles);
        addRoute("GET", "/vehicles/{id}", vehicleController::getVehicle);
        addRoute("POST", "/vehicles", vehicleController::createVehicle);
        addRoute("PUT", "/vehicles/{id}", vehicleController::replaceVehicle);
        addRoute("PATCH", "/vehicles/{id}", vehicleController::updateVehicle);
        addRoute("DELETE", "/vehicles/{id}", vehicleController::deleteVehicle);

        addRoute("GET", "/service-centres", serviceCentreController::getAllServiceCentres);
        addRoute("GET", "/service-centres/nearby", serviceCentreController::getNearby);
        addRoute("GET", "/service-centres/{id}", serviceCentreController::getServiceCentre);
        addAdminRoute("POST", "/service-centres", serviceCentreController::createServiceCentre);
        addAdminRoute("PUT", "/service-centres/{id}", serviceCentreController::replaceServiceCentre);
        addAdminRoute("PATCH", "/service-centres/{id}", serviceCentreController::updateServiceCentre);
        addAdminRoute("DELETE", "/service-centres/{id}", serviceCentreController::deleteServiceCentre);

        addRoute("GET", "/service-visits", serviceVisitController::getMyServiceVisits);
        addRoute("GET", "/service-visits/{id}", serviceVisitController::getServiceVisit);
        addRoute("POST", "/service-visits", serviceVisitController::createServiceVisit);
        addRoute("PUT", "/service-visits/{id}", serviceVisitController::replaceServiceVisit);
        addRoute("PATCH", "/service-visits/{id}", serviceVisitController::updateServiceVisit);
        addRoute("DELETE", "/service-visits/{id}", serviceVisitController::deleteServiceVisit);

        addRoute("GET", "/assistance-requests", assistanceController::getMyRequests);
        addRoute("POST", "/assistance-requests", assistanceController::createRequest);
        addRoute("GET", "/assistance-requests/{id}", assistanceController::getRequest);
        addRoute("PUT", "/assistance-requests/{id}", assistanceController::replaceRequest);
        addRoute("PATCH", "/assistance-requests/{id}", assistanceController::updateRequest);
        addRoute("DELETE", "/assistance-requests/{id}", assistanceController::cancelRequest);
        addRoute("GET", "/assistance-requests/{id}/mechanic", assistanceController::getMechanic);
        addRoute("POST", "/assistance-requests/{id}/complete", assistanceController::completeRequest);

        // admin = dispatcher: the only one who moves jobs forward (on the way -> in progress -> completed) and sets costs
        addAdminRoute("GET", "/admin/assistance-requests", assistanceController::adminList);
        addAdminRoute("PATCH", "/admin/assistance-requests/{id}", assistanceController::adminUpdate);
        addAdminRoute("GET", "/admin/service-visits", serviceVisitController::adminList);
        addAdminRoute("PATCH", "/admin/service-visits/{id}", serviceVisitController::adminUpdate);

        addRoute("GET", "/pings", pingController::getMyPings);
        addRoute("POST", "/pings", pingController::createPing);
        addRoute("GET", "/pings/{id}", pingController::getPing);
        addRoute("GET", "/pings/{id}/helper", pingController::getHelper);
        addRoute("PATCH", "/pings/{id}", pingController::updatePing);
        addRoute("POST", "/pings/{id}/complete", pingController::completePing);
        addRoute("DELETE", "/pings/{id}", pingController::cancelPing);
    }

    private enum Access { PUBLIC, USER, ADMIN }

    private record Route(String method, String path, Handler handler, Access access) {}

    // any logged-in user
    public void addRoute(String method, String path, Handler handler) {
        routes.add(new Route(method, path, handler, Access.USER));
    }

    public void addPublicRoute(String method, String path, Handler handler) {
        routes.add(new Route(method, path, handler, Access.PUBLIC));
    }

    // logged in AND role ADMIN
    public void addAdminRoute(String method, String path, Handler handler) {
        routes.add(new Route(method, path, handler, Access.ADMIN));
    }

    // returns the {placeholder} values if the path matches the route, otherwise null
    public Map<String, String> pathMatcher(String routePath, String requestPath) {
        String[] routeSplit = routePath.split("/");
        String[] requestSplit = requestPath.split("/");

        if (routeSplit.length != requestSplit.length) return null;

        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < routeSplit.length; i++) {
            if (routeSplit[i].startsWith("{") && routeSplit[i].endsWith("}")) {
                params.put(routeSplit[i].substring(1, routeSplit[i].length() - 1), requestSplit[i]);
            } else if (!routeSplit[i].equals(requestSplit[i])) {
                return null;
            }
        }
        return params;
    }

    // the X-Token header as a UUID, or null if it is missing or not a UUID
    private UUID parseToken(String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void sendResponse(HttpExchange exchange, Response response) throws IOException {

        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.putAll(response.getHeaders());

        byte[] responseBytes = response.getBody().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(response.getStatusCode(), responseBytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String requestMethod = exchange.getRequestMethod();
        String requestPath = exchange.getRequestURI().getPath();
        String requestUri = exchange.getRequestURI().toString();     // keeps ?query=string
        Headers requestHeaders = exchange.getRequestHeaders();

        byte[] bodyBytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bodyBytes.length > MAX_BODY_BYTES) {
            sendResponse(exchange, Response.error(413, "Request body too large"));
            return;
        }
        String requestBody = new String(bodyBytes, StandardCharsets.UTF_8);

        boolean pathFound = false;      // some route has this path (maybe with another method)
        Response response = null;

        for (Route route : routes) {
            Map<String, String> pathParams = pathMatcher(route.path(), requestPath);
            if (pathParams == null) continue;

            pathFound = true;
            if (!route.method().equals(requestMethod)) continue;

            // protected route: the token must belong to a logged-in user
            UUID token = null;
            UUID userId = null;
            if (route.access() != Access.PUBLIC) {
                token = parseToken(requestHeaders.getFirst(TOKEN_HEADER));
                Sessions.Session session = Sessions.get(token);
                if (session == null) {
                    response = Response.error(401, "Login required: send your token in the " + TOKEN_HEADER + " header");
                    break;
                }
                if (route.access() == Access.ADMIN && !User.ADMIN.equals(session.role())) {
                    response = Response.error(403, "Admin access required");
                    break;
                }
                userId = session.userId();
            }

            try {
                response = route.handler().handle(new Request(requestMethod, requestUri, requestHeaders, requestBody, pathParams, userId, token));
            } catch (IllegalArgumentException e) {
                response = Response.error(400, e.getMessage());          // bad input
            } catch (UnauthorizedException e) {
                response = Response.error(401, e.getMessage());          // wrong email or password
            } catch (SecurityException e) {
                response = Response.error(403, e.getMessage());          // someone else's record
            } catch (NoSuchElementException e) {
                response = Response.error(404, e.getMessage());          // not found
            } catch (IllegalStateException e) {
                response = Response.error(409, e.getMessage());          // not allowed in current state
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error handling " + requestMethod + " " + requestPath, e);
                response = Response.error(500, "Internal server error");
            }
            if (response == null) {
                logger.severe("Handler returned null for " + requestMethod + " " + requestPath);
                response = Response.error(500, "Internal server error");
            }
            break;
        }

        if (response == null) {
            response = pathFound ? Response.error(405, "Method not allowed") : Response.error(404, "Endpoint not found");
        }

        sendResponse(exchange, response);
    }
}
