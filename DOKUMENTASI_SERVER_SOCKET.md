# 📡 Dokumentasi Lengkap Server & Socket Programming
## Commodity Server - CommodityLive System

---

## 📚 Daftar Isi
1. [Pengenalan Arsitektur](#pengenalan-arsitektur)
2. [Konsep Socket Programming](#konsep-socket-programming)
3. [Komponen Utama Server](#komponen-utama-server)
4. [Alur Komunikasi Client-Server](#alur-komunikasi-client-server)
5. [Code Analysis Detail](#code-analysis-detail)
6. [Threading Model](#threading-model)
7. [Protocol & Message Format](#protocol-message-format)
8. [Best Practices & Design Patterns](#best-practices-design-patterns)

---

## 1. Pengenalan Arsitektur

### 🏗️ Arsitektur Multi-Threaded Server
```
┌─────────────────────────────────────────────────────┐
│                   SERVER MAIN                        │
│  - Port: 5000                                        │
│  - ServerSocket (Listener)                           │
└─────────────┬───────────────────────────────────────┘
              │
              ├──► 🔄 Broadcast Thread (Pengirim Pesan)
              │     └─ BlockingQueue untuk message
              │
              ├──► 📈 Price Simulation Thread (Auto Update)
              │     └─ Update harga tiap 10 detik
              │
              └──► 👥 Client Handler Threads (Per Client)
                    ├─ Client 1 Handler
                    ├─ Client 2 Handler
                    └─ Client N Handler
```

### 🎯 Tujuan Desain
- **Concurrency**: Melayani banyak client secara bersamaan
- **Non-Blocking**: Broadcast tidak menghambat request-response
- **Scalability**: Mudah menambah handler untuk action baru
- **Real-time**: Update harga otomatis ke semua client

---

## 2. Konsep Socket Programming

### 🔌 Apa itu Socket?
Socket adalah **endpoint komunikasi** antara dua program melalui jaringan. Seperti "colokan listrik" untuk komunikasi data.

### 📡 TCP/IP Socket Model
```
┌─────────────┐                      ┌─────────────┐
│   CLIENT    │                      │   SERVER    │
│             │                      │             │
│  Socket     │◄────────────────────►│ ServerSocket│
│  (Client)   │   TCP Connection     │  (Listener) │
│             │                      │             │
│ InputStream │◄─────── Data ───────►│ OutputStream│
│OutputStream│◄─────── Data ───────►│ InputStream │
└─────────────┘                      └─────────────┘
```

### 🔐 Kenapa Pakai TCP (bukan UDP)?
- ✅ **Reliable**: Paket data dijamin sampai dan urut
- ✅ **Connection-oriented**: Ada handshake sebelum kirim data
- ✅ **Error checking**: Ada mekanisme retry jika gagal
- ❌ UDP lebih cepat tapi data bisa hilang (tidak cocok untuk transaksi)

---

## 3. Komponen Utama Server

### 📦 Structure Overview
```
src/com/CommodityLive/server/
├── ServerMain.java              # Entry point & socket listener
├── ClientHandler.java           # Per-client request processor
├── ServerBroadcastThread.java   # Message broadcaster
├── PriceSimulationThread.java   # Auto price updater
├── ServerDataManager.java       # In-memory database
└── handler/                     # Request handlers
    ├── GetCommodityListHandler.java
    ├── CreateCommodityHandler.java
    ├── DeleteCommodityHandler.java
    └── ... (7 handlers total)
```

---

## 4. Alur Komunikasi Client-Server

### 🔄 Flow Diagram
```
CLIENT                          SERVER                      DATABASE
  │                               │                            │
  │  1. Connect Socket            │                            │
  ├──────────────────────────────►│                            │
  │                               │                            │
  │  2. Send REQUEST Message      │                            │
  ├──────────────────────────────►│                            │
  │                               │                            │
  │                        3. Process Request                  │
  │                               ├───────────────────────────►│
  │                               │   Query/Update Data        │
  │                               │◄───────────────────────────┤
  │                               │   Return Data              │
  │                               │                            │
  │  4. Receive RESPONSE          │                            │
  │◄──────────────────────────────┤                            │
  │                               │                            │
  │  5. Listen for BROADCAST      │                            │
  │◄──────────────────────────────┤                            │
  │     (Price updates, etc.)     │                            │
```

### 📝 Message Types
1. **REQUEST**: Client → Server (minta data/aksi)
2. **RESPONSE**: Server → Client (jawaban dari request)
3. **BROADCAST**: Server → All Clients (notifikasi real-time)

---

## 5. Code Analysis Detail

### 🚀 ServerMain.java - Jantung Server

```java
package com.CommodityLive.server;

import com.CommodityLive.common.Message;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerMain {
    // ============================================
    // KONSTANTA & VARIABEL GLOBAL
    // ============================================
    
    // Port 5000: Port yang akan di-listen oleh server
    // Kenapa 5000? Bisa port apapun > 1024 (di bawah itu untuk system)
    private static final int PORT = 5000;
    
    // List untuk menyimpan semua client yang terkoneksi
    // CopyOnWriteArrayList: Thread-safe, cocok untuk multiple threads
    // yang baca/tulis secara bersamaan tanpa conflict
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    
    // Thread khusus untuk broadcast message ke semua client
    // Dipisah agar tidak blocking main thread
    private static ServerBroadcastThread broadcastThread = new ServerBroadcastThread();
    
    // ServerSocket: "Pintu masuk" server untuk menerima koneksi
    private ServerSocket serverSocket;
    
    // Flag untuk kontrol server running/stop
    private boolean running = false;
    
    // Thread untuk simulasi harga otomatis
    private PriceSimulationThread simulationThread;
    
    // ============================================
    // CONSTRUCTOR
    // ============================================
    public ServerMain() {}
    
    // ============================================
    // METHOD START - Menjalankan Server
    // ============================================
    public void start() {
        try {
            // LANGKAH 1: Buat ServerSocket dan bind ke PORT 5000
            // ServerSocket akan "mendengarkan" koneksi masuk di port ini
            // Analogi: Membuka toko dan pasang nomor telepon
            serverSocket = new ServerSocket(PORT);
            running = true;
            
            // LANGKAH 2: Start Broadcast Thread
            // Thread ini akan handle pengiriman message ke semua client
            // Tujuan: Agar broadcast tidak blocking accept() loop
            if (!broadcastThread.isAlive()) {
                broadcastThread.start();
            }

            // LANGKAH 3: Start Price Simulation Thread
            // Thread ini akan update harga komoditas tiap 10 detik
            // Tujuan: Simulasi fluktuasi harga real-time
            System.out.println("🚀 Menyalakan Mesin Simulasi Harga...");
            simulationThread = new PriceSimulationThread();
            simulationThread.start();
            
            System.out.println("✅ Server started on port " + PORT);
            System.out.println("   Waiting for clients...");
            
            // LANGKAH 4: Accept Loop - Menerima koneksi client
            // Loop ini akan terus berjalan sampai server di-stop
            while (running) {
                try {
                    // BLOCKING CALL: Menunggu client connect
                    // accept() akan "tidur" sampai ada client baru
                    // Ketika client connect, return Socket object
                    Socket clientSocket = serverSocket.accept();
                    
                    System.out.println("Client baru terhubung: " 
                        + clientSocket.getInetAddress());
                    
                    // LANGKAH 5: Buat ClientHandler untuk client ini
                    // Setiap client punya thread sendiri untuk handle requestnya
                    // Tujuan: Server bisa melayani banyak client bersamaan
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    
                    // LANGKAH 6: Simpan ke list dan start threadnya
                    clients.add(clientHandler);
                    clientHandler.start(); // Start thread untuk client ini
                    
                } catch (IOException e) {
                    // Error saat accept (misalnya socket ditutup)
                    if (running) {
                        System.err.println("Error accept: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // ============================================
    // METHOD STOP - Matikan Server
    // ============================================
    public void stop() {
        running = false;
        
        // Stop semua thread
        if (simulationThread != null) simulationThread.stopSimulation();
        if (broadcastThread != null) broadcastThread.stopBroadcast();
        
        // Tutup semua koneksi client
        for (ClientHandler c : clients) {
            c.closeConnection();
        }
        
        // Tutup server socket
        try { 
            if (serverSocket != null) serverSocket.close(); 
        } catch (IOException e) {}
    }
    
    // ============================================
    // METHOD BROADCAST - Kirim ke Semua Client
    // ============================================
    /**
     * Static method untuk broadcast message dari mana saja
     * Contoh: PriceSimulationThread panggil ini untuk update harga
     * 
     * Alur:
     * 1. Set type jadi BROADCAST
     * 2. Masukkan ke queue di BroadcastThread
     * 3. BroadcastThread yang akan kirim ke semua client
     * 
     * Kenapa pakai queue? Agar caller tidak blocking menunggu kirim
     */
    public static void broadcast(Message msg) {
        // Pastikan type adalah BROADCAST
        if (msg.getType() != Message.Type.BROADCAST) {
            msg.setType(Message.Type.BROADCAST);
        }
        
        // Masukkan ke queue, BroadcastThread yang handle
        broadcastThread.queueMessage(msg);
    }
    
    // ============================================
    // METHOD REMOVE CLIENT
    // ============================================
    /**
     * Dipanggil oleh ClientHandler saat connection closed
     * Untuk cleanup dan remove dari list
     */
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }
    
    // ============================================
    // METHOD GET CLIENTS
    // ============================================
    /**
     * Return copy of clients list (untuk broadcast)
     * Return copy agar thread lain tidak modify list asli
     */
    public static List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }
    
    // ============================================
    // MAIN - Entry Point
    // ============================================
    public static void main(String[] args) {
        new ServerMain().start();
    }
}
```

---

### 👥 ClientHandler.java - Per-Client Thread

```java
package com.CommodityLive.server;

import com.CommodityLive.common.Message;
import com.CommodityLive.server.handler.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.EnumMap;
import java.util.Map;

/**
 * =====================================================
 * CLIENT HANDLER - Thread untuk Handle Satu Client
 * =====================================================
 * 
 * KONSEP: Setiap client yang connect akan punya instance
 *         ClientHandler sendiri yang berjalan di thread terpisah
 * 
 * TUJUAN: 
 * - Isolasi: Request dari client A tidak ganggu client B
 * - Concurrency: Banyak client bisa dilayani bersamaan
 * - Responsiveness: Server tidak freeze saat proses request lama
 */
public class ClientHandler extends Thread {
    // ============================================
    // VARIABEL INSTANCE
    // ============================================
    
    // Socket koneksi dengan client
    private Socket socket;
    
    // Stream untuk baca object dari client
    private ObjectInputStream input;
    
    // Stream untuk kirim object ke client
    private ObjectOutputStream output;
    
    // Flag untuk kontrol thread
    private boolean running = false;
    
    // ID unik untuk client (untuk logging)
    private String clientId;
    
    // Map untuk routing action ke handler
    // EnumMap: Efisien untuk enum sebagai key
    private final Map<Message.Action, RequestHandler> handlers 
        = new EnumMap<>(Message.Action.class);
    
    // ============================================
    // CONSTRUCTOR
    // ============================================
    /**
     * Constructor menerima Socket dari ServerMain
     * Socket ini sudah "connected" ke client
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
        
        // Buat ID unik dari IP dan port client
        this.clientId = socket.getInetAddress().getHostAddress() 
            + ":" + socket.getPort();
        
        // Register semua handler
        registerHandlers();
    }
    
    // ============================================
    // METHOD INITIALIZE STREAMS
    // ============================================
    /**
     * Setup ObjectInputStream dan ObjectOutputStream
     * 
     * PENTING: Output stream HARUS dibuat DULU sebelum input!
     * Kenapa? Karena ObjectInputStream constructor akan blocking
     * menunggu header dari ObjectOutputStream di sisi lain.
     * Jika kedua sisi buat input dulu = DEADLOCK!
     * 
     * Solusi: Server dan client sama-sama buat output dulu
     */
    private boolean initializeStreams() {
        try {
            // LANGKAH 1: Buat output stream dulu
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush(); // Kirim header
            
            // LANGKAH 2: Baru buat input stream
            input = new ObjectInputStream(socket.getInputStream());
            
            return true;
        } catch (IOException e) {
            System.err.println("Error initializing streams for client " 
                + clientId + ": " + e.getMessage());
            return false;
        }
    }
    
    // ============================================
    // METHOD RUN - Thread Execution
    // ============================================
    /**
     * Method ini berjalan di thread terpisah
     * Loop terus-menerus untuk menerima request dari client
     */
    @Override
    public void run() {
        running = true;
        
        // Setup streams
        if (!initializeStreams()) {
            closeConnection();
            return;
        }
        
        System.out.println("Client handler started for: " + clientId);
        
        try {
            // MAIN LOOP: Terus terima dan proses message
            while (running) {
                try {
                    // BLOCKING CALL: Baca object dari stream
                    // Ini akan "tidur" sampai ada data masuk
                    Message request = (Message) input.readObject();
                    
                    if (request != null) {
                        System.out.println("Received from " + clientId 
                            + ": " + request.getAction());
                        
                        // Process request dan kirim response
                        processRequest(request);
                    }
                    
                } catch (EOFException | SocketException e) {
                    // EOFException: Client disconnect normal
                    // SocketException: Connection reset
                    System.out.println("Client disconnected: " + clientId);
                    break;
                    
                } catch (ClassNotFoundException e) {
                    // Object yang diterima bukan Message class
                    System.err.println("Invalid message class from client " 
                        + clientId);
                    sendErrorResponse(null, "Invalid message format");
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error in client handler " + clientId 
                    + ": " + e.getMessage());
            }
        } finally {
            // Cleanup ketika loop selesai
            closeConnection();
        }
    }
    
    // ============================================
    // METHOD PROCESS REQUEST
    // ============================================
    /**
     * Process request dari client
     * 
     * PATTERN: Strategy Pattern
     * - Setiap Action punya Handler sendiri
     * - Handler implement interface RequestHandler
     * - Code lebih modular dan mudah extend
     */
    private void processRequest(Message request) {
        // Buat response message
        Message response = new Message(Message.Type.RESPONSE, 
            request.getAction());
        
        try {
            // VALIDASI 1: Cek type harus REQUEST
            if (request.getType() != Message.Type.REQUEST) {
                response.setSuccess(false);
                response.setErrorMessage(
                    "Invalid message type. Expected REQUEST.");
                sendMessage(response);
                return;
            }
            
            // VALIDASI 2: Cek action tidak null
            Message.Action action = request.getAction();
            if (action == null) {
                response.setSuccess(false);
                response.setErrorMessage("Missing action in request");
                sendMessage(response);
                return;
            }
            
            // VALIDASI 3: Cek handler exists untuk action ini
            RequestHandler handler = handlers.get(action);
            if (handler == null) {
                response.setSuccess(false);
                response.setErrorMessage(
                    "No handler registered for action: " + action);
                sendMessage(response);
                return;
            }
            
            // EXECUTE: Panggil handler untuk proses request
            // Handler akan modify response object
            handler.handle(request, response);
            
            // SEND: Kirim response ke client
            sendMessage(response);
            
        } catch (Exception e) {
            System.err.println("Error processing request: " 
                + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(request.getAction(), 
                "Server error: " + e.getMessage());
        }
    }
    
    // ============================================
    // METHOD REGISTER HANDLERS
    // ============================================
    /**
     * Register semua handler untuk routing
     * Setiap Action di-map ke Handler class
     */
    private void registerHandlers() {
        handlers.put(Message.Action.GET_COMMODITY_LIST, 
            new GetCommodityListHandler());
        handlers.put(Message.Action.FILTER_COMMODITY, 
            new FilterCommodityHandler());
        handlers.put(Message.Action.UPDATE_PRICE, 
            new UpdatePriceHandler());
        handlers.put(Message.Action.CREATE_COMMODITY, 
            new CreateCommodityHandler());
        handlers.put(Message.Action.DELETE_COMMODITY, 
            new DeleteCommodityHandler());
        handlers.put(Message.Action.SEND_REPORT, 
            new SendReportHandler());
        handlers.put(Message.Action.GET_REPORTS, 
            new GetReportsHandler());
    }
    
    // ============================================
    // METHOD SEND MESSAGE
    // ============================================
    /**
     * Kirim message ke client
     * 
     * SYNCHRONIZED: Agar tidak ada 2 thread kirim bersamaan
     * (misal: response thread dan broadcast thread)
     */
    public synchronized void sendMessage(Message message) {
        if (output != null && socket.isConnected()) {
            try {
                // Kirim object melalui stream
                output.writeObject(message);
                output.flush(); // Pastikan terkirim
                
                System.out.println("Sent to " + clientId + ": " 
                    + message.getAction() + " (success=" 
                    + message.isSuccess() + ")");
                    
            } catch (IOException e) {
                System.err.println("Error sending message to " 
                    + clientId + ": " + e.getMessage());
                closeConnection();
            }
        }
    }
    
    // ============================================
    // METHOD SEND ERROR RESPONSE
    // ============================================
    private void sendErrorResponse(Message.Action action, 
            String errorMessage) {
        Message response = new Message(
            Message.Type.RESPONSE, 
            action != null ? action : Message.Action.GET_COMMODITY_LIST
        );
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        sendMessage(response);
    }
    
    // ============================================
    // METHOD CLOSE CONNECTION
    // ============================================
    /**
     * Cleanup saat client disconnect
     * Tutup semua stream dan socket
     */
    public void closeConnection() {
        running = false;
        
        // Close input stream
        try {
            if (input != null) input.close();
        } catch (IOException e) { /* Ignore */ }
        
        // Close output stream
        try {
            if (output != null) output.close();
        } catch (IOException e) { /* Ignore */ }
        
        // Close socket
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) { /* Ignore */ }
        
        // Remove dari list client di server
        ServerMain.removeClient(this);
        
        System.out.println("Connection closed for: " + clientId);
    }
    
    // ============================================
    // GETTER METHODS
    // ============================================
    
    public boolean isConnected() {
        return running && socket != null 
            && socket.isConnected() && !socket.isClosed();
    }
    
    public String getClientId() {
        return clientId;
    }
}
```

---

### 📣 ServerBroadcastThread.java - Broadcaster

```java
package com.CommodityLive.server;

import com.CommodityLive.common.Message;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * =====================================================
 * BROADCAST THREAD - Pengirim Message ke Semua Client
 * =====================================================
 * 
 * KONSEP: Producer-Consumer Pattern
 * - Producer: Thread yang panggil broadcast() 
 *             (contoh: PriceSimulationThread)
 * - Queue: BlockingQueue sebagai buffer
 * - Consumer: BroadcastThread yang ambil dari queue dan kirim
 * 
 * TUJUAN:
 * - Non-blocking: Producer tidak tunggu pengiriman selesai
 * - Ordered: Message dikirim sesuai urutan masuk queue
 * - Thread-safe: BlockingQueue handle synchronization
 */
public class ServerBroadcastThread extends Thread {
    // ============================================
    // VARIABEL INSTANCE
    // ============================================
    
    // Queue untuk menyimpan message yang akan di-broadcast
    // LinkedBlockingQueue: Thread-safe, support blocking operations
    private BlockingQueue<Message> messageQueue;
    
    // Flag untuk kontrol thread
    private boolean running = false;
    
    // ============================================
    // CONSTRUCTOR
    // ============================================
    public ServerBroadcastThread() {
        // Buat queue tanpa batas kapasitas
        this.messageQueue = new LinkedBlockingQueue<>();
        
        // Set nama thread untuk debugging
        this.setName("BroadcastThread");
        
        // Daemon thread: Akan otomatis stop saat main thread selesai
        this.setDaemon(true);
    }
    
    // ============================================
    // METHOD RUN - Thread Execution
    // ============================================
    /**
     * Loop utama broadcast thread
     * Terus-menerus ambil message dari queue dan broadcast
     */
    @Override
    public void run() {
        running = true;
        System.out.println("Broadcast thread started");
        
        while (running) {
            try {
                // BLOCKING CALL: Ambil message dari queue
                // take() akan "tidur" sampai ada message di queue
                // Ini lebih efisien daripada polling (cek terus-menerus)
                Message message = messageQueue.take();
                
                if (message != null) {
                    // Broadcast ke semua client
                    broadcastToAll(message);
                }
                
            } catch (InterruptedException e) {
                // Thread di-interrupt (biasanya saat shutdown)
                if (running) {
                    System.err.println("Broadcast thread interrupted");
                }
                break;
            }
        }
        
        System.out.println("Broadcast thread stopped");
    }
    
    // ============================================
    // METHOD QUEUE MESSAGE
    // ============================================
    /**
     * Masukkan message ke queue untuk di-broadcast
     * Method ini NON-BLOCKING
     * 
     * Dipanggil dari thread lain (misal: PriceSimulationThread)
     */
    public void queueMessage(Message message) {
        if (message == null) {
            System.err.println("Cannot queue null message");
            return;
        }
        
        try {
            // put() akan masukkan message ke queue
            // Jika queue penuh (tidak terjadi karena unlimited), akan blocking
            messageQueue.put(message);
            
            System.out.println("Message queued for broadcast: " 
                + message.getAction() + " (Queue size: " 
                + messageQueue.size() + ")");
                
        } catch (InterruptedException e) {
            System.err.println("Error queueing message: " 
                + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
    
    // ============================================
    // METHOD BROADCAST TO ALL
    // ============================================
    /**
     * Kirim message ke semua client yang terkoneksi
     * 
     * IMPORTANT: 
     * - Kirim ke copy of list, bukan list asli
     * - Agar tidak error saat ada client disconnect di tengah loop
     */
    private void broadcastToAll(Message message) {
        // Ambil snapshot list client saat ini
        List<ClientHandler> clients = ServerMain.getClients();
        
        if (clients.isEmpty()) {
            System.out.println("No clients to broadcast to");
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        System.out.println("Broadcasting " + message.getAction() 
            + " to " + clients.size() + " client(s)");
        
        // Kirim ke setiap client
        for (ClientHandler client : clients) {
            try {
                // Cek client masih connected
                if (client.isConnected()) {
                    client.sendMessage(message);
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                System.err.println("Error broadcasting to client " 
                    + client.getClientId() + ": " + e.getMessage());
                failCount++;
            }
        }
        
        System.out.println("Broadcast completed: " + successCount 
            + " success, " + failCount + " failed");
    }
    
    // ============================================
    // METHOD STOP BROADCAST
    // ============================================
    /**
     * Stop broadcast thread dan clear queue
     */
    public void stopBroadcast() {
        running = false;
        this.interrupt(); // Bangunkan thread jika sedang blocking
        
        // Clear remaining messages
        int remainingMessages = messageQueue.size();
        if (remainingMessages > 0) {
            System.out.println("Clearing " + remainingMessages 
                + " remaining messages in broadcast queue");
            messageQueue.clear();
        }
    }
    
    // ============================================
    // GETTER METHODS
    // ============================================
    
    public int getQueueSize() {
        return messageQueue.size();
    }
    
    public boolean isRunning() {
        return running && this.isAlive();
    }
}
```

---

### 📈 PriceSimulationThread.java - Auto Price Updater

```java
package com.CommodityLive.server;

import com.CommodityLive.common.Message;
import com.CommodityLive.model.MarketDataModel.CommodityData;
import java.util.List;
import java.util.Random;

/**
 * =====================================================
 * PRICE SIMULATION THREAD - Update Harga Otomatis
 * =====================================================
 * 
 * TUJUAN: Simulasi fluktuasi harga komoditas real-time
 * 
 * CARA KERJA:
 * 1. Sleep 10 detik
 * 2. Loop semua komoditas
 * 3. Random harga baru (+/- 500)
 * 4. Update database
 * 5. Broadcast ke semua client
 * 6. Repeat
 */
public class PriceSimulationThread extends Thread {
    // ============================================
    // VARIABEL INSTANCE
    // ============================================
    
    // Flag untuk kontrol thread
    private boolean running = true;
    
    // Random number generator untuk fluktuasi harga
    private final Random random = new Random();

    // ============================================
    // METHOD RUN - Thread Execution
    // ============================================
    @Override
    public void run() {
        System.out.println("📈 SIMULASI HARGA DIMULAI " +
            "(Update tiap 10 detik)...");

        while (running) {
            try {
                // LANGKAH 1: Tunggu 10 detik
                // Sleep tidak makan CPU (beda dengan loop kosong)
                Thread.sleep(10000); 

                System.out.println("⚡ [MESIN] Sedang mengacak harga...");
                
                // LANGKAH 2: Ambil semua komoditas dari database
                List<CommodityData> commodities = 
                    ServerDataManager.getInstance().getAllCommodities();
                
                // Validasi: Jika tidak ada barang, skip
                if (commodities.isEmpty()) {
                    System.out.println("⚠️ [MESIN] Tidak ada barang " +
                        "untuk diupdate!");
                    continue;
                }

                // LANGKAH 3: Loop setiap komoditas
                for (CommodityData c : commodities) {
                    try {
                        // LANGKAH 3.1: Generate fluktuasi random
                        // Range: -5 sampai +5, dikali 100 = -500 s.d +500
                        int fluctuation = (random.nextInt(11) - 5) * 100;
                        
                        // Hindari fluktuasi 0 (harga harus berubah)
                        if (fluctuation == 0) {
                            fluctuation = (random.nextBoolean() ? 100 : -100);
                        }

                        // LANGKAH 3.2: Hitung harga baru
                        int oldPrice = c.price;
                        int newPrice = oldPrice + fluctuation;
                        
                        // Validasi: Harga minimum 1000
                        if (newPrice < 1000) newPrice = 1000;

                        // LANGKAH 3.3: Update ke database
                        ServerDataManager.getInstance()
                            .updatePrice(c.id, newPrice);
                        
                        // LOG: Debug info
                        System.out.println("   -> " + c.name + ": Rp " 
                            + oldPrice + " -> Rp " + newPrice);

                        // LANGKAH 3.4: Broadcast ke semua client
                        // Buat message PRICE_UPDATE
                        Message broadcastMsg = new Message(
                            Message.Type.BROADCAST, 
                            Message.Action.PRICE_UPDATE
                        );
                        broadcastMsg.addData("commodityName", c.name);
                        broadcastMsg.addData("newPrice", newPrice);
                        
                        // Kirim via BroadcastThread
                        ServerMain.broadcast(broadcastMsg);
                        
                        // LANGKAH 3.5: Jeda sebentar antar komoditas
                        // Agar tidak overload network
                        Thread.sleep(50);
                        
                    } catch (Exception e) {
                        System.err.println("❌ Error saat update barang " 
                            + c.name + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                System.out.println("✅ [MESIN] Update Selesai. " +
                    "Menunggu 10 detik lagi...\n");

            } catch (InterruptedException e) {
                // Thread di-interrupt (shutdown)
                running = false;
                System.out.println("Mesin simulasi berhenti.");
                
            } catch (Exception e) {
                System.err.println("❌ CRITICAL ERROR DI MESIN SIMULASI: " 
                    + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ============================================
    // METHOD STOP SIMULATION
    // ============================================
    /**
     * Stop simulation thread
     */
    public void stopSimulation() {
        running = false;
        this.interrupt();
    }
}
```

---

## 6. Threading Model

### 🧵 Thread Architecture
```
┌─────────────────────────────────────────────────────────┐
│                    MAIN THREAD                          │
│  - Start ServerSocket                                   │
│  - Accept connections loop                              │
│  - Create ClientHandler threads                         │
└─────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ BROADCAST    │  │ SIMULATION   │  │ CLIENT       │
│ THREAD       │  │ THREAD       │  │ HANDLER 1    │
│              │  │              │  │              │
│ - Queue      │  │ - Update     │  │ - Request    │
│ - Send to    │  │   prices     │  │ - Response   │
│   all clients│  │ - Broadcast  │  │ - Dedicated  │
└──────────────┘  └──────────────┘  └──────────────┘
                                              │
                                    ┌─────────┴─────────┐
                                    ▼                   ▼
                           ┌──────────────┐  ┌──────────────┐
                           │ CLIENT       │  │ CLIENT       │
                           │ HANDLER 2    │  │ HANDLER N    │
                           └──────────────┘  └──────────────┘
```

### ⚡ Thread Synchronization

#### 1. **CopyOnWriteArrayList** untuk Client List
```java
// Thread-safe tanpa explicit locking
private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();

// Bisa add/remove dari thread manapun
clients.add(handler);      // Thread-safe
clients.remove(handler);   // Thread-safe

// Iterator tidak throw ConcurrentModificationException
for (ClientHandler c : clients) {
    c.sendMessage(msg);
}
```

**Kenapa CopyOnWriteArrayList?**
- ✅ Iterator tidak pernah throw exception
- ✅ Cocok untuk read-heavy operations (broadcast lebih sering dari add/remove)
- ❌ Write operation lambat (copy entire array)

#### 2. **BlockingQueue** untuk Broadcast
```java
private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

// Producer (dari thread manapun)
messageQueue.put(message);  // Non-blocking, thread-safe

// Consumer (BroadcastThread)
Message msg = messageQueue.take();  // Blocking sampai ada data
```

**Kenapa BlockingQueue?**
- ✅ Built-in synchronization
- ✅ Blocking operation (efisien, tidak polling)
- ✅ Producer tidak perlu tunggu consumer

#### 3. **Synchronized** untuk Send Message
```java
public synchronized void sendMessage(Message message) {
    output.writeObject(message);
    output.flush();
}
```

**Kenapa Synchronized?**
- Cegah 2 thread kirim bersamaan ke satu socket
- Contoh race condition tanpa synchronized:
  ```
  Thread 1: writeObject(msg1) ─┐
                                ├─ COLLISION! Data corrupt
  Thread 2: writeObject(msg2) ─┘
  ```

---

## 7. Protocol & Message Format

### 📦 Message Class Structure
```java
public class Message implements Serializable {
    public enum Type { REQUEST, RESPONSE, BROADCAST }
    public enum Action { 
        GET_COMMODITY_LIST, FILTER_COMMODITY,
        UPDATE_PRICE, CREATE_COMMODITY, DELETE_COMMODITY,
        SEND_REPORT, GET_REPORTS, PRICE_UPDATE
    }
    
    private Type type;
    private Action action;
    private Map<String, Object> data;
    private boolean success;
    private String errorMessage;
}
```

### 🔄 Request-Response Pattern
```
CLIENT                                    SERVER
  │                                         │
  │  1. Create REQUEST message              │
  │     action = GET_COMMODITY_LIST         │
  ├────────────────────────────────────────►│
  │                                         │
  │                                2. Process request
  │                                   - Call handler
  │                                   - Query database
  │                                   - Build response
  │                                         │
  │  3. Receive RESPONSE message            │
  │◄────────────────────────────────────────┤
  │     success = true                      │
  │     data = [list of commodities]        │
```

### 📡 Broadcast Pattern
```
SIMULATION THREAD              BROADCAST THREAD              CLIENTS
       │                              │                          │
       │  1. Update price in DB       │                          │
       │  2. Create BROADCAST msg     │                          │
       ├─────────────────────────────►│                          │
       │     queueMessage(msg)         │                          │
       │                              │                          │
       │  3. Return immediately        │  4. take() from queue    │
       │     (non-blocking)            │     (blocking)           │
       │                              │                          │
       │                              │  5. Loop all clients     │
       │                              ├─────────────────────────►│
       │                              │     sendMessage()         │
       │                              ├─────────────────────────►│
       │                              │                          │
```

---

## 8. Best Practices & Design Patterns

### 🎨 Design Patterns Used

#### 1. **Singleton Pattern** - ServerDataManager
```java
public class ServerDataManager {
    private static ServerDataManager instance;
    
    public static synchronized ServerDataManager getInstance() {
        if (instance == null) {
            instance = new ServerDataManager();
        }
        return instance;
    }
    
    private ServerDataManager() { /* Initialize data */ }
}
```
**Tujuan**: Hanya satu instance database di memory

#### 2. **Strategy Pattern** - Request Handlers
```java
public interface RequestHandler {
    void handle(Message request, Message response);
}

// Different strategies untuk different actions
public class CreateCommodityHandler implements RequestHandler { ... }
public class DeleteCommodityHandler implements RequestHandler { ... }

// Client code
RequestHandler handler = handlers.get(action);
handler.handle(request, response);
```
**Tujuan**: Mudah extend tanpa ubah ClientHandler

#### 3. **Producer-Consumer Pattern** - Broadcast
```java
// Producer
public static void broadcast(Message msg) {
    broadcastThread.queueMessage(msg);  // Put in queue
}

// Consumer
public void run() {
    while (running) {
        Message msg = messageQueue.take();  // Take from queue
        broadcastToAll(msg);
    }
}
```
**Tujuan**: Decouple producer dan consumer

#### 4. **Thread-per-Client Pattern**
```java
while (running) {
    Socket clientSocket = serverSocket.accept();
    ClientHandler handler = new ClientHandler(clientSocket);
    handler.start();  // New thread for each client
}
```
**Tujuan**: Isolasi dan concurrency

### ✅ Best Practices

#### 1. **Resource Cleanup**
```java
public void closeConnection() {
    try { if (input != null) input.close(); } catch (IOException e) {}
    try { if (output != null) output.close(); } catch (IOException e) {}
    try { if (socket != null) socket.close(); } catch (IOException e) {}
}
```

#### 2. **Error Handling**
```java
try {
    handler.handle(request, response);
} catch (Exception e) {
    response.setSuccess(false);
    response.setErrorMessage("Server error: " + e.getMessage());
}
```

#### 3. **Thread Safety**
- Gunakan `CopyOnWriteArrayList` untuk shared list
- Gunakan `synchronized` untuk method yang akses shared resource
- Gunakan `BlockingQueue` untuk producer-consumer

#### 4. **Logging**
```java
System.out.println("Client baru terhubung: " + clientSocket.getInetAddress());
System.out.println("Received from " + clientId + ": " + request.getAction());
```

#### 5. **Graceful Shutdown**
```java
public void stop() {
    running = false;
    if (simulationThread != null) simulationThread.stopSimulation();
    if (broadcastThread != null) broadcastThread.stopBroadcast();
    for (ClientHandler c : clients) c.closeConnection();
}
```

---

## 🎯 Kesimpulan

### Arsitektur Server yang Solid:
1. ✅ **Multi-threaded**: Melayani banyak client bersamaan
2. ✅ **Non-blocking**: Broadcast tidak ganggu request-response
3. ✅ **Scalable**: Mudah tambah handler baru
4. ✅ **Real-time**: Update harga otomatis ke semua client
5. ✅ **Thread-safe**: Pakai data structure yang aman
6. ✅ **Modular**: Setiap komponen punya tanggung jawab jelas

### Key Takeaways:
- **Socket = Endpoint komunikasi** antar program
- **ServerSocket = Listener** untuk terima koneksi
- **Thread per client = Concurrency** dan isolasi
- **BlockingQueue = Decoupling** producer-consumer
- **Synchronized = Protection** untuk shared resource
- **ObjectInputStream/OutputStream = Serialization** untuk kirim object

---

## 📚 Referensi

### Java Networking
- `java.net.Socket` - Client-side socket
- `java.net.ServerSocket` - Server-side listener
- `java.io.ObjectInputStream` - Read serialized objects
- `java.io.ObjectOutputStream` - Write serialized objects

### Java Concurrency
- `java.lang.Thread` - Thread creation
- `java.util.concurrent.CopyOnWriteArrayList` - Thread-safe list
- `java.util.concurrent.BlockingQueue` - Producer-consumer queue
- `synchronized` keyword - Mutual exclusion

### Design Patterns
- Singleton Pattern
- Strategy Pattern
- Producer-Consumer Pattern
- Thread-per-Client Pattern

---

**Dibuat oleh**: GitHub Copilot
**Tanggal**: 1 Desember 2025
**Versi**: 1.0

