package com.pasarlive.client.ui;

import com.pasarlive.model.MarketDataModel;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JPanel;

public class CommodityChartPanel extends JPanel {
    private final Supplier<MarketDataModel.CommodityData> commoditySupplier;

    // --- MODERN PALETTE CONSTANTS ---
    private static final Color POSITIVE_COLOR = new Color(16, 185, 129); // Emerald Green
    private static final Color NEGATIVE_COLOR = new Color(239, 68, 68);  // Red
    private static final Color GRID_COLOR = new Color(241, 245, 249);    // Very light gray
    private static final Color TEXT_COLOR = new Color(148, 163, 184);    // Slate 400
    private static final Font AXIS_FONT = new Font("Segoe UI", Font.PLAIN, 11);
    
    public CommodityChartPanel(Supplier<MarketDataModel.CommodityData> commoditySupplier) {
        this.commoditySupplier = commoditySupplier;
        setBackground(Color.WHITE);
        setOpaque(false); // Allow rounded corner parent to show
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int padding = 40;
        int bottomPadding = 30;

        MarketDataModel.CommodityData commodity = commoditySupplier.get();

        // 1. Handle No Data State
        if (commodity == null || commodity.priceHistory.isEmpty()) {
            drawEmptyState(g2, width, height);
            return;
        }

        List<Integer> prices = commodity.priceHistory;
        int dataPoints = prices.size();

        // Calculate Min/Max for Scaling
        int minPrice = prices.stream().min(Integer::compareTo).orElse(0);
        int maxPrice = prices.stream().max(Integer::compareTo).orElse(1);
        
        // Add padding to chart range so line doesn't touch top/bottom exactly
        int rangeBuffer = (maxPrice - minPrice) == 0 ? 1000 : (maxPrice - minPrice) / 10;
        int chartMin = minPrice - rangeBuffer;
        int chartMax = maxPrice + rangeBuffer;
        int priceRange = chartMax - chartMin;
        if (priceRange == 0) priceRange = 1000;

        // Determine Trend Color
        Color themeColor = commodity.change >= 0 ? POSITIVE_COLOR : NEGATIVE_COLOR;

        // 2. Draw Grid & Y-Axis Labels
        drawGridAndYLabels(g2, width, height, padding, bottomPadding, chartMin, chartMax, priceRange);

        // 3. Calculate Coordinates
        int[] xPoints = new int[dataPoints];
        int[] yPoints = new int[dataPoints];
        
        int chartHeight = height - padding - bottomPadding;
        int chartWidth = width - (2 * padding);

        for (int i = 0; i < dataPoints; i++) {
            xPoints[i] = padding + (chartWidth * i / (dataPoints - 1));
            // Invert Y because Swing coords start at top
            yPoints[i] = (padding + chartHeight) - (int) ((prices.get(i) - chartMin) * chartHeight / (double) priceRange);
        }

        // 4. Draw Area (Gradient Fill under line)
        drawAreaFill(g2, xPoints, yPoints, themeColor, padding + chartHeight);

        // 5. Draw Line
        g2.setColor(themeColor);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        GeneralPath polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, dataPoints);
        polyline.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < dataPoints; i++) {
            polyline.lineTo(xPoints[i], yPoints[i]);
        }
        g2.draw(polyline);

        // 6. Draw Dots (Halo Effect)
        drawDataPoints(g2, xPoints, yPoints, themeColor);

        // 7. Draw X-Axis Labels (Dummy Dates based on index)
        drawXLabels(g2, dataPoints, xPoints, height);
    }

    private void drawEmptyState(Graphics2D g2, int width, int height) {
        g2.setColor(TEXT_COLOR);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        String msg = "Data grafik tidak tersedia";
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(msg);
        g2.drawString(msg, (width - textWidth) / 2, height / 2);
    }

    private void drawGridAndYLabels(Graphics2D g2, int width, int height, int padding, int bottomPadding, int min, int max, int range) {
        g2.setFont(AXIS_FONT);
        FontMetrics fm = g2.getFontMetrics();
        int steps = 5;
        
        for (int i = 0; i <= steps; i++) {
            int y = (height - bottomPadding) - ((height - padding - bottomPadding) * i / steps);
            
            // Draw Grid Line
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(padding, y, width - padding, y);

            // Draw Label
            g2.setColor(TEXT_COLOR);
            int priceValue = min + (range * i / steps);
            String label = formatPriceLabel(priceValue);
            
            // Right align text to the left of the chart
            int labelWidth = fm.stringWidth(label);
            g2.drawString(label, padding - labelWidth - 8, y + 4);
        }
    }

    private void drawAreaFill(Graphics2D g2, int[] x, int[] y, Color color, int bottomY) {
        GeneralPath area = new GeneralPath();
        area.moveTo(x[0], bottomY); // Start bottom-left
        for (int i = 0; i < x.length; i++) {
            area.lineTo(x[i], y[i]);
        }
        area.lineTo(x[x.length - 1], bottomY); // End bottom-right
        area.closePath();

        // Create gradient from semi-transparent color to fully transparent
        Color startColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 60); // 60 alpha
        Color endColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);   // 0 alpha
        GradientPaint gp = new GradientPaint(0, 0, startColor, 0, bottomY, endColor);
        
        g2.setPaint(gp);
        g2.fill(area);
    }

    private void drawDataPoints(Graphics2D g2, int[] x, int[] y, Color color) {
        int dotSize = 8;
        for (int i = 0; i < x.length; i++) {
            int drawX = x[i] - (dotSize / 2);
            int drawY = y[i] - (dotSize / 2);

            // White center
            g2.setColor(Color.WHITE);
            g2.fillOval(drawX, drawY, dotSize, dotSize);

            // Colored border
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(drawX, drawY, dotSize, dotSize);
        }
    }

    private void drawXLabels(Graphics2D g2, int count, int[] xPoints, int height) {
        g2.setColor(TEXT_COLOR);
        g2.setFont(AXIS_FONT);
        FontMetrics fm = g2.getFontMetrics();
        
        // Hardcoded labels for demo, ideally this comes from data
        String[] labels = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
        
        // Only draw labels if we have enough points, avoid overcrowding
        int skip = (int) Math.ceil((double)count / 6); 

        for (int i = 0; i < count; i += skip) {
            if (i >= xPoints.length) break;
            
            String label;
            if (i < labels.length) label = labels[i];
            else label = "H-" + (i+1);

            int textWidth = fm.stringWidth(label);
            g2.drawString(label, xPoints[i] - (textWidth / 2), height - 10);
        }
    }

    private String formatPriceLabel(int price) {
        if (price >= 1000) {
            return String.format("%.1fk", price / 1000.0);
        }
        return String.valueOf(price);
    }

    public void refreshChart() {
        repaint();
    }
}