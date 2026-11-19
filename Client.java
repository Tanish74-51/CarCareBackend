package CarCareBackend;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    static void writeToServer(DataOutputStream writer,String data) throws IOException{
        writer.writeBytes(data+"\n");
        writer.flush();
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
        Socket clientSocket=new Socket("localhost", 12000);

        DataOutputStream outToServer=new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer=new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


        Scanner inp=new Scanner(System.in);

        System.out.println("Register: 1");
        System.out.println("Get vehicles: 2");
        System.out.print("Enter: ");
        String choice=inp.nextLine();

        if (choice.equals("1")){
            System.out.println("Enter vehicle name: ");
            String data=inp.nextLine();
            writeToServer(outToServer,choice);
            writeToServer(outToServer,data);
            String response=inFromServer.readLine();
            System.out.println(response);
        }
        else if (choice.equals("2")){
            writeToServer(outToServer,choice);
            String data=reader(inFromServer);
            System.out.println(data);
        }

        clientSocket.close();
        inp.close();
    }
}

