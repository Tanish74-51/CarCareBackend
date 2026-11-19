package CarCareBackend;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Server{
    static void main(String[] args) throws IOException {

        System.out.println("Server Started");
        ServerSocket welcomeSocket=new ServerSocket(12000);
        Path dataFile=Paths.get(System.getProperty("user.dir")).resolve("src").resolve("CarCareBackend").resolve("vehicles");

        while(true){
            Socket connectionSocket=welcomeSocket.accept();

            BufferedReader inFromClient=new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient=new DataOutputStream(connectionSocket.getOutputStream());

            String data=inFromClient.readLine();
            System.out.println(data);

            try (BufferedWriter writer=Files.newBufferedWriter(dataFile,StandardOpenOption.CREATE,StandardOpenOption.APPEND)) {
                writer.write(data+"\r\n");
                writer.flush();
                outToClient.writeBytes("Vehicle registered\n");
                outToClient.flush();
                System.out.println("Vehicle registered");
            }
            catch (IOException e) {
                outToClient.writeBytes("Error\n");
                outToClient.flush();
                System.out.println(e.getMessage());
            }
            finally {
                outToClient.close();
            }
        }
    }
}
