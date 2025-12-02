# User Client UI (Executive Dashboard)

`CommodityLiveGUI` is a Swing-based dashboard that renders live commodity prices, charts, and field reports for regular users.

## Frame Composition (`src/com/commoditylive/client/CommodityLiveGUI.java`)
- Header shows the brand and exposes an "Login Admin" button that launches the admin dialog when needed.
- `createLeftPanel()` paints the highlighted commodity card plus an embedded `CommodityChartPanel` fed directly with the selected `CommodityData`.
- `createRightPanel()` hosts the sortable table (`CommodityTableManager`) and a report submission card.

```java
private JPanel createLeftPanel() {
    JPanel panel = UIComponentFactory.createCardPanel();
    selectedCommodityLabel = new JLabel("Pilih Komoditas");
    currentPriceLabel = new JLabel("Rp -");
    priceChangeLabel = new JLabel("0.0%");

    chartPanel = new CommodityChartPanel();
    chartPanel.setBackground(Color.WHITE);
    panel.add(chartPanel, BorderLayout.CENTER);
    return panel;
}
```

## Data Bootstrapping & Selection
- The GUI holds a single `MarketDataModel` instance, sets it with the latest server list, and keeps track of the selected commodity id so tables and charts stay in sync.
- `updateChartAndDetails()` refreshes labels, colors, and forwards the newest commodity snapshot to the chart component.

```java
private final MarketDataModel dataModel = new MarketDataModel();
private MarketDataModel.CommodityData selectedCommodity;

private void updateChartAndDetails() {
    if (selectedCommodity == null) return;
    selectedCommodityLabel.setText(selectedCommodity.name);
    currentPriceLabel.setText(String.format("Rp %,d", selectedCommodity.price));
    chartPanel.setCommodityData(selectedCommodity);
    updateTimeLabel.setText("Data diperbarui: " + getCurrentDateTime());
}
```

## Socket Lifecycle & Message Handling
- `connectToServer()` runs on a background thread, opens `ObjectOutputStream`/`ObjectInputStream`, and immediately requests the commodity list.
- Broadcasted `PRICE_UPDATE` messages only signal that the client should re-fetch the full list to ensure consistency.

```java
private void connectToServer() {
    new Thread(() -> {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());
        connected = true;
        sendRequest(new Message(Message.Type.REQUEST, Message.Action.GET_COMMODITY_LIST));
        while (connected) {
            Message msg = (Message) input.readObject();
            handleMessage(msg);
        }
    }).start();
}
```

## User Reports
- Text area in the right card lets field officers submit qualitative updates.
- `sendReportToAdmin()` wraps the message into a `SEND_REPORT` request and clears the form once delivered.

```java
private void sendReportToAdmin() {
    if(text.isEmpty() || text.startsWith("Contoh:")) return;
    Message msg = new Message(Message.Type.REQUEST, Message.Action.SEND_REPORT);
    msg.addData("reportText", text);
    if(selectedCommodity != null) msg.addData("commodityName", selectedCommodity.name);
    sendRequest(msg);
}
```

### UX Notes
- Any selection changes in the table propagate via `CommodityTableManager.setSelectionListener(this::onCommoditySelected)`.
- When admins pin a commodity from their dashboard, `selectCommodityFromAdmin()` triggers the same update path, so both user and admin views stay aligned.
