package com.pasarlive.client.ui;

import com.pasarlive.model.MarketDataModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class CommodityTableManager {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<String> commodityIds = new ArrayList<>();
    private Consumer<String> selectionListener;

    public CommodityTableManager() {
        // Kolom: Nama, Harga, Perubahan (Icon)
        tableModel = new DefaultTableModel(new Object[]{"Komoditas", "Harga", "Tren"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        configureModernTable();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                if (selectionListener != null && table.getSelectedRow() < commodityIds.size()) {
                    selectionListener.accept(commodityIds.get(table.getSelectedRow()));
                }
            }
        });
    }

    public void setSelectionListener(Consumer<String> listener) { this.selectionListener = listener; }

    public JScrollPane createScrollPane() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Hilangkan border tajam
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    public void refreshRows(List<MarketDataModel.CommodityData> commodities, String selectedId) {
        tableModel.setRowCount(0);
        commodityIds.clear();
        int selectedRow = -1;

        for (int i = 0; i < commodities.size(); i++) {
            MarketDataModel.CommodityData c = commodities.get(i);
            int diff = c.price - c.yesterdayPrice;
            
            // Format data
            tableModel.addRow(new Object[]{
                c.name,
                String.format("Rp %,d", c.price),
                diff // Kita kirim integer-nya, nanti Renderer yang ubah jadi panah
            });
            commodityIds.add(c.id);
            if (c.id.equals(selectedId)) selectedRow = i;
        }

        if (selectedRow >= 0) table.setRowSelectionInterval(selectedRow, selectedRow);
    }

    public void selectCommodity(String id) {
        int idx = commodityIds.indexOf(id);
        if (idx >= 0) table.setRowSelectionInterval(idx, idx);
    }

    private void configureModernTable() {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(45); // Baris lebih tinggi biar lega
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(240, 240, 240));
        table.setSelectionBackground(new Color(236, 240, 241)); // Warna select lembut
        table.setSelectionForeground(Color.BLACK);
        
        // Header Cantik
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(Color.WHITE);
        header.setForeground(Color.GRAY);
        header.setPreferredSize(new Dimension(0, 40));
        
        // Custom Renderer (Pewarnaan)
        table.setDefaultRenderer(Object.class, new ModernCellRenderer());
    }

    // Kelas dalam untuk mengatur warna teks (Merah/Hijau)
    private static class ModernCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); // Padding kiri kanan cell
            
            if (column == 2) { // Kolom Tren
                int diff = (Integer) value;
                if (diff > 0) {
                    setText("▲ Naik");
                    setForeground(UIComponentFactory.COLOR_SUCCESS);
                } else if (diff < 0) {
                    setText("▼ Turun");
                    setForeground(UIComponentFactory.COLOR_DANGER);
                } else {
                    setText("— Stabil");
                    setForeground(Color.GRAY);
                }
                setHorizontalAlignment(JLabel.RIGHT);
            } else if (column == 1) { // Kolom Harga
                setForeground(UIComponentFactory.COLOR_PRIMARY);
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(JLabel.RIGHT);
            } else { // Kolom Nama
                setForeground(Color.BLACK);
                setHorizontalAlignment(JLabel.LEFT);
            }
            return this;
        }
    }
}