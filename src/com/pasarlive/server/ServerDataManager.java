package com.pasarlive.server;

import com.pasarlive.model.MarketDataModel; 
import com.pasarlive.model.MarketDataModel.CommodityData;
import com.pasarlive.model.MarketDataModel.ReportEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerDataManager {
    private static ServerDataManager instance;
    private final List<CommodityData> commodities = new CopyOnWriteArrayList<>();
    private final List<ReportEntry> reports = new CopyOnWriteArrayList<>();

    public static synchronized ServerDataManager getInstance() {
        if (instance == null) {
            instance = new ServerDataManager();
        }
        return instance;
    }

    private ServerDataManager() {
        // KITA HARDCODE ID-NYA AGAR SAMA PERSIS DENGAN CLIENT
        // Jangan pakai counter otomatis lagi!
        
        addCommodity("COMM-001", "Beras Premium", 15000, "Bahan Pokok");
        addCommodity("COMM-002", "Cabai Merah", 45000, "Sayuran");
        addCommodity("COMM-003", "Gula Pasir", 17000, "Bahan Pokok");
        addCommodity("COMM-004", "Minyak Goreng", 18000, "Bahan Pokok");
        addCommodity("COMM-005", "Ayam Potong", 38000, "Lauk Pauk");
        addCommodity("COMM-006", "Tomat Merah", 12000, "Sayuran");
        addCommodity("COMM-007", "Bawang Merah", 42000, "Bumbu");
        addCommodity("COMM-008", "Telur Ayam", 28000, "Lauk Pauk");
    }

    public List<CommodityData> getAllCommodities() {
        return new ArrayList<>(commodities);
    }

    // METHOD INI DIUBAH: Sekarang menerima parameter 'id'
    public CommodityData addCommodity(String id, String name, int price, String category) {
        // Jika ID null (misal dari tombol Tambah Admin), baru kita generate
        if (id == null) {
            id = "COMM-" + System.currentTimeMillis(); 
        }
        
        CommodityData newItem = new CommodityData(id, name, price, 0.0, category);
        // Isi history awal agar grafik tidak kosong
        newItem.addPriceHistory(price, price, price, price, price); 
        
        commodities.add(newItem);
        return newItem;
    }

    public CommodityData updatePrice(String id, int newPrice) {
        for (CommodityData c : commodities) {
            if (c.id.equals(id)) {
                c.updatePrice(newPrice);
                return c;
            }
        }
        return null;
    }

    public boolean deleteCommodity(String id) {
        return commodities.removeIf(c -> c.id.equals(id));
    }

    public void addReport(String commodityName, String message) {
        ReportEntry newReport = new ReportEntry(commodityName, message, false);
        reports.add(0, newReport);
    }

    public List<ReportEntry> getAllReports() {
        return new ArrayList<>(reports);
    }
}