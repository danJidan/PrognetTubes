package com.pasarlive.client.ui.admin;

import com.pasarlive.model.MarketDataModel;
import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class AdminReportTableManager {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<MarketDataModel.ReportEntry> reports = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
    private Consumer<MarketDataModel.ReportEntry> selectionListener;

    public AdminReportTableManager() {
        this.tableModel = new DefaultTableModel(new String[]{"Status", "Komoditas", "Pesan", "Waktu"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.table = new JTable(tableModel);
        configureTableAppearance();
        this.table.getSelectionModel().addListSelectionListener(new ReportSelectionForwarder());
    }

    public JScrollPane createScrollPane() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    public void refresh(List<MarketDataModel.ReportEntry> newReports) {
        reports.clear();
        reports.addAll(newReports);
        tableModel.setRowCount(0);
        for (MarketDataModel.ReportEntry entry : reports) {
            tableModel.addRow(new Object[]{
                entry.isRead() ? "✅ Dibaca" : "🕘 Belum Dibaca",
                entry.getCommodityName(),
                entry.getMessage(),
                sdf.format(new Date(entry.getTimestamp()))
            });
        }
    }

    public void setSelectionListener(Consumer<MarketDataModel.ReportEntry> listener) {
        this.selectionListener = listener;
    }

    public MarketDataModel.ReportEntry getSelectedReport() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= reports.size()) {
            return null;
        }
        return reports.get(row);
    }

    public int getSelectedIndex() {
        return table.getSelectedRow();
    }

    private void configureTableAppearance() {
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setAutoCreateRowSorter(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(248, 248, 248));
        header.setForeground(Color.DARK_GRAY);
    }

    private class ReportSelectionForwarder implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            if (selectionListener != null) {
                selectionListener.accept(getSelectedReport());
            }
        }
    }
}
