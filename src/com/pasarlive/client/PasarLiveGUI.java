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
import javax.swing.border.LineBorder;

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
    private ModernCommodityList commodityList;
    private JLabel selectedCommodityLabel;
    private JLabel currentPriceLabel;
    private JLabel priceChangeLabel;
    private JLabel categoryBadge;
    private ModernPriceDetailCard yesterdayPriceCard;
    private ModernPriceDetailCard todayPriceCard;
    private ModernPriceDetailCard priceDiffCard;
    private ModernPriceDetailCard percentageCard;
    private PasarLiveAdminUI adminUI;

    // Data model
    private final MarketDataModel dataModel = new MarketDataModel();
    private MarketDataModel.CommodityData selectedCommodity;
    private String selectedCommodityId;

    // Modern color scheme
    private static final Color PRIMARY_GREEN = new Color(16, 185, 129);
    private static final Color BACKGROUND_GRAY = new Color(249, 250, 251);
    private static final Color CARD_WHITE = Color.WHITE;
    private static final Color TEXT_DARK = new Color(17, 24, 39);
    private static final Color TEXT_GRAY = new Color(107, 114, 128);
    private static final Color BORDER_LIGHT = new Color(229, 231, 235);
    private static final Color RED_ACCENT = new Color(239, 68, 68);
    private static final Color GREEN_ACCENT = new Color(34, 197, 94);
    private static final Color BLUE_ACCENT = new Color(59, 130, 246);
    
    // Detail card colors
    private static final Color BLUE_LIGHT_BG = new Color(219, 234, 254);
    private static final Color GREEN_LIGHT_BG = new Color(220, 252, 231);
    private static final Color RED_LIGHT_BG = new Color(254, 226, 226);
    private static final Color YELLOW_LIGHT_BG = new Color(254, 243, 199);

    public PasarLiveGUI() {
        initializeGUI();
        bootstrapFromModel();
        connectToServer();
    }

    private void bootstrapFromModel() {
        selectedCommodity = dataModel.getDefaultCommodity();
        selectedCommodityId = selectedCommodity != null ? selectedCommodity.id : null;
        refreshCommodityList();
        SwingUtilities.invokeLater(() -> {
            if (commodityList != null && selectedCommodityId != null) {
                commodityList.selectCommodity(selectedCommodityId);
            }
            updateChartAndDetails();
        });
    }

    private void initializeGUI() {
        setTitle("PasarLive - Real-time Market Prices");
        setSize(1400, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }
        
        // Main container with background color
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(BACKGROUND_GRAY);
        
        // Header
        mainContainer.add(createModernHeader(), BorderLayout.NORTH);
        
        // Content area
        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBackground(BACKGROUND_GRAY);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Left side - Chart and details
        JPanel leftPanel = createModernChartPanel();
        
        // Right side - Commodity list
        JPanel rightPanel = createModernCommodityListPanel();
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(850);
        splitPane.setBorder(null);
        splitPane.setBackground(BACKGROUND_GRAY);
        
        contentPanel.add(splitPane, BorderLayout.CENTER);
        
        // Bottom section - Report
        contentPanel.add(createReportSection(), BorderLayout.SOUTH);
        
        mainContainer.add(contentPanel, BorderLayout.CENTER);
        add(mainContainer);
    }
    
    private JPanel createModernHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD_WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT),
            new EmptyBorder(15, 25, 15, 25)
        ));
        
        // Left side - Logo and title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setBackground(CARD_WHITE);
        
        // Logo with modern icon background
        JPanel logoContainer = new JPanel();
        logoContainer.setLayout(new BorderLayout());
        logoContainer.setBackground(PRIMARY_GREEN);
        logoContainer.setPreferredSize(new Dimension(40, 40));
        logoContainer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        JLabel logoLabel = new JLabel("📊");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoContainer.add(logoLabel, BorderLayout.CENTER);
        
        // Title and subtitle
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setBackground(CARD_WHITE);
        
        JLabel titleLabel = new JLabel("PasarLive");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(TEXT_DARK);
        
        JLabel subtitleLabel = new JLabel("Real-time Market Prices");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(TEXT_GRAY);
        
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        
        leftPanel.add(logoContainer);
        leftPanel.add(titlePanel);
        
        // Right side - User info and buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setBackground(CARD_WHITE);
        
        // User indicator
        JLabel userIcon = new JLabel("👤");
        userIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        
        JLabel userName = new JLabel("Iam");
        userName.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userName.setForeground(TEXT_DARK);
        
        // Notes icon
        JLabel notesIcon = new JLabel("📋");
        notesIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        notesIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Admin Panel button
        JButton adminButton = new JButton("⚙ Admin Panel");
        adminButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        adminButton.setForeground(Color.WHITE);
        adminButton.setBackground(PRIMARY_GREEN);
        adminButton.setBorder(new EmptyBorder(8, 20, 8, 20));
        adminButton.setFocusPainted(false);
        adminButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        adminButton.addActionListener(e -> getAdminUI().showLoginDialog());
        
        // Share icon
        JLabel shareIcon = new JLabel("↗");
        shareIcon.setFont(new Font("Segoe UI", Font.BOLD, 20));
        shareIcon.setForeground(TEXT_GRAY);
        shareIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        rightPanel.add(userIcon);
        rightPanel.add(userName);
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(notesIcon);
        rightPanel.add(Box.createHorizontalStrut(5));
        rightPanel.add(adminButton);
        rightPanel.add(Box.createHorizontalStrut(5));
        rightPanel.add(shareIcon);
        
        header.add(leftPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createModernChartPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(BACKGROUND_GRAY);
        
        // Top card - Chart
        JPanel chartCard = new JPanel(new BorderLayout(0, 15));
        chartCard.setBackground(CARD_WHITE);
        chartCard.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_LIGHT, 1, true),
            new EmptyBorder(25, 25, 25, 25)
        ));
        
        // Header with title and price info
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(CARD_WHITE);
        
        // Left side - Title and commodity info
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 8));
        titlePanel.setBackground(CARD_WHITE);
        
        JLabel titleLabel = new JLabel("Grafik Harga");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_DARK);
        
        JPanel commodityInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        commodityInfoPanel.setBackground(CARD_WHITE);
        
        selectedCommodityLabel = new JLabel("Beras Premium");
        selectedCommodityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        selectedCommodityLabel.setForeground(TEXT_DARK);
        
        categoryBadge = new JLabel("Bahan Pokok");
        categoryBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        categoryBadge.setForeground(BLUE_ACCENT);
        categoryBadge.setBackground(BLUE_LIGHT_BG);
        categoryBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
        categoryBadge.setOpaque(true);
        
        priceChangeLabel = new JLabel("↗ +3.4%");
        priceChangeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        priceChangeLabel.setForeground(RED_ACCENT);
        
        commodityInfoPanel.add(selectedCommodityLabel);
        commodityInfoPanel.add(categoryBadge);
        commodityInfoPanel.add(priceChangeLabel);
        
        titlePanel.add(titleLabel);
        titlePanel.add(commodityInfoPanel);
        
        // Right side - Current price
        JPanel pricePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        pricePanel.setBackground(CARD_WHITE);
        
        JLabel priceLabel = new JLabel("Harga Saat Ini", SwingConstants.RIGHT);
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        priceLabel.setForeground(TEXT_GRAY);
        
        currentPriceLabel = new JLabel("Rp 15.000", SwingConstants.RIGHT);
        currentPriceLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        currentPriceLabel.setForeground(TEXT_DARK);
        
        JLabel perKgLabel = new JLabel("per kg", SwingConstants.RIGHT);
        perKgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        perKgLabel.setForeground(TEXT_GRAY);
        
        pricePanel.add(priceLabel);
        
        JPanel priceWithUnitPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        priceWithUnitPanel.setBackground(CARD_WHITE);
        priceWithUnitPanel.add(currentPriceLabel);
        priceWithUnitPanel.add(perKgLabel);
        pricePanel.add(priceWithUnitPanel);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(pricePanel, BorderLayout.EAST);
        
        // Chart area
        chartPanel = new CommodityChartPanel(this::getSelectedCommoditySnapshot);
        chartPanel.setPreferredSize(new Dimension(750, 250));
        chartPanel.setBackground(CARD_WHITE);
        
        chartCard.add(headerPanel, BorderLayout.NORTH);
        chartCard.add(chartPanel, BorderLayout.CENTER);
        
        // Bottom card - Detail Perubahan Harga
        JPanel detailCard = new JPanel(new BorderLayout(0, 15));
        detailCard.setBackground(CARD_WHITE);
        detailCard.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_LIGHT, 1, true),
            new EmptyBorder(25, 25, 25, 25)
        ));
        
        JLabel detailTitle = new JLabel("Detail Perubahan Harga");
        detailTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        detailTitle.setForeground(TEXT_DARK);
        
        // Detail cards panel
        JPanel detailsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        detailsPanel.setBackground(CARD_WHITE);
        
        yesterdayPriceCard = new ModernPriceDetailCard("Harga Kemarin", "Rp 14.500", BLUE_LIGHT_BG, BLUE_ACCENT);
        todayPriceCard = new ModernPriceDetailCard("Harga Hari Ini", "Rp 15.000", GREEN_LIGHT_BG, GREEN_ACCENT);
        priceDiffCard = new ModernPriceDetailCard("Selisih Harga", "+Rp 500", RED_LIGHT_BG, RED_ACCENT);
        percentageCard = new ModernPriceDetailCard("Persentase", "+3.4 %", YELLOW_LIGHT_BG, new Color(245, 158, 11));
        
        detailsPanel.add(yesterdayPriceCard);
        detailsPanel.add(todayPriceCard);
        detailsPanel.add(priceDiffCard);
        detailsPanel.add(percentageCard);
        
        // Update time
        updateTimeLabel = new JLabel("Terakhir diperbarui: Selasa, 25 November 2025 pukul 17:18");
        updateTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        updateTimeLabel.setForeground(TEXT_GRAY);
        
        detailCard.add(detailTitle, BorderLayout.NORTH);
        detailCard.add(detailsPanel, BorderLayout.CENTER);
        detailCard.add(updateTimeLabel, BorderLayout.SOUTH);
        
        mainPanel.add(chartCard, BorderLayout.NORTH);
        mainPanel.add(detailCard, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private JPanel createModernCommodityListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(CARD_WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_LIGHT, 1, true),
            new EmptyBorder(25, 25, 25, 25)
        ));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(CARD_WHITE);
        
        JLabel titleLabel = new JLabel("Daftar Komoditas");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_DARK);
        
        JPanel updatePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        updatePanel.setBackground(CARD_WHITE);
        
        JButton updateButton = new JButton("🔄");
        updateButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        updateButton.setBackground(CARD_WHITE);
        updateButton.setBorder(new EmptyBorder(5, 10, 5, 10));
        updateButton.setFocusPainted(false);
        updateButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updateButton.addActionListener(e -> requestCommodityList());
        
        JLabel updateLabel = new JLabel("Update:");
        updateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        updateLabel.setForeground(TEXT_GRAY);
        
        JLabel dateLabel = new JLabel("17.41.03");
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateLabel.setForeground(TEXT_DARK);
        
        updatePanel.add(updateLabel);
        updatePanel.add(dateLabel);
        updatePanel.add(updateButton);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(updatePanel, BorderLayout.EAST);
        
        // Commodity list
        commodityList = new ModernCommodityList();
        commodityList.setSelectionListener(this::onCommoditySelected);
        JScrollPane scrollPane = new JScrollPane(commodityList);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createReportSection() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBackground(CARD_WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_LIGHT, 1, true),
            new EmptyBorder(20, 25, 20, 25)
        ));
        
        // Icon with background
        JPanel iconContainer = new JPanel(new BorderLayout());
        iconContainer.setBackground(new Color(219, 234, 254));
        iconContainer.setPreferredSize(new Dimension(50, 50));
        iconContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel iconLabel = new JLabel("📋");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconContainer.add(iconLabel, BorderLayout.CENTER);
        
        // Text content
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setBackground(CARD_WHITE);
        
        JLabel titleLabel = new JLabel("Ada Keluhan atau Saran?");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(TEXT_DARK);
        
        JLabel descLabel = new JLabel("Klik tombol laporan untuk menyampaikan keluhan, saran, atau informasi seputar pasar kepada admin.");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setForeground(TEXT_GRAY);
        
        textPanel.add(titleLabel);
        textPanel.add(descLabel);
        
        // Button
        JButton reportButton = new JButton("📝 Buat Laporan");
        reportButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        reportButton.setForeground(Color.WHITE);
        reportButton.setBackground(BLUE_ACCENT);
        reportButton.setBorder(new EmptyBorder(10, 25, 10, 25));
        reportButton.setFocusPainted(false);
        reportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        reportButton.addActionListener(e -> showReportDialog());
        
        panel.add(iconContainer, BorderLayout.WEST);
        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(reportButton, BorderLayout.EAST);
        
        return panel;
    }
    
    private void showReportDialog() {
        JDialog dialog = new JDialog(this, "Buat Laporan", true);
        dialog.setLayout(new BorderLayout(15, 15));
        dialog.setSize(500, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(CARD_WHITE);
        
        JLabel titleLabel = new JLabel("Laporkan Perubahan Harga atau Keluhan");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JTextArea reportArea = new JTextArea(8, 40);
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);
        reportArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reportArea.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_LIGHT),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        JScrollPane scrollPane = new JScrollPane(reportArea);
        scrollPane.setBorder(null);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(CARD_WHITE);
        
        JButton cancelButton = new JButton("Batal");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelButton.setBackground(new Color(243, 244, 246));
        cancelButton.setForeground(TEXT_DARK);
        cancelButton.setBorder(new EmptyBorder(8, 20, 8, 20));
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dialog.dispose());
        
        JButton sendButton = new JButton("Kirim ke Admin");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setBackground(GREEN_ACCENT);
        sendButton.setForeground(Color.WHITE);
        sendButton.setBorder(new EmptyBorder(8, 20, 8, 20));
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(e -> {
            String text = reportArea.getText().trim();
            if (!text.isEmpty()) {
                sendReportToAdmin(text);
                dialog.dispose();
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(sendButton);
        
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(contentPanel);
        dialog.setVisible(true);
    }
    
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
        if (syncUserTableSelection && commodityList != null) {
            commodityList.selectCommodity(commodityId);
        }
        if (adminUI != null) {
            adminUI.repaintChart();
        }
    }
    
    private void updateChartAndDetails() {
        if (selectedCommodity == null) return;
        
        // Update commodity info
        selectedCommodityLabel.setText(selectedCommodity.name);
        categoryBadge.setText(selectedCommodity.category);
        
        // Update current price
        currentPriceLabel.setText(String.format("Rp %,d", selectedCommodity.price));
        
        // Update price change
        String changeSymbol = selectedCommodity.change > 0 ? "↗" : (selectedCommodity.change < 0 ? "↘" : "—");
        String changeText = String.format("%s %+.1f%%", changeSymbol, selectedCommodity.change);
        priceChangeLabel.setText(changeText);
        
        Color changeColor = selectedCommodity.change > 0 ? RED_ACCENT : 
                           (selectedCommodity.change < 0 ? GREEN_ACCENT : TEXT_GRAY);
        priceChangeLabel.setForeground(changeColor);
        
        // Update detail cards
        int yesterdayPrice = selectedCommodity.yesterdayPrice;
        int todayPrice = selectedCommodity.price;
        int priceDiff = todayPrice - yesterdayPrice;
        double percentage = selectedCommodity.change;
        
        yesterdayPriceCard.setValue(String.format("Rp %,d", yesterdayPrice));
        todayPriceCard.setValue(String.format("Rp %,d", todayPrice));
        
        String diffSymbol = priceDiff >= 0 ? "+" : "";
        priceDiffCard.setValue(String.format("%sRp %,d", diffSymbol, priceDiff));
        
        String percentSymbol = percentage >= 0 ? "+" : "";
        percentageCard.setValue(String.format("%s%.1f %%", percentSymbol, percentage));
        
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

    private void refreshCommodityList() {
        List<MarketDataModel.CommodityData> sorted = getSortedCommodities();
        if (commodityList != null) {
            commodityList.refreshList(sorted, selectedCommodityId);
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
        refreshCommodityList();
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
        refreshCommodityList();
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
            refreshCommodityList();
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
    
    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(socket.getInputStream());
                connected = true;
                
                System.out.println("Connected to server!");
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
    
    private void handleMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            if (message.getType() == Message.Type.BROADCAST &&
                message.getAction() == Message.Action.PRICE_UPDATE) {
                handlePriceUpdate(message);
            }
        });
    }

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
    
    private void sendReportToAdmin(String reportText) {
        if (reportText == null || reportText.trim().isEmpty()) {
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
            
        } catch (IOException e) {
            System.err.println("Error sending report: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Gagal mengirim laporan.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy 'pukul' HH:mm", new Locale("id", "ID"));
        return sdf.format(new Date());
    }
    
    // Inner class for modern price detail card with enhanced design
    private static class ModernPriceDetailCard extends JPanel {
        private JLabel valueLabel;
        
        public ModernPriceDetailCard(String title, String value, Color bgColor, Color accentColor) {
            setLayout(new BorderLayout(0, 10));
            setBackground(bgColor);
            setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(bgColor.darker(), 1, true),
                new EmptyBorder(18, 18, 18, 18)
            ));
            
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            titleLabel.setForeground(TEXT_GRAY);
            
            valueLabel = new JLabel(value);
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            valueLabel.setForeground(accentColor);
            
            add(titleLabel, BorderLayout.NORTH);
            add(valueLabel, BorderLayout.CENTER);
        }
        
        public void setValue(String value) {
            valueLabel.setText(value);
        }
    }
    
    // Inner class for modern commodity list
    private static class ModernCommodityList extends JPanel {
        private List<CommodityCard> cards = new java.util.ArrayList<>();
        private CommoditySelectionListener listener;
        private String selectedId;
        
        public ModernCommodityList() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(CARD_WHITE);
        }
        
        public void setSelectionListener(CommoditySelectionListener listener) {
            this.listener = listener;
        }
        
        public void refreshList(List<MarketDataModel.CommodityData> commodities, String selectedId) {
            this.selectedId = selectedId;
            removeAll();
            cards.clear();
            
            for (MarketDataModel.CommodityData commodity : commodities) {
                CommodityCard card = new CommodityCard(commodity);
                card.setSelected(commodity.id.equals(selectedId));
                card.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectCommodity(commodity.id);
                        if (listener != null) {
                            listener.onCommoditySelected(commodity.id);
                        }
                    }
                    
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        if (!card.getCommodityId().equals(selectedId)) {
                            card.setHovered(true);
                        }
                    }
                    
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        card.setHovered(false);
                    }
                });
                cards.add(card);
                add(card);
                add(Box.createVerticalStrut(10));
            }
            
            revalidate();
            repaint();
        }
        
        public void selectCommodity(String commodityId) {
            selectedId = commodityId;
            for (CommodityCard card : cards) {
                card.setSelected(card.getCommodityId().equals(commodityId));
            }
        }
        
        private static class CommodityCard extends JPanel {
            private final String commodityId;
            private boolean selected = false;
            private boolean hovered = false;
            
            public CommodityCard(MarketDataModel.CommodityData commodity) {
                this.commodityId = commodity.id;
                setLayout(new BorderLayout(10, 8));
                setBackground(CARD_WHITE);
                setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_LIGHT, 1, true),
                    new EmptyBorder(12, 15, 12, 15)
                ));
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
                
                // Left side - Name and category
                JPanel leftPanel = new JPanel(new GridLayout(2, 1, 0, 4));
                leftPanel.setBackground(CARD_WHITE);
                
                JLabel nameLabel = new JLabel(commodity.name);
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                nameLabel.setForeground(TEXT_DARK);
                
                JLabel categoryLabel = new JLabel(commodity.category);
                categoryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                categoryLabel.setForeground(TEXT_GRAY);
                
                leftPanel.add(nameLabel);
                leftPanel.add(categoryLabel);
                
                // Right side - Price and change
                JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 4));
                rightPanel.setBackground(CARD_WHITE);
                
                JPanel pricePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                pricePanel.setBackground(CARD_WHITE);
                
                JLabel priceLabel = new JLabel(String.format("Rp %,d", commodity.price));
                priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                priceLabel.setForeground(TEXT_DARK);
                
                JLabel unitLabel = new JLabel("/kg");
                unitLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                unitLabel.setForeground(TEXT_GRAY);
                
                pricePanel.add(priceLabel);
                pricePanel.add(unitLabel);
                
                String changeSymbol = commodity.change > 0 ? "↗" : (commodity.change < 0 ? "↘" : "—");
                String changeText = String.format("%s %+.1f%%", changeSymbol, commodity.change);
                JLabel changeLabel = new JLabel(changeText, SwingConstants.RIGHT);
                changeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                Color changeColor = commodity.change > 0 ? RED_ACCENT : 
                                   (commodity.change < 0 ? GREEN_ACCENT : TEXT_GRAY);
                changeLabel.setForeground(changeColor);
                
                rightPanel.add(pricePanel);
                rightPanel.add(changeLabel);
                
                add(leftPanel, BorderLayout.WEST);
                add(rightPanel, BorderLayout.EAST);
            }
            
            public String getCommodityId() {
                return commodityId;
            }
            
            public void setSelected(boolean selected) {
                this.selected = selected;
                updateAppearance();
            }
            
            public void setHovered(boolean hovered) {
                this.hovered = hovered;
                updateAppearance();
            }
            
            private void updateAppearance() {
                if (selected) {
                    setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(PRIMARY_GREEN, 2, true),
                        new EmptyBorder(11, 14, 11, 14)
                    ));
                    setBackground(new Color(236, 253, 245));
                } else if (hovered) {
                    setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER_LIGHT, 1, true),
                        new EmptyBorder(12, 15, 12, 15)
                    ));
                    setBackground(new Color(249, 250, 251));
                } else {
                    setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER_LIGHT, 1, true),
                        new EmptyBorder(12, 15, 12, 15)
                    ));
                    setBackground(CARD_WHITE);
                }
            }
        }
    }
    
    private interface CommoditySelectionListener {
        void onCommoditySelected(String commodityId);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PasarLiveGUI gui = new PasarLiveGUI();
            gui.setVisible(true);
        });
    }
}