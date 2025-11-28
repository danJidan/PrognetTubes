package com.pasarlive.server;

import com.pasarlive.common.Message;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server utama untuk menangani koneksi client dan broadcast message
 */
public class ServerMain {
    private static final int PORT = 5000;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static ServerBroadcastThread broadcastThread = new ServerBroadcastThread();
    private ServerSocket serverSocket;
    private boolean running = false;
    
    /**
     * Constructor
     */
    public ServerMain() {
        // broadcastThread sudah diinisialisasi sebagai static field
    }
    
    /**
     * Start server
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            
            // Start broadcast thread jika belum running
            if (!broadcastThread.isAlive()) {
                broadcastThread.start();
            }
            
            System.out.println("Server started on port " + PORT);
            System.out.println("Waiting for clients...");
            
            // Accept client connections
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                    
                    // Create client handler
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    clientHandler.start();
                    
                    System.out.println("Total clients connected: " + clients.size());
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port " + PORT);
            e.printStackTrace();
        }
    }
    
    /**
     * Stop server
     */
    public void stop() {
        running = false;
        
        // Stop broadcast thread
        if (broadcastThread != null) {
            broadcastThread.stopBroadcast();
        }
        
        // Close all client connections
        for (ClientHandler client : clients) {
            client.closeConnection();
        }
        clients.clear();
        
        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        System.out.println("Server stopped");
    }
    
    /**
     * Broadcast message ke semua client yang terhubung
     * Method ini static agar bisa dipanggil dari manapun
     */
    public static void broadcast(Message msg) {
        if (msg == null) {
            System.err.println("Cannot broadcast null message");
            return;
        }
        
        if (broadcastThread == null) {
            System.err.println("Broadcast thread not initialized!");
            return;
        }
        
        // Set type ke BROADCAST jika belum
        if (msg.getType() != Message.Type.BROADCAST) {
            msg.setType(Message.Type.BROADCAST);
        }
        
        // Queue message untuk broadcast
        broadcastThread.queueMessage(msg);
        
        System.out.println("Queued broadcast message: " + msg.getAction());
    }
    
    /**
     * Remove client dari list
     */
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client removed. Total clients: " + clients.size());
    }
    
    /**
     * Get semua client yang terhubung
     */
    public static List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }
    
    /**
     * Get jumlah client yang terhubung
     */
    public static int getClientCount() {
        return clients.size();
    }
    
    /**
     * Start admin console untuk monitoring dan broadcast
     */
    private void startAdminConsole() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            
            try {
                Thread.sleep(1000); // Wait for server to fully start
            } catch (InterruptedException e) {
                return;
            }
            
            System.out.println("\n===========================================");
            System.out.println("    ADMIN CONSOLE - Press ENTER to start");
            System.out.println("===========================================");
            
            while (running) {
                System.out.println("\n--- ADMIN MENU ---");
                System.out.println("1. Check Connected Clients");
                System.out.println("2. Broadcast PRICE_UPDATE");
                System.out.println("3. Broadcast Custom Message");
                System.out.println("4. Server Status");
                System.out.println("0. Shutdown Server");
                System.out.print("\nAdmin > ");
                
                String input = scanner.nextLine().trim();
                
                if (input.equals("0")) {
                    System.out.println("Shutting down server...");
                    stop();
                    System.exit(0);
                    break;
                }
                
                try {
                    int choice = Integer.parseInt(input);
                    
                    switch (choice) {
                        case 1:
                            showConnectedClients();
                            break;
                        case 2:
                            broadcastPriceUpdate();
                            break;
                        case 3:
                            broadcastCustomMessage(scanner);
                            break;
                        case 4:
                            showServerStatus();
                            break;
                        default:
                            System.out.println("Invalid choice!");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input!");
                }
            }
            
            scanner.close();
        }).start();
    }
    
    /**
     * Show connected clients
     */
    private void showConnectedClients() {
        System.out.println("\n--- Connected Clients ---");
        System.out.println("Total: " + clients.size() + " client(s)");
        
        if (!clients.isEmpty()) {
            System.out.println("\nClient List:");
            int i = 1;
            for (ClientHandler client : clients) {
                System.out.println("  " + i + ". " + client.getClientId() + 
                                 " (Connected: " + client.isConnected() + ")");
                i++;
            }
        } else {
            System.out.println("No clients connected.");
        }
    }
    
    /**
     * Broadcast price update
     */
    private void broadcastPriceUpdate() {
        System.out.println("\n--- Broadcasting PRICE_UPDATE ---");
        
        Message broadcastMsg = new Message(Message.Type.BROADCAST, Message.Action.PRICE_UPDATE);
        broadcastMsg.addData("commodityId", "COMM-001");
        broadcastMsg.addData("commodityName", "Beras Premium");
        broadcastMsg.addData("oldPrice", 12000);
        broadcastMsg.addData("newPrice", 12500);
        broadcastMsg.addData("updatedBy", "Admin");
        
        broadcast(broadcastMsg);
        
        System.out.println("✓ Broadcast sent to " + clients.size() + " client(s)");
        System.out.println("  - Commodity: Beras Premium");
        System.out.println("  - Old Price: Rp 12,000");
        System.out.println("  - New Price: Rp 12,500");
    }
    
    /**
     * Broadcast custom message
     */
    private void broadcastCustomMessage(Scanner scanner) {
        System.out.print("\nEnter message: ");
        String msg = scanner.nextLine();
        
        Message broadcastMsg = new Message(Message.Type.BROADCAST, Message.Action.PRICE_UPDATE);
        broadcastMsg.addData("customMessage", msg);
        broadcastMsg.addData("timestamp", System.currentTimeMillis());
        
        broadcast(broadcastMsg);
        
        System.out.println("✓ Custom broadcast sent to " + clients.size() + " client(s): " + msg);
    }
    
    /**
     * Show server status
     */
    private void showServerStatus() {
        System.out.println("\n--- Server Status ---");
        System.out.println("Port: " + PORT);
        System.out.println("Running: " + running);
        System.out.println("Connected Clients: " + clients.size());
        System.out.println("Broadcast Thread: " + (broadcastThread.isAlive() ? "Running" : "Stopped"));
        System.out.println("Broadcast Queue Size: " + broadcastThread.getQueueSize());
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        ServerMain server = new ServerMain();
        
        // Add shutdown hook untuk cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));
        
        // Start admin console
        server.startAdminConsole();
        
        // Start server (blocking)
        server.start();
    }
}
