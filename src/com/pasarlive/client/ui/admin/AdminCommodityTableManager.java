package com.pasarlive.client.ui.admin;

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
import javax.swing.RowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class AdminCommodityTableManager {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<String> commodityIds = new ArrayList<>();
    private Consumer<String> selectionListener;

    public AdminCommodityTableManager() {
        this.tableModel = new DefaultTableModel(new String[]{"ID", "Nama", "Kategori", "Harga"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.table = new JTable(tableModel);
        configureTableAppearance();
        this.table.getSelectionModel().addListSelectionListener(new SelectionForwarder());
    }

    public JScrollPane createScrollPane() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    public void refreshRows(List<MarketDataModel.CommodityData> commodities) {
        tableModel.setRowCount(0);
        commodityIds.clear();
        for (MarketDataModel.CommodityData commodity : commodities) {
            tableModel.addRow(new Object[]{
                commodity.id,
                commodity.name,
                commodity.category,
                String.format("Rp %,d", commodity.price)
            });
            commodityIds.add(commodity.id);
        }
    }

    public void selectCommodity(String commodityId) {
        int modelIndex = commodityIds.indexOf(commodityId);
        if (modelIndex < 0) {
            return;
        }
        int viewIndex = convertModelToView(modelIndex);
        if (viewIndex >= 0 && viewIndex < table.getRowCount()) {
            table.setRowSelectionInterval(viewIndex, viewIndex);
        }
    }

    public void setSelectionListener(Consumer<String> listener) {
        this.selectionListener = listener;
    }

    public String getSelectedCommodityId() {
        int viewIndex = table.getSelectedRow();
        if (viewIndex < 0) {
            return null;
        }
        int modelIndex = convertViewToModel(viewIndex);
        if (modelIndex >= 0 && modelIndex < commodityIds.size()) {
            return commodityIds.get(modelIndex);
        }
        return null;
    }

    private int convertViewToModel(int viewRow) {
        RowSorter<? extends TableModel> sorter = table.getRowSorter();
        if (sorter != null) {
            return table.convertRowIndexToModel(viewRow);
        }
        return viewRow;
    }

    private int convertModelToView(int modelRow) {
        RowSorter<? extends TableModel> sorter = table.getRowSorter();
        if (sorter != null) {
            return table.convertRowIndexToView(modelRow);
        }
        return modelRow;
    }

    private void configureTableAppearance() {
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoCreateRowSorter(true);
        table.setSelectionBackground(new Color(230, 247, 255));
        table.setSelectionForeground(Color.BLACK);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(248, 248, 248));
        header.setForeground(Color.DARK_GRAY);

        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setCellRenderer(new StripeRenderer(i == 3));
        }
    }

    private void forwardSelection() {
        if (selectionListener == null) {
            return;
        }
        String commodityId = getSelectedCommodityId();
        if (commodityId != null) {
            selectionListener.accept(commodityId);
        }
    }

    private class SelectionForwarder implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            forwardSelection();
        }
    }

    private static class StripeRenderer extends DefaultTableCellRenderer {
        private final boolean alignRight;

        private StripeRenderer(boolean alignRight) {
            this.alignRight = alignRight;
            setOpaque(true);
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(alignRight ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 249, 249));
            }
            return this;
        }
    }
}
