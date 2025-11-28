package com.pasarlive.client.ui;

import com.pasarlive.model.MarketDataModel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JPanel;

public class CommodityChartPanel extends JPanel {
    private final Supplier<MarketDataModel.CommodityData> commoditySupplier;

    public CommodityChartPanel(Supplier<MarketDataModel.CommodityData> commoditySupplier) {
        this.commoditySupplier = commoditySupplier;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        MarketDataModel.CommodityData commodity = commoditySupplier.get();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int padding = 40;

        if (commodity == null || commodity.priceHistory.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString("Pilih komoditas untuk melihat grafik", width / 2 - 90, height / 2);
            return;
        }

        List<Integer> prices = commodity.priceHistory;
        int dataPoints = prices.size();

        int minPrice = prices.stream().min(Integer::compareTo).orElse(0);
        int maxPrice = prices.stream().max(Integer::compareTo).orElse(1);
        int priceRange = maxPrice - minPrice;
        if (priceRange == 0) {
            priceRange = 1000;
        }

        g2.setColor(new Color(240, 240, 240));
        for (int i = 0; i <= 5; i++) {
            int y = padding + (height - 2 * padding) * i / 5;
            g2.drawLine(padding, y, width - padding, y);
        }

        g2.setColor(Color.GRAY);
        for (int i = 0; i <= 5; i++) {
            int price = maxPrice - (priceRange * i / 5);
            String priceLabel = String.format("%dk", price / 1000);
            int y = padding + (height - 2 * padding) * i / 5;
            g2.drawString(priceLabel, 5, y + 5);
        }

        String[] xLabels = {"20 Nov", "21 Nov", "22 Nov", "23 Nov", "24 Nov", "25 Nov"};
        for (int i = 0; i < Math.min(dataPoints, xLabels.length); i++) {
            int x = padding + (width - 2 * padding) * i / (dataPoints - 1);
            g2.drawString(xLabels[i], x - 15, height - 10);
        }

        Color lineColor = commodity.change >= 0 ? new Color(46, 204, 113) : new Color(220, 53, 69);
        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke(3));

        for (int i = 0; i < dataPoints - 1; i++) {
            int x1 = padding + (width - 2 * padding) * i / (dataPoints - 1);
            int price1 = prices.get(i);
            int y1 = padding + (height - 2 * padding) - (int) ((price1 - minPrice) * (height - 2 * padding) / (double) priceRange);

            int x2 = padding + (width - 2 * padding) * (i + 1) / (dataPoints - 1);
            int price2 = prices.get(i + 1);
            int y2 = padding + (height - 2 * padding) - (int) ((price2 - minPrice) * (height - 2 * padding) / (double) priceRange);

            g2.drawLine(x1, y1, x2, y2);
            g2.fillOval(x1 - 4, y1 - 4, 8, 8);
        }

        int lastX = padding + (width - 2 * padding);
        int lastPrice = prices.get(dataPoints - 1);
        int lastY = padding + (height - 2 * padding) - (int) ((lastPrice - minPrice) * (height - 2 * padding) / (double) priceRange);
        g2.fillOval(lastX - 4, lastY - 4, 8, 8);
    }

    public void refreshChart() {
        repaint();
    }
}
