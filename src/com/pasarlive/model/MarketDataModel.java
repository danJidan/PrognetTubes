package com.pasarlive.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MarketDataModel {
    private final Map<String, CommodityData> commodities = new HashMap<>();
    private final List<ReportEntry> userReports = new ArrayList<>();
    private int commoditySequence = 9;

    public MarketDataModel() {
        initializeSampleData();
        initializeSampleReports();
    }

    public List<CommodityData> getSortedCommodities() {
        List<CommodityData> list = new ArrayList<>(commodities.values());
        list.sort(Comparator.comparing(c -> c.name.toLowerCase(Locale.ROOT)));
        return list;
    }

    public CommodityData getCommodityById(String id) {
        return commodities.get(id);
    }

    public CommodityData getDefaultCommodity() {
        List<CommodityData> sorted = getSortedCommodities();
        return sorted.isEmpty() ? null : sorted.get(0);
    }

    public CommodityData addCommodity(String name, int price, String category) {
        String newId = String.format("COMM-%03d", commoditySequence++);
        CommodityData commodity = new CommodityData(newId, name, price, 0.0, category);
        commodity.addPriceHistory(price, price, price, price, price, price);
        commodity.setYesterdayPrice(price);
        commodities.put(newId, commodity);
        return commodity;
    }

    public CommodityData updateCommodity(String commodityId, String name, int price, String category) {
        CommodityData commodity = commodities.get(commodityId);
        if (commodity == null) {
            return null;
        }
        commodity.name = name;
        if (category != null) {
            commodity.category = category;
        }
        commodity.updatePrice(price);
        return commodity;
    }

    public CommodityData deleteCommodity(String commodityId) {
        return commodities.remove(commodityId);
    }

    public void addReportEntry(ReportEntry entry) {
        userReports.add(0, entry);
    }

    public List<ReportEntry> getReportsSnapshot() {
        return new ArrayList<>(userReports);
    }

    public boolean updateReportReadState(int index, boolean read) {
        if (index < 0 || index >= userReports.size()) {
            return false;
        }
        userReports.get(index).setRead(read);
        return true;
    }

    private void initializeSampleData() {
        CommodityData berasPremium = new CommodityData("COMM-001", "Beras Premium", 15000, 3.4, "Bahan Pokok");
        berasPremium.addPriceHistory(11000, 11500, 12000, 13000, 14500, 15000);
        berasPremium.setYesterdayPrice(14500);
        commodities.put(berasPremium.id, berasPremium);

        CommodityData cabaiMerah = new CommodityData("COMM-002", "Cabai Merah", 45000, -10.0, "Sayuran");
        cabaiMerah.addPriceHistory(48000, 49000, 50000, 48500, 50000, 45000);
        cabaiMerah.setYesterdayPrice(50000);
        commodities.put(cabaiMerah.id, cabaiMerah);

        CommodityData gulaPasir = new CommodityData("COMM-003", "Gula Pasir", 17000, 0.0, "Bahan Pokok");
        gulaPasir.addPriceHistory(17000, 17000, 17000, 17000, 17000, 17000);
        gulaPasir.setYesterdayPrice(17000);
        commodities.put(gulaPasir.id, gulaPasir);

        CommodityData minyakGoreng = new CommodityData("COMM-004", "Minyak Goreng", 18000, 5.8, "Bahan Pokok");
        minyakGoreng.addPriceHistory(15000, 15500, 16000, 16500, 17000, 18000);
        minyakGoreng.setYesterdayPrice(17000);
        commodities.put(minyakGoreng.id, minyakGoreng);

        CommodityData ayamPotong = new CommodityData("COMM-005", "Ayam Potong", 38000, 0.0, "Lauk Pauk");
        ayamPotong.addPriceHistory(38000, 38000, 38000, 38000, 38000, 38000);
        ayamPotong.setYesterdayPrice(38000);
        commodities.put(ayamPotong.id, ayamPotong);

        CommodityData tomatMerah = new CommodityData("COMM-006", "Tomat Merah", 12000, 2.1, "Sayuran");
        tomatMerah.addPriceHistory(10000, 10500, 11000, 11200, 11750, 12000);
        tomatMerah.setYesterdayPrice(11750);
        commodities.put(tomatMerah.id, tomatMerah);

        CommodityData bawangMerah = new CommodityData("COMM-007", "Bawang Merah", 42000, -3.2, "Bumbu");
        bawangMerah.addPriceHistory(45000, 44000, 43500, 43400, 43500, 42000);
        bawangMerah.setYesterdayPrice(43500);
        commodities.put(bawangMerah.id, bawangMerah);

        CommodityData telurAyam = new CommodityData("COMM-008", "Telur Ayam", 28000, 1.8, "Lauk Pauk");
        telurAyam.addPriceHistory(26000, 26500, 27000, 27200, 27500, 28000);
        telurAyam.setYesterdayPrice(27500);
        commodities.put(telurAyam.id, telurAyam);
    }

    private void initializeSampleReports() {
        addReportEntry(new ReportEntry("Beras Premium", "Harga beras naik di pasar Tebet.", false));
        addReportEntry(new ReportEntry("Cabai Merah", "Diskon besar di pemasok lokal.", true));
        addReportEntry(new ReportEntry("Telur Ayam", "Kualitas menurun, tolong cek pemasok.", false));
    }

    public static class CommodityData {
        public final String id;
        public String name;
        public int price;
        public int yesterdayPrice;
        public double change;
        public String category;
        public final List<Integer> priceHistory;

        CommodityData(String id, String name, int price, double change, String category) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.change = change;
            this.category = category;
            this.priceHistory = new ArrayList<>();
            this.yesterdayPrice = price;
        }

        void setYesterdayPrice(int yesterdayPrice) {
            this.yesterdayPrice = yesterdayPrice;
        }

        void addPriceHistory(int... prices) {
            for (int p : prices) {
                priceHistory.add(p);
            }
        }

        void updatePrice(int newPrice) {
            if (!priceHistory.isEmpty()) {
                yesterdayPrice = priceHistory.get(priceHistory.size() - 1);
            }
            this.price = newPrice;
            priceHistory.add(newPrice);
            if (priceHistory.size() > 6) {
                priceHistory.remove(0);
            }
            if (yesterdayPrice > 0) {
                this.change = ((double) (newPrice - yesterdayPrice) / yesterdayPrice) * 100;
            }
        }
    }

    public static class ReportEntry {
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

        public String getCommodityName() {
            return commodityName;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isRead() {
            return isRead;
        }

        public void setRead(boolean read) {
            this.isRead = read;
        }
    }
}
