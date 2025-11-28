package com.pasarlive.client.ui;

import com.pasarlive.model.MarketDataModel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class CommodityTableManager {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<String> commodityIds = new ArrayList<>();
    private Consumer<String> selectionListener;

    public CommodityTableManager() {
        tableModel = new DefaultTableModel(new Object[]{"Komoditas", "Perubahan", "Harga"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        configureTableAppearance();
        table.getSelectionModel().addListSelectionListener(new TableSelectionListener());
    }

    public void setSelectionListener(Consumer<String> listener) {
        this.selectionListener = listener;
    }

    public JScrollPane createScrollPane() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    public void refreshRows(List<MarketDataModel.CommodityData> commodities, String selectedCommodityId) {
        tableModel.setRowCount(0);
        commodityIds.clear();
        for (MarketDataModel.CommodityData commodity : commodities) {
            tableModel.addRow(new Object[]{
                commodity.name,
                buildChangeLabel(commodity.price - commodity.yesterdayPrice),
                String.format("Rp %,d", commodity.price)
            });
            commodityIds.add(commodity.id);
        }
        if (selectedCommodityId != null) {
            selectCommodity(selectedCommodityId);
        }
    }

    public void selectCommodity(String commodityId) {
        int rowIndex = commodityIds.indexOf(commodityId);
        if (rowIndex >= 0 && rowIndex < table.getRowCount()) {
            table.setRowSelectionInterval(rowIndex, rowIndex);
        }
    }

    private void configureTableAppearance() {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(Color.WHITE);
        table.setSelectionForeground(Color.BLACK);
        table.setFocusable(false);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(248, 248, 248));
        header.setForeground(Color.DARK_GRAY);
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer rightAlignRenderer = new DefaultTableCellRenderer();
        rightAlignRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(rightAlignRenderer);
    }

    private String buildChangeLabel(int priceDiff) {
        if (priceDiff > 0) {
            return String.format("↗ +%,d", priceDiff);
        }
        if (priceDiff < 0) {
            return String.format("↘ %,d", priceDiff);
        }
        return "— +0";
    }

    private class TableSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < commodityIds.size() && selectionListener != null) {
                selectionListener.accept(commodityIds.get(selectedRow));
            }
        }
    }
}
