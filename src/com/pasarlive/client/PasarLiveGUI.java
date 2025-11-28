package com.pasarlive.client;

import com.pasarlive.client.ui.CommodityChartPanel;
import com.pasarlive.client.ui.CommodityTableManager;
import com.pasarlive.client.ui.UIComponentFactory;
import com.pasarlive.client.ui.cards.PriceDetailCard;
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
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class PasarLiveGUI extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private boolean connected = false;

    // GUI components
    private CommodityChartPanel chartPanel;
    private JLabel updateTimeLabel;
    private CommodityTableManager commodityTableManager;
    private JLabel selectedCommodityLabel;
    private JLabel currentPriceLabel;
    private JLabel priceChangeLabel;
    private PriceDetailCard yesterdayPriceCard;
    private PriceDetailCard todayPriceCard;
    private PriceDetailCard priceDiffCard;
    private PriceDetailCard percentageCard;
    private JTextArea reportTextArea;
    private PasarLiveAdminUI adminUI;

    // Data model
    private final MarketDataModel dataModel = new MarketDataModel();
    private MarketDataModel.CommodityData selectedCommodity;
    private String selectedCommodityId;

    public PasarLiveGUI() {
        initializeGUI();
        bootstrapFromModel();
        connectToServer();
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

    /**
     * Initialize GUI components
     */
    private void initializeGUI() {
        setTitle("PasarLive - Real-time Mutual Prices");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }
        
        JPanel mainPanel = UIComponentFactory.createWhitePanel(new BorderLayout(10, 10), 15);
        
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createChartPanel());
        splitPane.setRightComponent(createCommodityListPanel());
        splitPane.setDividerLocation(450);
        splitPane.setBorder(null);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        add(mainPanel);
    }
    
    /**
     * Create header panel
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = UIComponentFactory.createWhitePanel(new BorderLayout());
        
        JPanel titlePanel = UIComponentFactory.createWhitePanel(new FlowLayout(FlowLayout.LEFT));
        
        JLabel logoLabel = new JLabel("🏪");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        
        JLabel titleLabel = new JLabel("PasarLive");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(34, 139, 34));
        
        JLabel subtitleLabel = new JLabel("Real-time Mutual Prices");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.GRAY);
        
        titlePanel.add(logoLabel);
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createHorizontalStrut(10));
        titlePanel.add(subtitleLabel);
        
        JButton loginAdminButton = new JButton("🔐 Login Admin");
        UIComponentFactory.applyFlatButtonStyle(
            loginAdminButton,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(52, 152, 219),
            Color.WHITE
        );
        loginAdminButton.setPreferredSize(new Dimension(140, 35));
        loginAdminButton.addActionListener(e -> getAdminUI().showLoginDialog());
        
        JPanel buttonPanel = UIComponentFactory.createWhitePanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(loginAdminButton);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        return headerPanel;
    }
    
    /**
     * Create chart panel (left side)
     */
    private JPanel createChartPanel() {
        JPanel panel = UIComponentFactory.createWhitePanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230)),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        // Title
        JLabel titleLabel = new JLabel("Grafik Harga");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        selectedCommodityLabel = new JLabel("Beras Premium");
        selectedCommodityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        selectedCommodityLabel.setForeground(Color.GRAY);
        
        JPanel titlePanel = UIComponentFactory.createWhitePanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(selectedCommodityLabel, BorderLayout.SOUTH);
        
        // Price info
        JPanel priceInfoPanel = UIComponentFactory.createWhitePanel(new FlowLayout(FlowLayout.RIGHT));
        
        currentPriceLabel = new JLabel("Rp 15,000");
        currentPriceLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        priceChangeLabel = new JLabel("↗ +3.4%");
        priceChangeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        priceChangeLabel.setForeground(new Color(220, 53, 69));
        
        priceInfoPanel.add(currentPriceLabel);
        priceInfoPanel.add(priceChangeLabel);
        
        titlePanel.add(priceInfoPanel, BorderLayout.EAST);
        
        // Chart area
        chartPanel = new CommodityChartPanel(this::getSelectedCommoditySnapshot);
        chartPanel.setPreferredSize(new Dimension(400, 200));
        
        // Price detail panel
        JPanel detailPanel = createPriceDetailPanel();
        
        // Update time
        updateTimeLabel = new JLabel("Terakhir diperbarui: " + getCurrentDateTime());
        updateTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        updateTimeLabel.setForeground(Color.GRAY);
        
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        panel.add(detailPanel, BorderLayout.SOUTH);
        
        JPanel bottomPanel = UIComponentFactory.createWhitePanel(new BorderLayout());
        bottomPanel.add(updateTimeLabel, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.PAGE_END);
        
        return panel;
    }

    /**
     * Create price detail panel
     */
    private JPanel createPriceDetailPanel() {
        JPanel panel = UIComponentFactory.createWhitePanel(new GridLayout(1, 4, 10, 10));
        panel.setBorder(new EmptyBorder(15, 0, 15, 0));
        
        yesterdayPriceCard = new PriceDetailCard("Harga Kemarin", "Rp 14,500", new Color(52, 152, 219));
        todayPriceCard = new PriceDetailCard("Harga Hari Ini", "Rp 15,000", new Color(46, 204, 113));
        priceDiffCard = new PriceDetailCard("Selisih Harga", "+Rp 500", new Color(231, 76, 60));
        percentageCard = new PriceDetailCard("Persentase", "+3.4%", new Color(241, 196, 15));
        
        panel.add(yesterdayPriceCard);
        panel.add(todayPriceCard);
        panel.add(priceDiffCard);
        panel.add(percentageCard);
        
        return panel;
    }
    
    /**
     * Create commodity list panel (right side)
     */
    private JPanel createCommodityListPanel() {
        JPanel panel = UIComponentFactory.createWhitePanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230)),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        // Title
        JLabel titleLabel = new JLabel("Daftar Komoditas");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JPanel titlePanel = UIComponentFactory.createWhitePanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.WEST);
        
        JButton updateButton = new JButton("Update");
        UIComponentFactory.applyFlatButtonStyle(
            updateButton,
            new Font("Segoe UI", Font.PLAIN, 11),
            new Color(52, 152, 219),
            Color.WHITE
        );
        updateButton.addActionListener(e -> requestCommodityList());
        
        JLabel dateLabel = new JLabel("17-01-03");
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dateLabel.setForeground(Color.GRAY);
        
        JPanel updatePanel = UIComponentFactory.createWhitePanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        updatePanel.add(updateButton);
        updatePanel.add(dateLabel);
        
        titlePanel.add(updatePanel, BorderLayout.EAST);
        commodityTableManager = new CommodityTableManager();
        commodityTableManager.setSelectionListener(this::onCommoditySelected);
        JScrollPane scrollPane = commodityTableManager.createScrollPane();
        
        JPanel reportPanel = UIComponentFactory.createWhitePanel(new BorderLayout(8, 8));
        reportPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(10, 0, 0, 0, new Color(245, 245, 245)),
            new EmptyBorder(10, 0, 0, 0)
        ));

        JLabel reportLabel = new JLabel("Laporkan Perubahan Harga");
        reportLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reportLabel.setForeground(new Color(52, 73, 94));
        reportPanel.add(reportLabel, BorderLayout.NORTH);

        reportTextArea = new JTextArea(3, 20);
        reportTextArea.setLineWrap(true);
        reportTextArea.setWrapStyleWord(true);
        reportTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        reportTextArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            new EmptyBorder(6, 8, 6, 8)
        ));
        JScrollPane reportScrollPane = new JScrollPane(reportTextArea);
        reportScrollPane.setBorder(null);
        reportPanel.add(reportScrollPane, BorderLayout.CENTER);

        JButton sendReportButton = new JButton("Kirim ke Admin");
        UIComponentFactory.applyFlatButtonStyle(
            sendReportButton,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(46, 204, 113),
            Color.WHITE
        );
        sendReportButton.setPreferredSize(new Dimension(140, 32));
        sendReportButton.addActionListener(e -> sendReportToAdmin());

        JPanel buttonContainer = UIComponentFactory.createWhitePanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        buttonContainer.add(sendReportButton);
        reportPanel.add(buttonContainer, BorderLayout.SOUTH);
        
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(reportPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    
    
    /**
     * Handle commodity selection from table
     */
    private void onCommoditySelected(String commodityId) {
        updateSelectedCommodity(commodityId, false);
    }

    void selectCommodityFromAdmin(String commodityId) {
        updateSelectedCommodity(commodityId, true);
    }

    private void updateSelectedCommodity(String commodityId, boolean syncUserTableSelection) {
        MarketDataModel.CommodityData commodity = dataModel.getCommodityById(commodityId);
        if (commodity == null) {
            return;
        }
        selectedCommodity = commodity;
        selectedCommodityId = commodityId;
        updateChartAndDetails();
        if (syncUserTableSelection && commodityTableManager != null) {
            commodityTableManager.selectCommodity(commodityId);
        }
        if (adminUI != null) {
            adminUI.repaintChart();
        }
    }
    
    /**
     * Update chart and detail panels based on selected commodity
     */
    private void updateChartAndDetails() {
        if (selectedCommodity == null) return;
        
        // Update commodity name label
        selectedCommodityLabel.setText(selectedCommodity.name);
        
        // Update current price
        currentPriceLabel.setText(String.format("Rp %,d", selectedCommodity.price));
        
        // Update price change with trend indicator
        String changeSymbol = selectedCommodity.change > 0 ? "↗" : (selectedCommodity.change < 0 ? "↘" : "—");
        String changeText = String.format("%s %.1f%%", changeSymbol, Math.abs(selectedCommodity.change));
        priceChangeLabel.setText(changeText);
        
        Color changeColor = selectedCommodity.change > 0 ? new Color(220, 53, 69) : 
                           (selectedCommodity.change < 0 ? new Color(46, 204, 113) : Color.GRAY);
        priceChangeLabel.setForeground(changeColor);
        
        // Update detail cards
        int yesterdayPrice = selectedCommodity.yesterdayPrice;
        int todayPrice = selectedCommodity.price;
        int priceDiff = todayPrice - yesterdayPrice;
        double percentage = selectedCommodity.change;
        
        if (yesterdayPriceCard != null) {
            yesterdayPriceCard.setValue(String.format("Rp %,d", yesterdayPrice));
        }
        if (todayPriceCard != null) {
            todayPriceCard.setValue(String.format("Rp %,d", todayPrice));
        }
        
        String diffSymbol = priceDiff >= 0 ? "+" : "";
        if (priceDiffCard != null) {
            priceDiffCard.setValue(String.format("%sRp %,d", diffSymbol, priceDiff));
        }
        
        String percentSymbol = percentage >= 0 ? "+" : "";
        if (percentageCard != null) {
            percentageCard.setValue(String.format("%s%.1f%%", percentSymbol, percentage));
        }
        
        // Repaint chart
        chartPanel.refreshChart();
        
        // Update timestamp
        updateTimeLabel.setText("Terakhir diperbarui: " + getCurrentDateTime());
    }

    private MarketDataModel.CommodityData getSelectedCommoditySnapshot() {
        return selectedCommodity;
    }
    
    List<MarketDataModel.CommodityData> getSortedCommodities() {
        return dataModel.getSortedCommodities();
    }

    private void refreshUserCommodityTable() {
        List<MarketDataModel.CommodityData> sorted = getSortedCommodities();
        if (commodityTableManager != null) {
            commodityTableManager.refreshRows(sorted, selectedCommodityId);
        }
        if (adminUI != null) {
            adminUI.refreshCommodityTable();
        }
    }

    void addReportEntry(MarketDataModel.ReportEntry entry) {
        dataModel.addReportEntry(entry);
        if (adminUI != null) {
            adminUI.refreshReportTable();
        }
    }

    private PasarLiveAdminUI getAdminUI() {
        if (adminUI == null) {
            adminUI = new PasarLiveAdminUI(this);
        }
        return adminUI;
    }

    MarketDataModel.CommodityData addCommodityFromAdmin(String name, int price, String category) {
        MarketDataModel.CommodityData commodity = dataModel.addCommodity(name, price, category);
        selectedCommodity = commodity;
        selectedCommodityId = commodity.id;
        refreshUserCommodityTable();
        updateChartAndDetails();
        return commodity;
    }

    MarketDataModel.CommodityData updateCommodityFromAdmin(String commodityId, String name, int price, String category) {
        MarketDataModel.CommodityData commodity = dataModel.updateCommodity(commodityId, name, price, category);
        if (commodity == null) {
            return null;
        }
        selectedCommodityId = commodityId;
        if (selectedCommodity != null && selectedCommodity.id.equals(commodityId)) {
            selectedCommodity = commodity;
        }
        refreshUserCommodityTable();
        updateChartAndDetails();
        return commodity;
    }

    MarketDataModel.CommodityData deleteCommodityFromAdmin(String commodityId) {
        MarketDataModel.CommodityData removed = dataModel.deleteCommodity(commodityId);
        if (removed != null) {
            if (commodityId.equals(selectedCommodityId)) {
                MarketDataModel.CommodityData fallback = dataModel.getDefaultCommodity();
                selectedCommodity = fallback;
                selectedCommodityId = fallback != null ? fallback.id : null;
            }
            refreshUserCommodityTable();
            updateChartAndDetails();
        }
        return removed;
    }

    MarketDataModel.CommodityData getCommodityById(String commodityId) {
        return dataModel.getCommodityById(commodityId);
    }

    String getSelectedCommodityId() {
        return selectedCommodityId;
    }

    List<MarketDataModel.ReportEntry> getReportsSnapshot() {
        return dataModel.getReportsSnapshot();
    }

    boolean updateReportReadState(int index, boolean read) {
        return dataModel.updateReportReadState(index, read);
    }
    
    /**
     * Connect to server
     */
    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(socket.getInputStream());
                connected = true;
                
                System.out.println("Connected to server!");
                
                // Start receiving messages
                receiveMessages();
                
            } catch (IOException e) {
                System.err.println("Connection failed: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, 
                        "Tidak dapat terhubung ke server.\nPastikan server sedang berjalan!", 
                        "Connection Error", 
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    /**
     * Receive messages from server
     */
    private void receiveMessages() {
        try {
            while (connected) {
                Message message = (Message) input.readObject();
                if (message != null) {
                    handleMessage(message);
                }
            }
        } catch (Exception e) {
            if (connected) {
                System.err.println("Error receiving message: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle message from server
     */
    private void handleMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            if (message.getType() == Message.Type.BROADCAST &&
                message.getAction() == Message.Action.PRICE_UPDATE) {
                handlePriceUpdate(message);
            }
        });
    }

    /**
     * Handle price update broadcast
     */
    private void handlePriceUpdate(Message message) {
        String commodityName = (String) message.getData("commodityName");
        Object newPriceObj = message.getData("newPrice");
        
        if (commodityName != null && newPriceObj != null) {
            int newPrice = ((Number) newPriceObj).intValue();
            updateTimeLabel.setText("Terakhir diperbarui: " + getCurrentDateTime());
            chartPanel.refreshChart();
            JOptionPane.showMessageDialog(this,
                String.format("📢 Harga %s diupdate!\nHarga baru: Rp %,d", commodityName, newPrice),
                "Price Update",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void requestCommodityList() {
        if (!connected) {
            JOptionPane.showMessageDialog(this,
                "Tidak terhubung ke server!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Message request = new Message(Message.Type.REQUEST, Message.Action.GET_COMMODITY_LIST);
            output.writeObject(request);
            output.flush();
            System.out.println("Requested commodity list");
        } catch (IOException e) {
            System.err.println("Error sending request: " + e.getMessage());
        }
    }
    
    /**
     * Show admin dashboard after successful login
     */

    /**
     * Send textual report to admin
     */
    private void sendReportToAdmin() {
        if (reportTextArea == null) {
            return;
        }
        String reportText = reportTextArea.getText().trim();
        if (reportText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Isi pesan laporan terlebih dahulu.",
                "Validasi",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!connected) {
            JOptionPane.showMessageDialog(this,
                "Tidak terhubung ke server!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Message reportMessage = new Message(Message.Type.REQUEST, Message.Action.SEND_REPORT);
            reportMessage.addData("reportText", reportText);
            reportMessage.addData("clientTime", getCurrentDateTime());
            if (selectedCommodity != null) {
                reportMessage.addData("commodityId", selectedCommodity.id);
                reportMessage.addData("commodityName", selectedCommodity.name);
            }
            output.writeObject(reportMessage);
            output.flush();
            JOptionPane.showMessageDialog(this,
                "Laporan berhasil dikirim ke admin.",
                "Berhasil",
                JOptionPane.INFORMATION_MESSAGE);
            String commodityName = selectedCommodity != null ? selectedCommodity.name : "Umum";
            addReportEntry(new MarketDataModel.ReportEntry(commodityName, reportText, false));
            reportTextArea.setText("");
        } catch (IOException e) {
            System.err.println("Error sending report: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Gagal mengirim laporan.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    
    /**
     * Get current date time
     */
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy 'pukul' HH:mm", new Locale("id", "ID"));
        return sdf.format(new Date());
    }
    
    /**
     * Custom table cell renderer
     */
    // Table uses the default renderer to keep code and dependencies minimal.
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PasarLiveGUI gui = new PasarLiveGUI();
            gui.setVisible(true);
        });
    }
}
