package CarCareBackend.src.main.java.carcare.http;

public interface Handler {
        Response handle(Request request) throws Exception;
}
