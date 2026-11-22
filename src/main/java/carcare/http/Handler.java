package CarCareBackend.src.main.java.carcare.http;


import CarCareBackend.src.main.java.carcare.model.Request;
import CarCareBackend.src.main.java.carcare.model.Response;

public interface Handler {
        Response handle(Request request) throws Exception;
}

