package com.pasarlive.client;

import com.pasarlive.client.ui.UIComponentFactory;
import com.pasarlive.client.ui.admin.AdminChartPanel;
import com.pasarlive.client.ui.admin.AdminCommodityTableManager;
import com.pasarlive.client.ui.admin.AdminReportTableManager;
import com.pasarlive.client.ui.cards.StatInfoCard;
import com.pasarlive.model.MarketDataModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

class PasarLiveAdminUI {
    // --- Modern Color Palette ---
    private static final Color BG_COLOR = new Color(245, 247, 250); // Light Gray Background
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(46, 204, 113); // Emerald Green
    private static final Color DANGER_COLOR = new Color(231, 76, 60);   // Alizarin Red
    private static final Color INFO_COLOR = new Color(52, 152, 219);    // Peter River Blue
    private static final Color TEXT_HEADER = new Color(44, 62, 80);     // Dark Blue/Gray
    private static final Color TEXT_BODY = new Color(127, 140, 141);    // Gray
    
    private final PasarLiveGUI parent;
    private JDialog dashboardDialog;
    private AdminCommodityTableManager commodityTableManager;
    private JTextField nameField;
    private JTextField priceField;
    private JComboBox<String> categoryCombo;
    private AdminReportTableManager reportTableManager;
    private JTextArea reportDetailArea;
    private AdminChartPanel chartPreview;
    private List<MarketDataModel.ReportEntry> currentReports = new ArrayList<>();

    PasarLiveAdminUI(PasarLiveGUI parent) {
        this.parent = parent;
    }

    // --- Helper Style Methods ---
    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 220, 220), 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        
        // Hover Effect
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bgColor.darker()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bgColor); }
        });
        return btn;
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_COLOR);
        panel.setBorder(new LineBorder(new Color(230, 230, 230), 1));
        return panel;
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(TEXT_HEADER);
        label.setBorder(new EmptyBorder(0, 0, 15, 0));
        return label;
    }

    // --- Main Logic ---

    void showLoginDialog() {
        JDialog loginDialog = new JDialog(parent, "Admin Login", true);
        loginDialog.setSize(420, 480);
        loginDialog.setLocationRelativeTo(parent);
        loginDialog.setResizable(false);
        loginDialog.setUndecorated(true); // Modern look without OS borders (optional)
        loginDialog.getRootPane().setBorder(new LineBorder(new Color(200, 200, 200), 1));

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(40, 50, 40, 50));

        // Header
        JLabel iconLabel = new JLabel("🔒");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Admin Portal");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(TEXT_HEADER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Silakan masuk untuk melanjutkan");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(TEXT_BODY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Inputs
        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        userLbl.setForeground(TEXT_HEADER);
        userLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField usernameField = new JTextField();
        styleTextField(usernameField);
        usernameField.setMaximumSize(new Dimension(400, 40));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passLbl.setForeground(TEXT_HEADER);
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField passwordField = new JPasswordField();
        styleTextField(passwordField);
        passwordField.setMaximumSize(new Dimension(400, 40));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Buttons
        JButton loginButton = createStyledButton("LOGIN DASHBOARD", PRIMARY_COLOR);
        loginButton.setMaximumSize(new Dimension(400, 45));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton cancelButton = createStyledButton("Batal", new Color(149, 165, 166));
        cancelButton.setMaximumSize(new Dimension(400, 45));
        cancelButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Layout Assembly
        mainPanel.add(iconLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(titleLabel);
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(40));
        mainPanel.add(userLbl);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(usernameField);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(passLbl);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(passwordField);
        mainPanel.add(Box.createVerticalStrut(30));
        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(cancelButton);

        // Actions
        loginButton.addActionListener(e -> {
            String u = usernameField.getText();
            String p = new String(passwordField.getPassword());
            if (u.equals("admin") && p.equals("admin123")) {
                loginDialog.dispose();
                showDashboard(u);
            } else {
                JOptionPane.showMessageDialog(loginDialog, "Kredensial salah.", "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
            }
        });
        passwordField.addActionListener(e -> loginButton.doClick());
        cancelButton.addActionListener(e -> loginDialog.dispose());

        loginDialog.add(mainPanel);
        loginDialog.setVisible(true);
    }

    private void showDashboard(String username) {
        dashboardDialog = new JDialog(parent, "PasarLive Admin", false);
        dashboardDialog.setSize(1100, 750);
        dashboardDialog.setLocationRelativeTo(parent);
        dashboardDialog.setLayout(new BorderLayout());
        
        // --- Custom Header ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        headerPanel.setPreferredSize(new Dimension(0, 60));
        
        JLabel brandLabel = new JLabel("  PasarLive Manager");
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brandLabel.setForeground(PRIMARY_COLOR);
        brandLabel.setIcon(new ImageIcon(new byte[0])); // Placeholder for icon if needed
        
        JLabel userLabel = new JLabel("Halo, " + username + "  ");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userLabel.setForeground(TEXT_BODY);

        headerPanel.add(brandLabel, BorderLayout.WEST);
        headerPanel.add(userLabel, BorderLayout.EAST);
        dashboardDialog.add(headerPanel, BorderLayout.NORTH);

        // --- Tabs Styling ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabbedPane.setBackground(BG_COLOR);
        
        // Add padding around tabs
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(BG_COLOR);
        contentWrapper.setBorder(new EmptyBorder(15, 15, 15, 15));
        contentWrapper.add(tabbedPane, BorderLayout.CENTER);
        
        tabbedPane.addTab("Dashboard & Monitoring", createMonitoringPanel());
        tabbedPane.addTab("Pesan & Laporan", createReportsPanel());
        
        dashboardDialog.add(contentWrapper, BorderLayout.CENTER);

        // Window Cleanup
        dashboardDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) { clearDashboardReferences(); }
        });

        refreshCommodityTable();
        refreshReportTable();
        dashboardDialog.setVisible(true);
    }

    private JPanel createMonitoringPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // 1. Stats Row
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setBackground(BG_COLOR);
        // Assuming StatInfoCard can be styled or we wrap it. 
        // If StatInfoCard is fixed, we just add it. If you can edit it, remove borders there.
        statsPanel.add(new StatInfoCard("Total Komoditas", String.valueOf(parent.getSortedCommodities().size()), "🛒"));
        statsPanel.add(new StatInfoCard("Laporan Masuk", String.valueOf(parent.getReportsSnapshot().size()), "📨"));
        statsPanel.add(new StatInfoCard("Tren Naik", String.valueOf(countPriceIncrease()), "📈"));
        panel.add(statsPanel, BorderLayout.NORTH);

        // 2. Main Content Split (Left: Table/Form, Right: Chart)
        // Using GridBagLayout for flexible sizing
        JPanel contentGrid = new JPanel(new GridBagLayout());
        contentGrid.setBackground(BG_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Left Column: Management (Table + Form) - 65% width
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.65; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 10);
        contentGrid.add(createCommodityManagementSection(), gbc);
        
        // Right Column: Chart - 35% width
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 0.35;
        gbc.insets = new Insets(0, 10, 0, 0);
        
        JPanel chartCard = createCardPanel();
        chartCard.setLayout(new BorderLayout());
        chartCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel chartTitle = createSectionTitle("Analitik Harga");
        chartCard.add(chartTitle, BorderLayout.NORTH);
        
        chartPreview = new AdminChartPanel(this::getChartCommoditySnapshot);
        chartPreview.setBackground(Color.WHITE);
        chartCard.add(chartPreview, BorderLayout.CENTER);
        
        contentGrid.add(chartCard, gbc);

        panel.add(contentGrid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCommodityManagementSection() {
        JPanel container = createCardPanel();
        container.setLayout(new BorderLayout(0, 15));
        container.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Top: Table
        commodityTableManager = new AdminCommodityTableManager();
        commodityTableManager.setSelectionListener(id -> populateCommodityForm());
        JScrollPane tableScroll = commodityTableManager.createScrollPane();
        tableScroll.setBorder(new LineBorder(new Color(230,230,230))); // Clean border
        tableScroll.getViewport().setBackground(Color.WHITE);
        
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBackground(Color.WHITE);
        tableWrapper.add(createSectionTitle("Daftar Komoditas"), BorderLayout.NORTH);
        tableWrapper.add(tableScroll, BorderLayout.CENTER);

        // Bottom: Form
        JPanel formSection = new JPanel(new BorderLayout());
        formSection.setBackground(Color.WHITE);
        formSection.setBorder(new MatteBorder(1, 0, 0, 0, new Color(240, 240, 240))); // Divider line
        
        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setBackground(Color.WHITE);
        formGrid.setBorder(new EmptyBorder(15, 0, 0, 0));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Form Row 1
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1;
        formGrid.add(new JLabel("Nama Item:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.4;
        nameField = new JTextField();
        styleTextField(nameField);
        formGrid.add(nameField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.1;
        formGrid.add(new JLabel("Kategori:"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.4;
        categoryCombo = new JComboBox<>(new String[]{
            "Bahan Pokok", "Sayuran", "Lauk Pauk", "Bumbu", "Buah", "Minuman"
        });
        categoryCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        categoryCombo.setBackground(Color.WHITE);
        formGrid.add(categoryCombo, gbc);

        // Form Row 2
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.1;
        formGrid.add(new JLabel("Harga (Rp):"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.4;
        priceField = new JTextField();
        styleTextField(priceField);
        formGrid.add(priceField, gbc);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(Color.WHITE);
        
        JButton btnAdd = createStyledButton("Tambah Baru", PRIMARY_COLOR);
        btnAdd.addActionListener(e -> handleAddCommodity());
        
        JButton btnUpdate = createStyledButton("Update", INFO_COLOR);
        btnUpdate.addActionListener(e -> handleUpdateCommodity());
        
        JButton btnDel = createStyledButton("Hapus", DANGER_COLOR);
        btnDel.addActionListener(e -> handleDeleteCommodity());
        
        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDel);

        gbc.gridx = 2; gbc.gridy = 1; 
        gbc.gridwidth = 2; 
        gbc.weightx = 0.0;
        formGrid.add(btnPanel, gbc);

        formSection.add(formGrid, BorderLayout.CENTER);
        
        container.add(tableWrapper, BorderLayout.CENTER);
        container.add(formSection, BorderLayout.SOUTH);
        
        return container;
    }

    private JPanel createReportsPanel() {
        JPanel container = createCardPanel();
        container.setLayout(new BorderLayout(0, 20));
        container.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Header with Actions
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.add(createSectionTitle("Kotak Masuk & Laporan"), BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(Color.WHITE);
        
        JButton btnUnread = createStyledButton("Mark Unread", TEXT_BODY);
        btnUnread.addActionListener(e -> setReportReadState(false));
        
        JButton btnRead = createStyledButton("Mark Read", INFO_COLOR);
        btnRead.addActionListener(e -> setReportReadState(true));
        
        actionPanel.add(btnUnread);
        actionPanel.add(btnRead);
        topPanel.add(actionPanel, BorderLayout.EAST);

        container.add(topPanel, BorderLayout.NORTH);

        // Split Pane for Mail-like interface
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);
        splitPane.setResizeWeight(0.6); // 60% table, 40% detail

        // Table
        reportTableManager = new AdminReportTableManager();
        reportTableManager.setSelectionListener(entry -> updateReportDetailArea());
        JScrollPane tableScroll = reportTableManager.createScrollPane();
        tableScroll.setBorder(new LineBorder(new Color(230,230,230)));
        splitPane.setTopComponent(tableScroll);

        // Detail View
        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBackground(Color.WHITE);
        detailPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        JLabel detailTitle = new JLabel("Detail Pesan");
        detailTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        detailTitle.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        reportDetailArea = new JTextArea();
        reportDetailArea.setEditable(false);
        reportDetailArea.setLineWrap(true);
        reportDetailArea.setWrapStyleWord(true);
        reportDetailArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        reportDetailArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        reportDetailArea.setBackground(new Color(250, 250, 250));
        
        JScrollPane detailScroll = new JScrollPane(reportDetailArea);
        detailScroll.setBorder(new LineBorder(new Color(230,230,230)));
        
        detailPanel.add(detailTitle, BorderLayout.NORTH);
        detailPanel.add(detailScroll, BorderLayout.CENTER);
        
        splitPane.setBottomComponent(detailPanel);
        container.add(splitPane, BorderLayout.CENTER);

        return container;
    }

    // --- Functional Methods (Logic preserved) ---
    // ... (Metode logika sama persis seperti sebelumnya, hanya refresh UI) ...

    void refreshCommodityTable() {
        if (commodityTableManager != null) {
            commodityTableManager.refreshRows(parent.getSortedCommodities());
            String selectedId = parent.getSelectedCommodityId();
            if (selectedId != null) commodityTableManager.selectCommodity(selectedId);
            repaintChart();
        }
    }

    void repaintChart() {
        if (chartPreview != null) chartPreview.refreshChart();
    }

    void refreshReportTable() {
        if (reportTableManager != null) {
            currentReports = parent.getReportsSnapshot();
            reportTableManager.refresh(currentReports);
            updateReportDetailArea();
        }
    }

    private void populateCommodityForm() {
        if (commodityTableManager == null) return;
        String commodityId = commodityTableManager.getSelectedCommodityId();
        if (commodityId == null) return;
        MarketDataModel.CommodityData commodity = parent.getCommodityById(commodityId);
        if (commodity == null) return;
        nameField.setText(commodity.name);
        priceField.setText(String.valueOf(commodity.price));
        categoryCombo.setSelectedItem(commodity.category);
        parent.selectCommodityFromAdmin(commodity.id);
        repaintChart();
    }

    private void handleAddCommodity() {
        String name = nameField.getText().trim();
        int price = parsePriceInput(priceField.getText());
        String category = (String) categoryCombo.getSelectedItem();
        if (name.isEmpty() || price <= 0) {
            JOptionPane.showMessageDialog(dashboardDialog, "Input tidak valid", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        MarketDataModel.CommodityData commodity = parent.addCommodityFromAdmin(name, price, category);
        refreshCommodityTable();
        selectCommodityInTable(commodity.id);
        nameField.setText("");
        priceField.setText("");
    }

    private void handleUpdateCommodity() {
        String id = commodityTableManager.getSelectedCommodityId();
        if (id == null) return;
        String name = nameField.getText().trim();
        int price = parsePriceInput(priceField.getText());
        String category = (String) categoryCombo.getSelectedItem();
        if (name.isEmpty() || price <= 0) return;
        parent.updateCommodityFromAdmin(id, name, price, category);
        refreshCommodityTable();
    }

    private void handleDeleteCommodity() {
        String id = commodityTableManager.getSelectedCommodityId();
        if (id == null) return;
        int confirm = JOptionPane.showConfirmDialog(dashboardDialog, "Hapus item ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            parent.deleteCommodityFromAdmin(id);
            refreshCommodityTable();
            nameField.setText("");
            priceField.setText("");
        }
    }

    private void setReportReadState(boolean read) {
        if (reportTableManager == null) return;
        int row = reportTableManager.getSelectedIndex();
        if (row >= 0 && row < currentReports.size()) {
            parent.updateReportReadState(row, read);
            refreshReportTable();
        }
    }

    private void updateReportDetailArea() {
        MarketDataModel.ReportEntry entry = reportTableManager.getSelectedReport();
        if (entry == null) {
            reportDetailArea.setText("");
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append("ITEM: ").append(entry.getCommodityName().toUpperCase()).append("\n");
        sb.append("TANGGAL: ").append(sdf.format(new Date(entry.getTimestamp()))).append("\n");
        sb.append("STATUS: ").append(entry.isRead() ? "DIBACA" : "BARU").append("\n");
        sb.append("--------------------------------------------------\n\n");
        sb.append(entry.getMessage());
        reportDetailArea.setText(sb.toString());
    }

    private void selectCommodityInTable(String commodityId) {
        if (commodityTableManager != null) commodityTableManager.selectCommodity(commodityId);
    }

    private int parsePriceInput(String text) {
        try { return Integer.parseInt(text.replaceAll("[^0-9]", "")); } 
        catch (NumberFormatException e) { return -1; }
    }

    private int countPriceIncrease() {
        return (int) parent.getSortedCommodities().stream().filter(c -> c.change > 0).count();
    }

    private void clearDashboardReferences() {
        dashboardDialog = null;
        commodityTableManager = null;
        reportTableManager = null;
        nameField = null;
        priceField = null;
        categoryCombo = null;
        reportDetailArea = null;
        chartPreview = null;
    }

    private MarketDataModel.CommodityData getChartCommoditySnapshot() {
        String id = (commodityTableManager != null) ? commodityTableManager.getSelectedCommodityId() : null;
        if (id == null) id = parent.getSelectedCommodityId();
        return (id != null) ? parent.getCommodityById(id) : null;
    }
}