package com.pasarlive.server;

import com.pasarlive.common.Message;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerMain {
    private static final int PORT = 5000;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static ServerBroadcastThread broadcastThread = new ServerBroadcastThread();
    private ServerSocket serverSocket;
    private boolean running = false;
    
    // Mesin Simulasi Harga
    private PriceSimulationThread simulationThread;
    
    public ServerMain() {}
    
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            
            // 1. Jalankan Broadcast (Pengirim Pesan)
            if (!broadcastThread.isAlive()) {
                broadcastThread.start();
            }

            // 2. JALANKAN MESIN SIMULASI (PENTING!)
            System.out.println("🚀 Menyalakan Mesin Simulasi Harga...");
            simulationThread = new PriceSimulationThread();
            simulationThread.start();
            
            System.out.println("✅ Server started on port " + PORT);
            System.out.println("   Waiting for clients...");
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client baru terhubung: " + clientSocket.getInetAddress());
                    
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    clientHandler.start();
                } catch (IOException e) {
                    if (running) System.err.println("Error accept: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void stop() {
        running = false;
        if (simulationThread != null) simulationThread.stopSimulation();
        if (broadcastThread != null) broadcastThread.stopBroadcast();
        for (ClientHandler c : clients) c.closeConnection();
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException e) {}
    }
    
    public static void broadcast(Message msg) {
        if (msg.getType() != Message.Type.BROADCAST) msg.setType(Message.Type.BROADCAST);
        broadcastThread.queueMessage(msg);
    }
    
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }
    
    public static List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }
    
    public static void main(String[] args) {
        new ServerMain().start();
    }
}