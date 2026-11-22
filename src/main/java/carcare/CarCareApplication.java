package CarCareBackend.src.main.java.carcare;

import CarCareBackend.src.main.java.carcare.config.Server;
import CarCareBackend.src.main.java.carcare.http.Router;

import java.io.IOException;

public class CarCareApplication {
    public static void main(String[] args) {
        Router router=new Router();
        try {
            Server server=new Server(8080,router);
            server.start();
        }
        catch(IOException e) {
            System.out.println("Server couldn't get started");
        }
    }
}
