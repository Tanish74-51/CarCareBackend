package CarCareBackend.src.main.java.carcare.config;

import CarCareBackend.src.main.java.carcare.http.Router;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(8);



    public Server(int port, Router router) throws IOException {

        String limit = System.getenv("MAX_IDLE_CONNECTIONS");
        System.setProperty("sun.net.httpserver.maxIdleConnections", limit == null || limit.isBlank() ? "5000" : limit.trim());

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(executor);
        server.createContext("/", router);
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Server started...");
    }

    public void stop() {
        server.stop(1);
        executor.shutdown();
    }
}
