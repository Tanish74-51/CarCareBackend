package CarCareBackend.src.main.java.carcare;

import CarCareBackend.src.main.java.carcare.config.Database;
import CarCareBackend.src.main.java.carcare.config.Server;
import CarCareBackend.src.main.java.carcare.http.Router;

public class CarCareApplication {

    public static void main(String[] args) throws Exception {
        Database.init();

        String portValue = System.getenv("PORT");
        int port = portValue == null || portValue.isBlank() ? 8080 : Integer.parseInt(portValue.trim());

        Server server = new Server(port, new Router());
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}