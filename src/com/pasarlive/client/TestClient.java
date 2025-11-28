package com.pasarlive.client;

import com.pasarlive.common.Message;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Simple test client untuk testing server
 */
public class TestClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private boolean running = false;
    private String clientName;
    
    public TestClient(String clientName) {
        this.clientName = clientName;
    }
    
    /**
     * Connect ke server
     */
    public boolean connect() {
        try {
            System.out.println("[" + clientName + "] Connecting to server...");
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            
            // Output stream dibuat dulu
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            
            // Kemudian input stream
            input = new ObjectInputStream(socket.getInputStream());
            
            running = true;
            
            // Start thread untuk menerima message dari server
            new Thread(this::receiveMessages).start();
            
            System.out.println("[" + clientName + "] Connected to server!");
            return true;
            
        } catch (IOException e) {
            System.err.println("[" + clientName + "] Connection failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Thread untuk menerima message dari server
     */
    private void receiveMessages() {
        try {
            while (running) {
                Message message = (Message) input.readObject();
                
                if (message != null) {
                    handleMessage(message);
                }
            }
        } catch (EOFException | java.net.SocketException e) {
            System.out.println("[" + clientName + "] Disconnected from server");
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                System.err.println("[" + clientName + "] Error receiving message: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle message dari server
     */
    private void handleMessage(Message message) {
        System.out.println("\n[" + clientName + "] <<<< Received " + message.getType() + " - " + message.getAction());
        
        if (message.getType() == Message.Type.RESPONSE) {
            if (message.isSuccess()) {
                System.out.println("  ✓ Success: " + message.getData());
            } else {
                System.out.println("  ✗ Error: " + message.getErrorMessage());
            }
        } else if (message.getType() == Message.Type.BROADCAST) {
            System.out.println("  📢 BROADCAST: " + message.getData());
        }
        
        System.out.print("\n[" + clientName + "] Enter command > ");
    }
    
    /**
     * Kirim request ke server
     */
    public void sendRequest(Message.Action action) {
        try {
            Message request = new Message(Message.Type.REQUEST, action);
            request.addData("clientName", clientName);
            request.addData("timestamp", System.currentTimeMillis());
            
            output.writeObject(request);
            output.flush();
            
            System.out.println("[" + clientName + "] >>>> Sent REQUEST - " + action);
            
        } catch (IOException e) {
            System.err.println("[" + clientName + "] Error sending request: " + e.getMessage());
        }
    }
    
    /**
     * Disconnect dari server
     */
    public void disconnect() {
        running = false;
        
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
            System.out.println("[" + clientName + "] Disconnected");
        } catch (IOException e) {
            System.err.println("[" + clientName + "] Error during disconnect: " + e.getMessage());
        }
    }
    
    /**
     * Main method untuk testing
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("===========================================");
        System.out.println("    TEST CLIENT - COMMODITY SERVER");
        System.out.println("===========================================");
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        
        TestClient client = new TestClient(name);
        
        if (!client.connect()) {
            System.out.println("Failed to connect. Make sure server is running!");
            return;
        }
        
        // Menu loop
        System.out.println("\n--- MENU ---");
        System.out.println("1. GET_COMMODITY_LIST");
        System.out.println("2. FILTER_COMMODITY");
        System.out.println("3. UPDATE_PRICE");
        System.out.println("4. CREATE_COMMODITY");
        System.out.println("5. DELETE_COMMODITY");
        System.out.println("6. SEND_REPORT");
        System.out.println("7. GET_REPORTS");
        System.out.println("0. Exit");
        System.out.println("------------\n");
        
        while (true) {
            System.out.print("[" + name + "] Enter command (0-7) > ");
            String input = scanner.nextLine().trim();
            
            if (input.equals("0")) {
                client.disconnect();
                break;
            }
            
            try {
                int choice = Integer.parseInt(input);
                Message.Action action = null;
                
                switch (choice) {
                    case 1:
                        action = Message.Action.GET_COMMODITY_LIST;
                        break;
                    case 2:
                        action = Message.Action.FILTER_COMMODITY;
                        break;
                    case 3:
                        action = Message.Action.UPDATE_PRICE;
                        break;
                    case 4:
                        action = Message.Action.CREATE_COMMODITY;
                        break;
                    case 5:
                        action = Message.Action.DELETE_COMMODITY;
                        break;
                    case 6:
                        action = Message.Action.SEND_REPORT;
                        break;
                    case 7:
                        action = Message.Action.GET_REPORTS;
                        break;
                    default:
                        System.out.println("Invalid choice!");
                        continue;
                }
                
                if (action != null) {
                    client.sendRequest(action);
                }
                
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
            }
        }
        
        scanner.close();
        System.out.println("Goodbye!");
    }
}
