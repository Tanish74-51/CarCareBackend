package CarCareBackend.src.main.java.carcare.config;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;


public class Server{

    public static void main(String[] args) throws IOException {

        HttpServer server=HttpServer.create(new InetSocketAddress(8080),0);

        server.createContext("/vehicles",exchange->{
            System.out.println("Request received!");

            String method=exchange.getRequestMethod();
            String path=exchange.getRequestURI().getPath();
            String body=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);

            System.out.println(method);
            System.out.println(path);
            System.out.println(body);
        });



        server.start();



    }
}
