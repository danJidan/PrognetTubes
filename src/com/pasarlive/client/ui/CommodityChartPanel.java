package com.pasarlive.client.ui;

import com.pasarlive.model.MarketDataModel;
import java.awt.*;
import java.util.List;
import javax.swing.JPanel;

public class CommodityChartPanel extends JPanel {
    // KITA SIMPAN DATANYA LANGSUNG DI SINI
    private MarketDataModel.CommodityData currentData;

    public CommodityChartPanel() {
        setBackground(Color.WHITE);
    }

    // --- METHOD BARU: PAKSA TERIMA DATA & GAMBAR ULANG ---
    public void setCommodityData(MarketDataModel.CommodityData data) {
        this.currentData = data;
        // Validasi dan Repaint instan
        if (this.isVisible()) {
            this.paintImmediately(0, 0, getWidth(), getHeight()); // LEBIH KUAT DARI REPAINT
        } else {
            this.repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int pad = 50;

        // Cek apakah data kosong
        if (currentData == null || currentData.priceHistory.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString("Menunggu Data...", w / 2 - 40, h / 2);
            return;
        }

        List<Integer> prices = currentData.priceHistory;
        int points = prices.size();

        // Cari Min/Max
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int p : prices) {
            if (p < min) min = p;
            if (p > max) max = p;
        }
        
        int range = max - min;
        if (range == 0) range = 1000;
        int graphMin = min - (range / 5); 
        int graphMax = max + (range / 5);
        int graphRange = graphMax - graphMin;

        // Grid
        g2.setColor(new Color(245, 245, 245));
        for (int i = 0; i <= 4; i++) {
            int y = pad + (h - 2 * pad) * i / 4;
            g2.drawLine(pad, y, w - pad, y);
        }

        // Label Y (Harga)
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        for (int i = 0; i <= 4; i++) {
            int price = graphMax - (graphRange * i / 4);
            int y = pad + (h - 2 * pad) * i / 4;
            g2.drawString(String.format("%dk", price/1000), 5, y + 5);
        }

        // Label X (Waktu)
        String[] timeLabels = {"60s", "50s", "40s", "30s", "20s", "10s", "Now"};
        for (int i = 0; i < points; i++) {
            int x = pad + (w - 2 * pad) * i / (Math.max(1, points - 1));
            String lbl = timeLabels[timeLabels.length - points + i];
            g2.drawString(lbl, x - 10, h - 15);
        }

        // Grafik Line
        Color lineColor = UIComponentFactory.COLOR_PRIMARY;
        if (currentData.change > 0) lineColor = UIComponentFactory.COLOR_SUCCESS;
        else if (currentData.change < 0) lineColor = UIComponentFactory.COLOR_DANGER;

        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int[] xPoints = new int[points];
        int[] yPoints = new int[points];

        for (int i = 0; i < points; i++) {
            xPoints[i] = pad + (w - 2 * pad) * i / (Math.max(1, points - 1));
            int p = prices.get(i);
            yPoints[i] = pad + (h - 2 * pad) - (int) ((double)(p - graphMin) / graphRange * (h - 2 * pad));
        }

        g2.drawPolyline(xPoints, yPoints, points);

        // Dots & Label Akhir
        for (int i = 0; i < points; i++) {
            g2.setColor(Color.WHITE);
            g2.fillOval(xPoints[i] - 5, yPoints[i] - 5, 10, 10);
            g2.setColor(lineColor);
            g2.fillOval(xPoints[i] - 3, yPoints[i] - 3, 6, 6);
        }
        
        int lastX = xPoints[points - 1];
        int lastY = yPoints[points - 1];
        g2.setColor(lineColor);
        g2.fillOval(lastX - 6, lastY - 6, 12, 12);
        
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.drawString(String.format("Rp %,d", prices.get(points-1)), lastX - 40, lastY - 15);
    }
    
    // Method lama dihapus saja, ganti dengan setCommodityData di atas
    public void refreshChart() { repaint(); }
}