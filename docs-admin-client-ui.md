# Admin Client UI (Control Panel)

`CommodityLiveAdminUI` is launched from the main dashboard and gives privileged users CRUD access to commodities plus a live inbox for field reports.

## Authentication & Entry Point (`src/com/commoditylive/client/CommodityLiveAdminUI.java`)
- `showLoginDialog()` displays a modal form with styled username/password fields and validates against the default `admin / admin123` credentials before opening the dashboard.

```java
void showLoginDialog() {
    JDialog loginDialog = new JDialog(parent, "Admin Login", true);
    JTextField userField = new JTextField();
    JPasswordField passField = new JPasswordField();
    JButton loginBtn = new JButton("LOGIN");
    loginBtn.addActionListener(e -> {
        if(userField.getText().equals("admin") && new String(passField.getPassword()).equals("admin123")) {
            loginDialog.dispose();
            showDashboard(userField.getText());
        } else {
            JOptionPane.showMessageDialog(loginDialog, "Wrong credentials!");
        }
    });
}
```

## Dashboard Layout
- `showDashboard()` renders a 900×600 dialog with two tabs:
  - **Manajemen Komoditas** — form + table for CRUD.
  - **Pesan & Laporan** — inbox of user-submitted reports.
- Both tabs reuse styles from `UIComponentFactory` to stay consistent with the main GUI.

## Commodity Management Tab
- Left column is a form card with fields for name, category, and price plus three primary buttons that call `handleAddCommodity()`, `handleUpdateCommodity()`, and `handleDeleteCommodity()`.
- Right column is driven by `AdminCommodityTableManager`, which exposes a selection listener so form fields are auto-populated when an existing commodity is chosen.

```java
commodityTableManager = new AdminCommodityTableManager();
commodityTableManager.setSelectionListener(this::populateForm);

JButton addBtn = new JButton("TAMBAH BARU");
addBtn.addActionListener(e -> handleAddCommodity());

private void handleUpdateCommodity() {
    String id = commodityTableManager.getSelectedCommodityId();
    Message msg = new Message(Message.Type.REQUEST, Message.Action.UPDATE_PRICE);
    msg.addData("id", id);
    msg.addData("newPrice", Integer.parseInt(priceField.getText()));
    parent.sendRequest(msg);
}
```

## Report Inbox Tab
- `AdminReportTableManager` lists all incoming `ReportEntry` objects with a refresh button that triggers `Message.Action.GET_REPORTS` through the parent GUI.

```java
reportTableManager = new AdminReportTableManager();
JButton refreshBtn = new JButton("Refresh Laporan");
refreshBtn.addActionListener(e -> refreshReportTable());

void refreshReportTable() {
    Message msg = new Message(Message.Type.REQUEST, Message.Action.GET_REPORTS);
    parent.sendRequest(msg);
}
```

## Server Interaction Pattern
1. Admin performs an action (add/update/delete).
2. UI packages the input into a `Message` and calls `parent.sendRequest(...)` so the existing socket connection is reused.
3. Server persists the change and broadcasts updates; the regular `CommodityLiveGUI` client refreshes tables and charts accordingly.

### Collaboration with Main GUI
- `refreshCommodityTable()` pulls fresh data from the `MarketDataModel` snapshot the parent already maintains, avoiding duplicate server calls.
- `updateReportTableData()` is invoked by `CommodityLiveGUI.handleMessage()` when the server responds with the latest report list.
