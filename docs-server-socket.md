# Server & Socket Core

This note explains how the CommodityLive TCP server accepts clients, dispatches their requests, and keeps every dashboard in sync.

## Accept Loop (`src/com/commoditylive/server/ServerMain.java`)
- `ServerMain` binds to port `5000`, starts a dedicated broadcast thread, and accepts sockets in a loop.
- Auto price simulation is intentionally disabled so that only admin actions change prices.

```java
private static final int PORT = 5000;
private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
private static ServerBroadcastThread broadcastThread = new ServerBroadcastThread();

public void start() {
    serverSocket = new ServerSocket(PORT);
    if (!broadcastThread.isAlive()) {
        broadcastThread.start();
    }
    while (running) {
        Socket clientSocket = serverSocket.accept();
        ClientHandler clientHandler = new ClientHandler(clientSocket);
        clients.add(clientHandler);
        clientHandler.start();
    }
}
```

## Per-Client Thread (`src/com/commoditylive/server/ClientHandler.java`)
- Each connection is wrapped in a `ClientHandler` thread with its own `ObjectInputStream`/`ObjectOutputStream` pair.
- Requests are routed through an `EnumMap<Message.Action, RequestHandler>` so server features stay modular.

```java
private final Map<Message.Action, RequestHandler> handlers = new EnumMap<>(Message.Action.class);

private boolean initializeStreams() {
    output = new ObjectOutputStream(socket.getOutputStream());
    output.flush();
    input = new ObjectInputStream(socket.getInputStream());
    return true;
}

private void processRequest(Message request) {
    Message response = new Message(Message.Type.RESPONSE, request.getAction());
    RequestHandler handler = handlers.get(request.getAction());
    handler.handle(request, response);
    sendMessage(response);
}
```

### Registered Handlers
`ClientHandler.registerHandlers()` wires up pricing, CRUD, reporting, and filtering actions. New server capabilities only require implementing another `RequestHandler` and adding it to the map.

## Broadcast Queue (`src/com/commoditylive/server/ServerBroadcastThread.java`)
- UI clients receive push updates via a daemon thread that drains a `LinkedBlockingQueue` and forwards `Message.Type.BROADCAST` payloads to all connected handlers.
- `ServerMain.broadcast(Message msg)` ensures every message in the queue is marked as `BROADCAST`.

```java
public class ServerBroadcastThread extends Thread {
    private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

    public void run() {
        while (running) {
            Message message = messageQueue.take();
            broadcastToAll(message);
        }
    }

    private void broadcastToAll(Message message) {
        for (ClientHandler client : ServerMain.getClients()) {
            if (client.isConnected()) {
                client.sendMessage(message);
            }
        }
    }
}
```

### Typical Flow
1. Admin updates a commodity price.
2. Handler persists the change and calls `ServerMain.broadcast()`.
3. Broadcast thread pushes the new data to every `ClientHandler`, which forwards it to the respective UI.
