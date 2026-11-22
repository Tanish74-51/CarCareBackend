package CarCareBackend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        Scanner inp = new Scanner(System.in);

        System.out.println("Register: 1");
        System.out.println("Get vehicles: 2");
        System.out.print("Enter: ");

        String choice = inp.nextLine();

        if (choice.equals("1")) {

            System.out.print("Enter vehicle name: ");
            String data = inp.nextLine();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/vehicles/100"))
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println(response.body());

        } else if (choice.equals("2")) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/vehicles"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println(response.body());
        }

        client.close();
        inp.close();
    }
}

