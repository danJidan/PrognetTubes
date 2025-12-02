package com.commoditylive.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketDataModel implements Serializable {
    private final Map<String, CommodityData> commodities = new HashMap<>();
    private final List<ReportEntry> userReports = new ArrayList<>();

    public MarketDataModel() {
        // Data akan di-load dari server
    }
    
    // Method update dari server
    public void setAllCommodities(List<CommodityData> list) {
        commodities.clear();
        for (CommodityData c : list) {
            commodities.put(c.id, c);
        }
    }

    public List<CommodityData> getSortedCommodities() {
        List<CommodityData> list = new ArrayList<>(commodities.values());
        list.sort(Comparator.comparing(c -> c.name.toLowerCase()));
        return list;
    }

    public CommodityData getCommodityById(String id) { return commodities.get(id); }

    public CommodityData getDefaultCommodity() {
        List<CommodityData> sorted = getSortedCommodities();
        return sorted.isEmpty() ? null : sorted.get(0);
    }

    public void addReportEntry(ReportEntry entry) { userReports.add(0, entry); }
    public List<ReportEntry> getReportsSnapshot() { return new ArrayList<>(userReports); }
    
    public boolean updateReportReadState(int index, boolean read) {
        if (index < 0 || index >= userReports.size()) return false;
        userReports.get(index).setRead(read);
        return true;
    }

    public static class CommodityData implements Serializable {
        public final String id;
        public String name;
        public int price;
        public int yesterdayPrice;
        public double change;
        public String category;
        public final List<Integer> priceHistory;

        public CommodityData(String id, String name, int price, double change, String category) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.change = change;
            this.category = category;
            this.priceHistory = new ArrayList<>();
            this.yesterdayPrice = price;
        }
        
        public void addPriceHistory(int... prices) {
            for (int p : prices) priceHistory.add(p);
        }

        public void updatePrice(int newPrice) {
            if (!priceHistory.isEmpty()) yesterdayPrice = priceHistory.get(priceHistory.size() - 1);
            this.price = newPrice;
            priceHistory.add(newPrice);
            while (priceHistory.size() > 7) priceHistory.remove(0);
            if (yesterdayPrice > 0) this.change = ((double) (newPrice - yesterdayPrice) / yesterdayPrice) * 100;
        }
    }

    public static class ReportEntry implements Serializable {
        private final String commodityName;
        private final String message;
        private final long timestamp;
        private boolean isRead;

        public ReportEntry(String commodityName, String message, boolean isRead) {
            this.commodityName = commodityName;
            this.message = message;
            this.isRead = isRead;
            this.timestamp = System.currentTimeMillis();
        }
        public String getCommodityName() { return commodityName; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
        public boolean isRead() { return isRead; }
        public void setRead(boolean read) { this.isRead = read; }
    }
}
