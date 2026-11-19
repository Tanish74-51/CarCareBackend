package CarCareBackend;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Server{

    private static Path dataFile=Paths.get(System.getProperty("user.dir")).resolve("src").resolve("CarCareBackend").resolve("vehicles");

    static void writeToFile(String data) throws IOException{
        BufferedWriter writer=Files.newBufferedWriter(dataFile,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        writer.write(data+"\n");
        writer.flush();
        writer.close();
    }


    static void writeToClient(DataOutputStream outToClient,String data) throws IOException{
        outToClient.writeBytes(data+"\n");
        outToClient.writeBytes("\n");
        outToClient.flush();
    }

    static String reader(BufferedReader reader) throws IOException {
        StringBuilder data=new StringBuilder();
        String line;

        while ((line=reader.readLine())!=null && !line.isEmpty()) {
            data.append(line).append("\n");
        }
        return data.toString();
    }

    public static void main(String[] args) throws IOException {

        System.out.println("Server Started...");
        ServerSocket welcomeSocket=new ServerSocket(12000);


        while(true){
            Socket connectionSocket=welcomeSocket.accept();


            BufferedReader inFromClient=new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient=new DataOutputStream(connectionSocket.getOutputStream());

            String choice=inFromClient.readLine();

            if (choice.equals("1")){
                String input=inFromClient.readLine();

                try {
                    writeToFile(input);
                    writeToClient(outToClient,"Vehicle registered successfully");
                }
                catch (IOException e){
                    writeToClient(outToClient,"ERROR WRITING\nTry again");
                    System.out.println(e.getMessage());
                }
                finally{
                    connectionSocket.close();
                }
            }
            else if (choice.equals("2")){
                BufferedReader fromDataFile=Files.newBufferedReader(dataFile);
                String data=reader(fromDataFile);
                try{
                    writeToClient(outToClient,data);
                }
                catch (IOException e){
                    writeToClient(outToClient,"ERROR READING\nTry again");
                    System.out.println(e.getMessage());
                }
                finally {
                    connectionSocket.close();
                    fromDataFile.close();
                }

            }

        }
    }
}
