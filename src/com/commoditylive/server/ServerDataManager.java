package com.commoditylive.server;

import com.commoditylive.model.MarketDataModel.CommodityData;
import com.commoditylive.model.MarketDataModel.ReportEntry;
import java.util.List;

/**
 * ServerDataManager sekarang delegate ke DatabaseManager
 * untuk persistensi data menggunakan SQLite
 */
public class ServerDataManager {
    private static ServerDataManager instance;
    private final DatabaseManager db;

    public static synchronized ServerDataManager getInstance() {
        if (instance == null) {
            instance = new ServerDataManager();
        }
        return instance;
    }

    private ServerDataManager() {
        db = DatabaseManager.getInstance();
    }

    public List<CommodityData> getAllCommodities() {
        return db.getAllCommodities();
    }

    public CommodityData addCommodity(String id, String name, int price, String category) {
        return db.addCommodity(id, name, price, category);
    }

    public CommodityData updatePrice(String id, int newPrice) {
        return db.updatePrice(id, newPrice);
    }

    public boolean deleteCommodity(String id) {
        return db.deleteCommodity(id);
    }

    public boolean isCommodityNameExists(String name) {
        return db.isCommodityNameExists(name);
    }

    public void addReport(String commodityName, String message) {
        db.addReport(commodityName, message);
    }

    public List<ReportEntry> getAllReports() {
        return db.getAllReports();
    }

    public boolean updateReportStatus(String commodityName, String message, boolean isRead) {
        return db.updateReportStatus(commodityName, message, isRead);
    }

    public void close() {
        db.close();
    }
}

