package com.pasarlive.server;

import com.pasarlive.common.Message;
import com.pasarlive.server.handler.CreateCommodityHandler;
import com.pasarlive.server.handler.DeleteCommodityHandler;
import com.pasarlive.server.handler.FilterCommodityHandler;
import com.pasarlive.server.handler.GetCommodityListHandler;
import com.pasarlive.server.handler.GetReportsHandler;
import com.pasarlive.server.handler.RequestHandler;
import com.pasarlive.server.handler.SendReportHandler;
import com.pasarlive.server.handler.UpdatePriceHandler;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Thread handler untuk setiap client yang terhubung
 * Menangani request dari client dan mengirim response
 */
public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private boolean running = false;
    private String clientId;
    private final Map<Message.Action, RequestHandler> handlers = new EnumMap<>(Message.Action.class);
    
    /**
     * Constructor
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.clientId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        registerHandlers();
    }
    
    /**
     * Initialize streams
     */
    private boolean initializeStreams() {
        try {
            // Output stream harus dibuat SEBELUM input stream
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            
            input = new ObjectInputStream(socket.getInputStream());
            return true;
        } catch (IOException e) {
            System.err.println("Error initializing streams for client " + clientId + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void run() {
        running = true;
        
        if (!initializeStreams()) {
            closeConnection();
            return;
        }
        
        System.out.println("Client handler started for: " + clientId);
        
        try {
            // Loop untuk menerima dan memproses message dari client
            while (running) {
                try {
                    // Baca message dari client
                    Message request = (Message) input.readObject();
                    
                    if (request != null) {
                        System.out.println("Received from " + clientId + ": " + request.getAction());
                        
                        // Process request dan kirim response
                        processRequest(request);
                    }
                } catch (EOFException | SocketException e) {
                    // Client disconnected
                    System.out.println("Client disconnected: " + clientId);
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("Invalid message class from client " + clientId);
                    sendErrorResponse(null, "Invalid message format");
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error in client handler " + clientId + ": " + e.getMessage());
            }
        } finally {
            closeConnection();
        }
    }
    
    /**
     * Process request dari client
     */
    private void processRequest(Message request) {
        Message response = new Message(Message.Type.RESPONSE, request.getAction());
        
        try {
            // Validasi request type
            if (request.getType() != Message.Type.REQUEST) {
                response.setSuccess(false);
                response.setErrorMessage("Invalid message type. Expected REQUEST.");
                sendMessage(response);
                return;
            }
            
            Message.Action action = request.getAction();
            if (action == null) {
                response.setSuccess(false);
                response.setErrorMessage("Missing action in request");
                sendMessage(response);
                return;
            }
            
            RequestHandler handler = handlers.get(action);
            if (handler == null) {
                response.setSuccess(false);
                response.setErrorMessage("No handler registered for action: " + action);
                sendMessage(response);
                return;
            }
            
            handler.handle(request, response);
            sendMessage(response);
            
        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(request.getAction(), "Server error: " + e.getMessage());
        }
    }
    
    private void registerHandlers() {
        handlers.put(Message.Action.GET_COMMODITY_LIST, new GetCommodityListHandler());
        handlers.put(Message.Action.FILTER_COMMODITY, new FilterCommodityHandler());
        handlers.put(Message.Action.UPDATE_PRICE, new UpdatePriceHandler());
        handlers.put(Message.Action.CREATE_COMMODITY, new CreateCommodityHandler());
        handlers.put(Message.Action.DELETE_COMMODITY, new DeleteCommodityHandler());
        handlers.put(Message.Action.SEND_REPORT, new SendReportHandler());
        handlers.put(Message.Action.GET_REPORTS, new GetReportsHandler());
    }
    
    /**
     * Send message ke client
     */
    public synchronized void sendMessage(Message message) {
        if (output != null && socket.isConnected()) {
            try {
                output.writeObject(message);
                output.flush();
                System.out.println("Sent to " + clientId + ": " + message.getAction() + " (success=" + message.isSuccess() + ")");
            } catch (IOException e) {
                System.err.println("Error sending message to " + clientId + ": " + e.getMessage());
                closeConnection();
            }
        }
    }
    
    /**
     * Send error response ke client
     */
    private void sendErrorResponse(Message.Action action, String errorMessage) {
        Message response = new Message(
            Message.Type.RESPONSE, 
            action != null ? action : Message.Action.GET_COMMODITY_LIST
        );
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        sendMessage(response);
    }
    
    /**
     * Close connection dan cleanup
     */
    public void closeConnection() {
        running = false;
        
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        
        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        
        // Remove dari list client di server
        ServerMain.removeClient(this);
        
        System.out.println("Connection closed for: " + clientId);
    }
    
    /**
     * Check apakah connection masih aktif
     */
    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }
    
    /**
     * Get client ID
     */
    public String getClientId() {
        return clientId;
    }
}
