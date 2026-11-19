package CarCareBackend;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws IOException {
        Socket clientSocket=new Socket("localhost", 12000);

        DataOutputStream outToServer=new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer=new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


        Scanner inp=new Scanner(System.in);

        System.out.print("Enter vehicle name: ");
        String input=inp.nextLine();

        outToServer.writeBytes(input+"\n");
        outToServer.flush();


        String response=inFromServer.readLine();
        System.out.println("Server: "+response);

        clientSocket.close();
        inp.close();
    }
}

