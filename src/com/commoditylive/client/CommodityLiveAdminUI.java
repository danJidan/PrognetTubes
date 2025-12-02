package com.commoditylive.client;

import com.commoditylive.client.ui.UIComponentFactory;
import com.commoditylive.client.ui.admin.AdminCommodityTableManager;
import com.commoditylive.client.ui.admin.AdminReportTableManager;
import com.commoditylive.common.Message;
import com.commoditylive.model.MarketDataModel;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

class CommodityLiveAdminUI {
    private final CommodityLiveGUI parent;
    private JDialog dashboardDialog;
    
    // Input Components
    private JTextField nameField, priceField;
    private JComboBox<String> categoryCombo;
    
    // Tables
    private AdminCommodityTableManager commodityTableManager;
    private AdminReportTableManager reportTableManager;

    CommodityLiveAdminUI(CommodityLiveGUI parent) {
        this.parent = parent;
    }

    // --- LOGIN DIALOG (Simple & Modern) ---
    void showLoginDialog() {
        JDialog loginDialog = new JDialog(parent, "Admin Login", true);
        loginDialog.setSize(350, 280);
        loginDialog.setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Admin Access");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UIComponentFactory.COLOR_PRIMARY);
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JTextField userField = new JTextField(); 
        userField.setBorder(BorderFactory.createTitledBorder("Username"));
        JPasswordField passField = new JPasswordField();
        passField.setBorder(BorderFactory.createTitledBorder("Password"));

        JButton loginBtn = new JButton("LOGIN");
        UIComponentFactory.applyModernButtonStyle(loginBtn, UIComponentFactory.COLOR_PRIMARY);
        
        loginBtn.addActionListener(e -> {
            String u = userField.getText();
            String p = new String(passField.getPassword());
            if(u.equals("admin") && p.equals("admin123")) {
                loginDialog.dispose();
                showDashboard(u);
            } else {
                JOptionPane.showMessageDialog(loginDialog, "Wrong credentials!");
            }
        });

        panel.add(title);
        panel.add(userField);
        panel.add(passField);
        panel.add(loginBtn);
        
        loginDialog.add(panel);
        loginDialog.setVisible(true);
    }

    // --- DASHBOARD UTAMA ADMIN ---
    private void showDashboard(String username) {
        dashboardDialog = new JDialog(parent, "Control Panel - " + username, false);
        dashboardDialog.setSize(900, 600);
        dashboardDialog.setLocationRelativeTo(parent);
        dashboardDialog.setLayout(new BorderLayout());

        // Header Admin
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIComponentFactory.COLOR_PRIMARY);
        header.setPreferredSize(new Dimension(0, 50));
        header.setBorder(new EmptyBorder(0, 20, 0, 20));
        
        JLabel title = new JLabel("Administrator Control Panel");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.add(title, BorderLayout.WEST);
        
        dashboardDialog.add(header, BorderLayout.NORTH);

        // Tabbed Pane Modern
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabs.addTab("Manajemen Komoditas", createCommodityPanel());
        tabs.addTab("Pesan & Laporan", createReportPanel());
        
        dashboardDialog.add(tabs, BorderLayout.CENTER);
        
        // Load data awal
        refreshCommodityTable();
        refreshReportTable();
        
        dashboardDialog.setVisible(true);
    }

    // --- TAB 1: MANAJEMEN KOMODITAS ---
    private JPanel createCommodityPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(UIComponentFactory.COLOR_BG_MAIN);

        // Kiri: Form Input
        JPanel formCard = UIComponentFactory.createCardPanel();
        formCard.setPreferredSize(new Dimension(300, 0));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        JLabel formTitle = new JLabel("Editor Barang");
        UIComponentFactory.applyHeaderStyle(formTitle);
        formCard.add(formTitle);
        formCard.add(Box.createVerticalStrut(20));

        nameField = new JTextField();
        priceField = new JTextField();
        categoryCombo = new JComboBox<>(new String[]{"Bahan Pokok", "Sayuran", "Lauk Pauk", "Bumbu", "Buah"});
        
        addLabelAndField(formCard, "Nama Komoditas", nameField);
        addLabelAndField(formCard, "Kategori", categoryCombo);
        addLabelAndField(formCard, "Harga (Rp)", priceField);

        formCard.add(Box.createVerticalStrut(20));
        
        // Tombol Action
        JPanel btnPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        btnPanel.setBackground(Color.WHITE);
        
        JButton addBtn = new JButton("TAMBAH BARU");
        UIComponentFactory.applyModernButtonStyle(addBtn, UIComponentFactory.COLOR_SUCCESS);
        addBtn.addActionListener(e -> handleAddCommodity());

        JButton updateBtn = new JButton("UPDATE HARGA");
        UIComponentFactory.applyModernButtonStyle(updateBtn, UIComponentFactory.COLOR_ACCENT);
        updateBtn.addActionListener(e -> handleUpdateCommodity());

        JButton deleteBtn = new JButton("HAPUS DATA");
        UIComponentFactory.applyModernButtonStyle(deleteBtn, UIComponentFactory.COLOR_DANGER);
        deleteBtn.addActionListener(e -> handleDeleteCommodity());

        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);
        formCard.add(btnPanel);

        // Kanan: Tabel
        commodityTableManager = new AdminCommodityTableManager();
        commodityTableManager.setSelectionListener(this::populateForm);
        JScrollPane tableScroll = commodityTableManager.createScrollPane();
        tableScroll.setBorder(BorderFactory.createTitledBorder("Database Server"));

        panel.add(formCard, BorderLayout.WEST);
        panel.add(tableScroll, BorderLayout.CENTER);
        return panel;
    }

    // --- TAB 2: LAPORAN USER ---
    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(UIComponentFactory.COLOR_BG_MAIN);

        reportTableManager = new AdminReportTableManager();
        JScrollPane scroll = reportTableManager.createScrollPane();
        scroll.setBorder(BorderFactory.createTitledBorder("Kotak Masuk Laporan"));

        JButton refreshBtn = new JButton("Refresh Laporan");
        UIComponentFactory.applyModernButtonStyle(refreshBtn, UIComponentFactory.COLOR_PRIMARY);
        refreshBtn.addActionListener(e -> refreshReportTable());

        panel.add(refreshBtn, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // --- LOGIC UTAMA (Tetap gunakan logic Dev 4 yang sudah kita buat) ---

    private void handleAddCommodity() {
        String n = nameField.getText();
        String pStr = priceField.getText();
        String c = (String) categoryCombo.getSelectedItem();
        
        if(n.isEmpty() || pStr.isEmpty()) return;
        
        try {
            int p = Integer.parseInt(pStr);
            Message msg = new Message(Message.Type.REQUEST, Message.Action.CREATE_COMMODITY);
            msg.addData("name", n);
            msg.addData("price", p);
            msg.addData("category", c);
            parent.sendRequest(msg);
            
            // Clear
            nameField.setText("");
            priceField.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(dashboardDialog, "Harga harus angka!");
        }
    }

    private void handleUpdateCommodity() {
        String id = commodityTableManager.getSelectedCommodityId();
        if(id == null) return;
        
        try {
            int p = Integer.parseInt(priceField.getText());
            Message msg = new Message(Message.Type.REQUEST, Message.Action.UPDATE_PRICE);
            msg.addData("id", id);
            msg.addData("newPrice", p);
            parent.sendRequest(msg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(dashboardDialog, "Harga tidak valid");
        }
    }

    private void handleDeleteCommodity() {
        String id = commodityTableManager.getSelectedCommodityId();
        if(id == null) {
            JOptionPane.showMessageDialog(dashboardDialog, "Pilih komoditas yang akan dihapus");
            return;
        }
        
        // Get commodity name for confirmation
        MarketDataModel.CommodityData c = parent.getCommodityById(id);
        String itemName = (c != null) ? c.name : id;
        
        int confirm = JOptionPane.showConfirmDialog(
            dashboardDialog, 
            "Hapus permanen komoditas '" + itemName + "'?",
            "Konfirmasi Hapus",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if(confirm == JOptionPane.YES_OPTION) {
            Message msg = new Message(Message.Type.REQUEST, Message.Action.DELETE_COMMODITY);
            msg.addData("id", id);
            parent.sendRequest(msg);
            
            // Clear form setelah delete
            nameField.setText("");
            priceField.setText("");
            categoryCombo.setSelectedIndex(0);
        }
    }

    private void populateForm(String id) {
        MarketDataModel.CommodityData c = parent.getCommodityById(id);
        if(c != null) {
            nameField.setText(c.name);
            priceField.setText(String.valueOf(c.price));
            categoryCombo.setSelectedItem(c.category);
        }
    }

    private void addLabelAndField(JPanel p, String text, JComponent field) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        p.add(l);
        p.add(Box.createVerticalStrut(5));
        field.setMaximumSize(new Dimension(300, 30));
        p.add(field);
        p.add(Box.createVerticalStrut(10));
    }

    // Metode Refresh Data
    void refreshCommodityTable() {
        if(commodityTableManager != null) 
            commodityTableManager.refreshRows(parent.getSortedCommodities());
    }
    
    void refreshReportTable() {
        if(reportTableManager == null) return;
        
        // Disini kita perlu Request ke Server untuk ambil laporan terbaru
        // Karena parent (GUI) tidak otomatis request laporan, kita bisa 'numpang' request
        Message msg = new Message(Message.Type.REQUEST, Message.Action.GET_REPORTS);
        parent.sendRequest(msg);
        
        // Note: Karena response asynchronus, tabel akan update setelah message response diterima di CommodityLiveGUI
        // Untuk sekarang, kita ambil snapshot lokal dulu kalau ada
        // reportTableManager.refresh(parent.getReportsSnapshot());
    }
    
    // Dipanggil dari CommodityLiveGUI saat data laporan baru datang dari server
    public void updateReportTableData(List<MarketDataModel.ReportEntry> reports) {
        if(reportTableManager != null) {
            reportTableManager.refresh(reports);
        }
    }
    
    // Jangan lupa update CommodityLiveGUI handleMessage untuk memanggil updateReportTableData
    void repaintChart() {} // Placeholder
}
