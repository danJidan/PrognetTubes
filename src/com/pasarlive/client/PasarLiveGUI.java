package com.pasarlive.client;

import com.pasarlive.client.ui.CommodityChartPanel;
import com.pasarlive.client.ui.CommodityTableManager;
import com.pasarlive.client.ui.UIComponentFactory;
import com.pasarlive.common.Message;
import com.pasarlive.model.MarketDataModel;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class PasarLiveGUI extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private boolean connected = false;

    // Components
    private CommodityChartPanel chartPanel;
    private JLabel updateTimeLabel;
    private CommodityTableManager commodityTableManager;
    private JLabel selectedCommodityLabel;
    private JLabel currentPriceLabel;
    private JLabel priceChangeLabel;
    private JTextArea reportTextArea;
    private PasarLiveAdminUI adminUI;

    // Data
    private final MarketDataModel dataModel = new MarketDataModel();
    private MarketDataModel.CommodityData selectedCommodity;
    private String selectedCommodityId;

    public PasarLiveGUI() {
        initializeGUI();
        // Langsung connect biar data fresh
        connectToServer();
    }

    private void initializeGUI() {
        setTitle("PasarLive - Executive Dashboard");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UIComponentFactory.COLOR_BG_MAIN);
        setLayout(new BorderLayout(15, 15));
        
        add(createHeader(), BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        contentPanel.setBackground(UIComponentFactory.COLOR_BG_MAIN);
        contentPanel.setBorder(new EmptyBorder(0, 15, 15, 15));
        contentPanel.add(createLeftPanel());
        contentPanel.add(createRightPanel());
        add(contentPanel, BorderLayout.CENTER);
        
        updateTimeLabel = new JLabel("Menunggu data server...");
        updateTimeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        updateTimeLabel.setBorder(new EmptyBorder(0, 20, 10, 0));
        add(updateTimeLabel, BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIComponentFactory.COLOR_PRIMARY);
        header.setPreferredSize(new Dimension(100, 60));
        header.setBorder(new EmptyBorder(0, 20, 0, 20));
        JLabel title = new JLabel("PasarLive");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel(" |  Real-time Commodity Market");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(200, 200, 200));
        JPanel titleBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 15));
        titleBlock.setOpaque(false);
        titleBlock.add(title);
        titleBlock.add(subtitle);
        JButton loginBtn = new JButton("Login Admin");
        UIComponentFactory.applyModernButtonStyle(loginBtn, UIComponentFactory.COLOR_ACCENT);
        loginBtn.addActionListener(e -> getAdminUI().showLoginDialog());
        JPanel btnContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        btnContainer.setOpaque(false);
        btnContainer.add(loginBtn);
        header.add(titleBlock, BorderLayout.WEST);
        header.add(btnContainer, BorderLayout.EAST);
        return header;
    }

    private JPanel createLeftPanel() {
        JPanel panel = UIComponentFactory.createCardPanel();
        panel.setLayout(new BorderLayout(10, 10));
        JPanel topInfo = new JPanel(new BorderLayout());
        topInfo.setOpaque(false);
        selectedCommodityLabel = new JLabel("Pilih Komoditas");
        selectedCommodityLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        selectedCommodityLabel.setForeground(UIComponentFactory.COLOR_PRIMARY);
        JPanel pricePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pricePanel.setOpaque(false);
        currentPriceLabel = new JLabel("Rp -");
        currentPriceLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        priceChangeLabel = new JLabel("0.0%");
        priceChangeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        priceChangeLabel.setBorder(new EmptyBorder(5, 10, 0, 0));
        pricePanel.add(currentPriceLabel);
        pricePanel.add(priceChangeLabel);
        topInfo.add(selectedCommodityLabel, BorderLayout.WEST);
        topInfo.add(pricePanel, BorderLayout.EAST);
        
        // UPDATE PENTING: Constructor Kosong
        chartPanel = new CommodityChartPanel();
        chartPanel.setBackground(Color.WHITE);
        panel.add(topInfo, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);
        JPanel tableCard = UIComponentFactory.createCardPanel();
        tableCard.setLayout(new BorderLayout());
        JLabel tableTitle = new JLabel("Daftar Harga Pasar");
        UIComponentFactory.applyHeaderStyle(tableTitle);
        tableTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        commodityTableManager = new CommodityTableManager();
        commodityTableManager.setSelectionListener(this::onCommoditySelected);
        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(commodityTableManager.createScrollPane(), BorderLayout.CENTER);
        JPanel reportCard = UIComponentFactory.createCardPanel();
        reportCard.setLayout(new BorderLayout(10, 10));
        reportCard.setPreferredSize(new Dimension(0, 150));
        JLabel reportLabel = new JLabel("Laporan Lapangan");
        reportLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        reportTextArea = new JTextArea("Contoh: Stok cabai menipis di pasar induk...");
        reportTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        reportTextArea.setForeground(Color.GRAY);
        reportTextArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(5, 5, 5, 5)
        ));
        reportTextArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (reportTextArea.getText().startsWith("Contoh:")) reportTextArea.setText("");
            }
        });
        JButton sendBtn = new JButton("Kirim Laporan");
        UIComponentFactory.applyModernButtonStyle(sendBtn, UIComponentFactory.COLOR_PRIMARY);
        sendBtn.addActionListener(e -> sendReportToAdmin());
        reportCard.add(reportLabel, BorderLayout.NORTH);
        reportCard.add(new JScrollPane(reportTextArea), BorderLayout.CENTER);
        reportCard.add(sendBtn, BorderLayout.SOUTH);
        panel.add(tableCard, BorderLayout.CENTER);
        panel.add(reportCard, BorderLayout.SOUTH);
        return panel;
    }

    private void bootstrapFromModel() {
        selectedCommodity = dataModel.getDefaultCommodity();
        selectedCommodityId = selectedCommodity != null ? selectedCommodity.id : null;
        refreshUserCommodityTable();
        SwingUtilities.invokeLater(() -> {
            if (commodityTableManager != null && selectedCommodityId != null) {
                commodityTableManager.selectCommodity(selectedCommodityId);
            }
            updateChartAndDetails();
        });
    }

    private void onCommoditySelected(String commodityId) {
        this.selectedCommodityId = commodityId;
        MarketDataModel.CommodityData c = dataModel.getCommodityById(commodityId);
        if (c != null) {
            this.selectedCommodity = c;
            updateChartAndDetails();
        }
    }
    
    // --- UPDATE UI: SEKARANG KITA PAKSA SET DATA KE CHART ---
    private void updateChartAndDetails() {
        if (selectedCommodity == null) return;
        
        System.out.println("🎨 REPAINTING CHART: " + selectedCommodity.name + " (" + selectedCommodity.price + ")");
        
        selectedCommodityLabel.setText(selectedCommodity.name);
        currentPriceLabel.setText(String.format("Rp %,d", selectedCommodity.price));
        String symbol = selectedCommodity.change > 0 ? "▲" : (selectedCommodity.change < 0 ? "▼" : "—");
        priceChangeLabel.setText(String.format("%s %.1f%%", symbol, Math.abs(selectedCommodity.change)));
        if (selectedCommodity.change > 0) priceChangeLabel.setForeground(UIComponentFactory.COLOR_SUCCESS);
        else if (selectedCommodity.change < 0) priceChangeLabel.setForeground(UIComponentFactory.COLOR_DANGER);
        else priceChangeLabel.setForeground(Color.GRAY);
        
        // PENTING: INI BAGIAN YANG DIPERBAIKI
        // Kita kirim object data langsung ke chart panel, bukan menyuruh dia cari sendiri
        chartPanel.setCommodityData(selectedCommodity); 
        
        updateTimeLabel.setText("Data diperbarui: " + getCurrentDateTime());
    }
    
    MarketDataModel.CommodityData getSelectedCommoditySnapshot() { return selectedCommodity; }
    String getSelectedCommodityId() { return selectedCommodityId; }
    List<MarketDataModel.CommodityData> getSortedCommodities() { return dataModel.getSortedCommodities(); }
    MarketDataModel.CommodityData getCommodityById(String id) { return dataModel.getCommodityById(id); }
    List<MarketDataModel.ReportEntry> getReportsSnapshot() { return dataModel.getReportsSnapshot(); }
    MarketDataModel.CommodityData addCommodityFromAdmin(String n, int p, String c) { return null; }
    MarketDataModel.CommodityData updateCommodityFromAdmin(String id, String n, int p, String c) { return null; }
    MarketDataModel.CommodityData deleteCommodityFromAdmin(String id) { return null; }
    boolean updateReportReadState(int idx, boolean r) { return dataModel.updateReportReadState(idx, r); }

    private void refreshUserCommodityTable() {
        if (commodityTableManager != null) 
            commodityTableManager.refreshRows(getSortedCommodities(), selectedCommodityId);
        if (adminUI != null) adminUI.refreshCommodityTable();
    }
    
    private String getCurrentDateTime() {
        return new SimpleDateFormat("dd MMM yyyy HH:mm:ss").format(new Date());
    }

    private PasarLiveAdminUI getAdminUI() {
        if (adminUI == null) adminUI = new PasarLiveAdminUI(this);
        return adminUI;
    }

    void selectCommodityFromAdmin(String commodityId) {
        this.selectedCommodityId = commodityId;
        onCommoditySelected(commodityId);
    }

    public void sendRequest(Message message) {
        if (!connected || output == null) return;
        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
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
            } catch (Exception e) {
                System.out.println("Koneksi terputus: " + e.getMessage());
            }
        }).start();
    }

    private void handleMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            
            if (message.getType() == Message.Type.BROADCAST && 
                message.getAction() == Message.Action.PRICE_UPDATE) {
                System.out.println("🔔 Broadcast diterima. Requesting fresh data...");
                sendRequest(new Message(Message.Type.REQUEST, Message.Action.GET_COMMODITY_LIST));
            }
            
            else if (message.getType() == Message.Type.RESPONSE && 
                     message.getAction() == Message.Action.GET_COMMODITY_LIST) {
                
                List<MarketDataModel.CommodityData> serverList = 
                    (List<MarketDataModel.CommodityData>) message.getData("list");
                
                if (serverList != null) {
                    System.out.println("📦 Data Masuk: " + serverList.size() + " barang.");
                    dataModel.setAllCommodities(serverList);
                    
                    // Update Table
                    refreshUserCommodityTable();
                    
                    // FAILSAFE SELECTOR
                    // Jika belum ada yang dipilih, pilih yang pertama
                    if (selectedCommodityId == null && !serverList.isEmpty()) {
                        selectedCommodityId = serverList.get(0).id;
                    }

                    // Update Grafik dengan Data Baru
                    if (selectedCommodityId != null) {
                        for (MarketDataModel.CommodityData c : serverList) {
                            if (c.id.equals(selectedCommodityId)) {
                                this.selectedCommodity = c; // GANTI DATA LAMA
                                updateChartAndDetails();    // GAMBAR ULANG
                                break;
                            }
                        }
                    }
                }
            }
            else if (message.getType() == Message.Type.RESPONSE && 
                     message.getAction() == Message.Action.GET_REPORTS) {
                List<MarketDataModel.ReportEntry> reports = 
                    (List<MarketDataModel.ReportEntry>) message.getData("list");
                if (reports != null && adminUI != null) {
                    adminUI.updateReportTableData(reports);
                }
            }
        });
    }

    private void sendReportToAdmin() {
        String text = reportTextArea.getText();
        if(text.isEmpty() || text.startsWith("Contoh:")) return;
        Message msg = new Message(Message.Type.REQUEST, Message.Action.SEND_REPORT);
        msg.addData("reportText", text);
        if(selectedCommodity != null) msg.addData("commodityName", selectedCommodity.name);
        sendRequest(msg);
        reportTextArea.setText("");
        JOptionPane.showMessageDialog(this, "Laporan terkirim.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PasarLiveGUI().setVisible(true));
    }
}