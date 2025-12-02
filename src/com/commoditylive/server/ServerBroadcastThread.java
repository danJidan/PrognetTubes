package com.commoditylive.server;

import com.commoditylive.common.Message;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread khusus untuk handle broadcasting message ke semua client
 * Menggunakan queue untuk menghindari blocking pada thread utama
 */
public class ServerBroadcastThread extends Thread {
    private BlockingQueue<Message> messageQueue;
    private boolean running = false;
    
    /**
     * Constructor
     */
    public ServerBroadcastThread() {
        this.messageQueue = new LinkedBlockingQueue<>();
        this.setName("BroadcastThread");
        this.setDaemon(true);
    }
    
    @Override
    public void run() {
        running = true;
        System.out.println("Broadcast thread started");
        
        while (running) {
            try {
                // Ambil message dari queue (blocking)
                Message message = messageQueue.take();
                
                if (message != null) {
                    // Broadcast ke semua client
                    broadcastToAll(message);
                }
                
            } catch (InterruptedException e) {
                if (running) {
                    System.err.println("Broadcast thread interrupted");
                }
                break;
            }
        }
        
        System.out.println("Broadcast thread stopped");
    }
    
    /**
     * Queue message untuk di-broadcast
     * Method ini non-blocking
     */
    public void queueMessage(Message message) {
        if (message == null) {
            System.err.println("Cannot queue null message");
            return;
        }
        
        try {
            messageQueue.put(message);
            System.out.println("Message queued for broadcast: " + message.getAction() + " (Queue size: " + messageQueue.size() + ")");
        } catch (InterruptedException e) {
            System.err.println("Error queueing message: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Broadcast message ke semua client yang terhubung
     */
    private void broadcastToAll(Message message) {
        List<ClientHandler> clients = ServerMain.getClients();
        
        if (clients.isEmpty()) {
            System.out.println("No clients to broadcast to");
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        System.out.println("Broadcasting " + message.getAction() + " to " + clients.size() + " client(s)");
        
        // Kirim ke setiap client
        for (ClientHandler client : clients) {
            try {
                if (client.isConnected()) {
                    client.sendMessage(message);
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                System.err.println("Error broadcasting to client " + client.getClientId() + ": " + e.getMessage());
                failCount++;
            }
        }
        
        System.out.println("Broadcast completed: " + successCount + " success, " + failCount + " failed");
    }
    
    /**
     * Stop broadcast thread
     */
    public void stopBroadcast() {
        running = false;
        this.interrupt();
        
        // Clear remaining messages in queue
        int remainingMessages = messageQueue.size();
        if (remainingMessages > 0) {
            System.out.println("Clearing " + remainingMessages + " remaining messages in broadcast queue");
            messageQueue.clear();
        }
    }
}

