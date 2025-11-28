package com.pasarlive.client;

import com.pasarlive.client.ui.UIComponentFactory;
import com.pasarlive.client.ui.admin.AdminChartPanel;
import com.pasarlive.client.ui.admin.AdminCommodityTableManager;
import com.pasarlive.client.ui.admin.AdminReportTableManager;
import com.pasarlive.client.ui.cards.StatInfoCard;
import com.pasarlive.model.MarketDataModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

class PasarLiveAdminUI {
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

    void showLoginDialog() {
        JDialog loginDialog = new JDialog(parent, "Login Admin", true);
        loginDialog.setSize(400, 300);
        loginDialog.setLocationRelativeTo(parent);
        loginDialog.setResizable(false);

        JPanel mainPanel = UIComponentFactory.createWhitePanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel titleLabel = new JLabel("🔐 Admin Login");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(52, 73, 94));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Masukkan kredensial admin");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(30));

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setMaximumSize(new Dimension(300, 35));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(5, 10, 5, 10)
        ));

        mainPanel.add(usernameLabel);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(usernameField);
        mainPanel.add(Box.createVerticalStrut(15));

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setMaximumSize(new Dimension(300, 35));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(5, 10, 5, 10)
        ));

        mainPanel.add(passwordLabel);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(passwordField);
        mainPanel.add(Box.createVerticalStrut(25));

        JButton loginButton = new JButton("Login");
        UIComponentFactory.applyFlatButtonStyle(
            loginButton,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(46, 204, 113),
            Color.WHITE
        );
        loginButton.setMaximumSize(new Dimension(300, 40));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog,
                    "Username dan password tidak boleh kosong!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            } else if (username.equals("admin") && password.equals("admin123")) {
                loginDialog.dispose();
                JOptionPane.showMessageDialog(parent,
                    "Login berhasil! Selamat datang, Admin.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                showDashboard(username);
            } else {
                JOptionPane.showMessageDialog(loginDialog,
                    "Username atau password salah!",
                    "Login Gagal",
                    JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
            }
        });

        JButton cancelButton = new JButton("Batal");
        UIComponentFactory.applyFlatButtonStyle(
            cancelButton,
            new Font("Segoe UI", Font.PLAIN, 12),
            new Color(149, 165, 166),
            Color.WHITE
        );
        cancelButton.setMaximumSize(new Dimension(300, 35));
        cancelButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        cancelButton.addActionListener(e -> loginDialog.dispose());

        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(cancelButton);

        passwordField.addActionListener(e -> loginButton.doClick());

        loginDialog.add(mainPanel);
        loginDialog.setVisible(true);
    }

    void refreshCommodityTable() {
        if (commodityTableManager == null) {
            return;
        }
        commodityTableManager.refreshRows(parent.getSortedCommodities());
        String selectedId = parent.getSelectedCommodityId();
        if (selectedId != null) {
            commodityTableManager.selectCommodity(selectedId);
        }
        repaintChart();
    }

    void repaintChart() {
        if (chartPreview != null) {
            chartPreview.refreshChart();
        }
    }

    void refreshReportTable() {
        if (reportTableManager == null) {
            return;
        }
        currentReports = parent.getReportsSnapshot();
        reportTableManager.refresh(currentReports);
        updateReportDetailArea();
    }

    private void showDashboard(String username) {
        dashboardDialog = new JDialog(parent, "Admin Dashboard", false);
        dashboardDialog.setSize(950, 650);
        dashboardDialog.setLocationRelativeTo(parent);
        dashboardDialog.setLayout(new BorderLayout());
        dashboardDialog.getContentPane().setBackground(Color.WHITE);
        dashboardDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                clearDashboardReferences();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                clearDashboardReferences();
            }
        });

        JLabel header = new JLabel("Panel Admin - " + username);
        header.setFont(new Font("Segoe UI", Font.BOLD, 20));
        header.setBorder(new EmptyBorder(10, 20, 10, 20));
        dashboardDialog.add(header, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Monitoring", createMonitoringPanel());
        tabbedPane.addTab("Pesan Pengguna", createReportsPanel());
        dashboardDialog.add(tabbedPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Tutup");
        UIComponentFactory.applyFlatButtonStyle(
            closeButton,
            new Font("Segoe UI", Font.PLAIN, 13),
            new Color(149, 165, 166),
            Color.WHITE
        );
        closeButton.addActionListener(e -> dashboardDialog.dispose());
        JPanel footer = UIComponentFactory.createWhitePanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBorder(new EmptyBorder(10, 20, 10, 20));
        footer.add(closeButton);
        dashboardDialog.add(footer, BorderLayout.SOUTH);

        refreshCommodityTable();
        refreshReportTable();
        dashboardDialog.setVisible(true);
    }

    private JPanel createMonitoringPanel() {
        JPanel panel = UIComponentFactory.createWhitePanel(new BorderLayout(10, 10), 15);

        JPanel statsPanel = UIComponentFactory.createWhitePanel(new GridLayout(1, 3, 10, 10));
        statsPanel.add(new StatInfoCard("Total Komoditas", String.valueOf(parent.getSortedCommodities().size()), "🛒"));
        statsPanel.add(new StatInfoCard("Laporan Masuk", String.valueOf(parent.getReportsSnapshot().size()), "📨"));
        statsPanel.add(new StatInfoCard("Harga Naik Hari Ini", String.valueOf(countPriceIncrease()), "📈"));
        panel.add(statsPanel, BorderLayout.NORTH);

        JPanel chartWrapper = UIComponentFactory.createWhitePanel(new BorderLayout());
        chartWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Grafik Harga"),
            new EmptyBorder(10, 10, 10, 10)
        ));
        chartPreview = new AdminChartPanel(this::getChartCommoditySnapshot);
        chartPreview.setPreferredSize(new Dimension(0, 220));
        chartPreview.setBackground(Color.WHITE);
        chartPreview.setBorder(BorderFactory.createLineBorder(new Color(235, 235, 235)));
        chartWrapper.add(chartPreview, BorderLayout.CENTER);
        
        JPanel contentPanel = UIComponentFactory.createWhitePanel(new GridLayout(1, 2, 10, 10));
        contentPanel.add(chartWrapper);
        contentPanel.add(createCommodityTableSection());
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCommodityTableSection() {
        commodityTableManager = new AdminCommodityTableManager();
        commodityTableManager.setSelectionListener(id -> populateCommodityForm());
        JScrollPane tableScroll = commodityTableManager.createScrollPane();
        tableScroll.setBorder(BorderFactory.createTitledBorder("Daftar Komoditas"));
        tableScroll.getViewport().setBackground(Color.WHITE);

        JPanel formPanel = UIComponentFactory.createWhitePanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Kelola Komoditas"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        formPanel.add(new JLabel("Nama"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField();
        formPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(new JLabel("Kategori"), gbc);
        gbc.gridx = 1;
        categoryCombo = new JComboBox<>(new String[]{
            "Bahan Pokok", "Sayuran", "Lauk Pauk", "Bumbu", "Buah", "Minuman"
        });
        formPanel.add(categoryCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(new JLabel("Harga"), gbc);
        gbc.gridx = 1;
        priceField = new JTextField();
        formPanel.add(priceField, gbc);

        JPanel buttonPanel = UIComponentFactory.createWhitePanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Tambah");
        addButton.addActionListener(e -> handleAddCommodity());
        JButton updateButton = new JButton("Update");
        updateButton.addActionListener(e -> handleUpdateCommodity());
        JButton deleteButton = new JButton("Hapus");
        deleteButton.addActionListener(e -> handleDeleteCommodity());
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);

        JPanel container = UIComponentFactory.createWhitePanel(new BorderLayout(10, 10));
        container.add(tableScroll, BorderLayout.CENTER);
        container.add(formPanel, BorderLayout.SOUTH);
        return container;
    }

    private JPanel createReportsPanel() {
        JPanel panel = UIComponentFactory.createWhitePanel(new BorderLayout(10, 10), 15);

        reportTableManager = new AdminReportTableManager();
        reportTableManager.setSelectionListener(entry -> updateReportDetailArea());
        JScrollPane tableScroll = reportTableManager.createScrollPane();
        tableScroll.setBorder(BorderFactory.createTitledBorder("Pesan Masuk"));

        reportDetailArea = new JTextArea();
        reportDetailArea.setEditable(false);
        reportDetailArea.setLineWrap(true);
        reportDetailArea.setWrapStyleWord(true);
        JScrollPane detailScroll = new JScrollPane(reportDetailArea);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Detail Pesan"));
        detailScroll.setPreferredSize(new Dimension(0, 150));

        JPanel buttonPanel = UIComponentFactory.createWhitePanel(new FlowLayout(FlowLayout.RIGHT));
        JButton markReadButton = new JButton("Tandai Dibaca");
        markReadButton.addActionListener(e -> setReportReadState(true));
        JButton markUnreadButton = new JButton("Tandai Belum Dibaca");
        markUnreadButton.addActionListener(e -> setReportReadState(false));
        buttonPanel.add(markUnreadButton);
        buttonPanel.add(markReadButton);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(detailScroll, BorderLayout.SOUTH);
        return panel;
    }

    private void populateCommodityForm() {
        if (commodityTableManager == null) {
            return;
        }
        String commodityId = commodityTableManager.getSelectedCommodityId();
        if (commodityId == null) {
            return;
        }
        MarketDataModel.CommodityData commodity = parent.getCommodityById(commodityId);
        if (commodity == null) {
            return;
        }
        nameField.setText(commodity.name);
        priceField.setText(String.valueOf(commodity.price));
        categoryCombo.setSelectedItem(commodity.category);
        parent.selectCommodityFromAdmin(commodity.id);
        repaintChart();
    }

    private void handleAddCommodity() {
        if (nameField == null || priceField == null || categoryCombo == null) {
            return;
        }
        String name = nameField.getText().trim();
        int price = parsePriceInput(priceField.getText());
        String category = (String) categoryCombo.getSelectedItem();
        if (name.isEmpty() || price <= 0 || category == null) {
            JOptionPane.showMessageDialog(dashboardDialog,
                "Isi nama, kategori, dan harga valid.",
                "Validasi",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        MarketDataModel.CommodityData commodity = parent.addCommodityFromAdmin(name, price, category);
        refreshCommodityTable();
        selectCommodityInTable(commodity.id);
        JOptionPane.showMessageDialog(dashboardDialog,
            "Komoditas baru ditambahkan.",
            "Sukses",
            JOptionPane.INFORMATION_MESSAGE);
        nameField.setText("");
        priceField.setText("");
    }

    private void handleUpdateCommodity() {
        if (commodityTableManager == null || nameField == null || priceField == null) {
            return;
        }
        String commodityId = commodityTableManager.getSelectedCommodityId();
        if (commodityId == null) {
            JOptionPane.showMessageDialog(dashboardDialog,
                "Pilih komoditas yang ingin diubah.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String name = nameField.getText().trim();
        int price = parsePriceInput(priceField.getText());
        String category = (String) categoryCombo.getSelectedItem();
        if (name.isEmpty() || price <= 0) {
            JOptionPane.showMessageDialog(dashboardDialog,
                "Isi nama dan harga valid.",
                "Validasi",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        MarketDataModel.CommodityData commodity = parent.updateCommodityFromAdmin(commodityId, name, price, category);
        if (commodity != null) {
            refreshCommodityTable();
            selectCommodityInTable(commodity.id);
            JOptionPane.showMessageDialog(dashboardDialog,
                "Komoditas diperbarui.",
                "Sukses",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleDeleteCommodity() {
        if (commodityTableManager == null) {
            return;
        }
        String commodityId = commodityTableManager.getSelectedCommodityId();
        if (commodityId == null) {
            JOptionPane.showMessageDialog(dashboardDialog,
                "Pilih komoditas yang ingin dihapus.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(dashboardDialog,
            "Yakin ingin menghapus komoditas ini?",
            "Konfirmasi",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        MarketDataModel.CommodityData removed = parent.deleteCommodityFromAdmin(commodityId);
        if (removed != null) {
            refreshCommodityTable();
            JOptionPane.showMessageDialog(dashboardDialog,
                "Komoditas dihapus.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void setReportReadState(boolean read) {
        if (reportTableManager == null) {
            return;
        }
        int row = reportTableManager.getSelectedIndex();
        if (row < 0 || row >= currentReports.size()) {
            JOptionPane.showMessageDialog(dashboardDialog,
                "Pilih laporan terlebih dahulu.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        boolean updated = parent.updateReportReadState(row, read);
        if (updated) {
            refreshReportTable();
        }
    }

    private void updateReportDetailArea() {
        if (reportDetailArea == null || reportTableManager == null) {
            return;
        }
        MarketDataModel.ReportEntry entry = reportTableManager.getSelectedReport();
        if (entry == null) {
            reportDetailArea.setText("");
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
        StringBuilder builder = new StringBuilder();
        builder.append("Komoditas: ").append(entry.getCommodityName()).append('\n');
        builder.append("Status: ").append(entry.isRead() ? "Sudah dibaca" : "Belum dibaca").append('\n');
        builder.append("Waktu: ").append(sdf.format(new Date(entry.getTimestamp()))).append("\n\n");
        builder.append(entry.getMessage());
        reportDetailArea.setText(builder.toString());
    }

    private void selectCommodityInTable(String commodityId) {
        if (commodityTableManager == null || commodityId == null) {
            return;
        }
        commodityTableManager.selectCommodity(commodityId);
    }

    private int parsePriceInput(String text) {
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int countPriceIncrease() {
        int count = 0;
        for (MarketDataModel.CommodityData commodity : parent.getSortedCommodities()) {
            if (commodity.change > 0) {
                count++;
            }
        }
        return count;
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
        currentReports = new ArrayList<>();
    }

    private MarketDataModel.CommodityData getChartCommoditySnapshot() {
        String commodityId = null;
        if (commodityTableManager != null) {
            commodityId = commodityTableManager.getSelectedCommodityId();
        }
        if (commodityId == null) {
            commodityId = parent.getSelectedCommodityId();
        }
        return commodityId != null ? parent.getCommodityById(commodityId) : null;
    }
}
