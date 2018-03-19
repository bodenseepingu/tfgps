package com.gpsview;

//import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCPServer implements Runnable {

    private Socket client;

    private ServerSocket server;
    private final int port;
    private DataOutputStream outStream;
    //private DataInputStream inStream;
    private boolean clientListens = false;

    public TCPServer(int port) {
        this.server = null;
        this.port = port;
        try {
            server = new ServerSocket(port);
        } catch (IOException ex) {
            System.out.println("Port " + port + " could not be bind");
            System.exit(1);
        }
    }

    public void acceptClientConnection() {
        try {
            client = server.accept();
            outStream = new DataOutputStream(client.getOutputStream());
            clientListens = true;
            //inStream = new DataInputStream(client.getInputStream());
        } catch (IOException e) {
            System.out.println("Accept failed at port: " + port);
            System.exit(2);
        }
    }

    public void sendData(byte[] b) {
        try {
            if (clientListens) {
                outStream.write(b);
            }
        } catch (IOException ex) {
            System.out.println("Write to client failed");
            System.exit(3);
        }
    }

    @Override
    public void run() {
        System.out.println("Waiting for client to connect");
        acceptClientConnection();
        System.out.println("Client connected");
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TCPServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
