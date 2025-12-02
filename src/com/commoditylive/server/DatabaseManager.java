package com.commoditylive.server;

import com.commoditylive.model.MarketDataModel.CommodityData;
import com.commoditylive.model.MarketDataModel.ReportEntry;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Database Manager menggunakan SQLite untuk persistensi data
 * Menggantikan in-memory storage dengan database
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:commoditylive.db";

    private DatabaseManager() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            initializeTables();
            initializeSampleData();
            System.out.println("✅ Database connected: commoditylive.db");
        } catch (Exception e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Inisialisasi tabel database
     */
    private void initializeTables() throws SQLException {
        String createCommoditiesTable = """
            CREATE TABLE IF NOT EXISTS commodities (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                price INTEGER NOT NULL,
                yesterday_price INTEGER NOT NULL,
                category TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createReportsTable = """
            CREATE TABLE IF NOT EXISTS reports (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                commodity_name TEXT NOT NULL,
                message TEXT NOT NULL,
                is_read INTEGER DEFAULT 0,
                timestamp BIGINT NOT NULL
            )
        """;

        String createPriceHistoryTable = """
            CREATE TABLE IF NOT EXISTS price_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                commodity_id TEXT NOT NULL,
                price INTEGER NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (commodity_id) REFERENCES commodities(id) ON DELETE CASCADE
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createCommoditiesTable);
            stmt.execute(createReportsTable);
            stmt.execute(createPriceHistoryTable);
        }
    }

    /**
     * Inisialisasi data sample jika database kosong
     */
    private void initializeSampleData() throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM commodities";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkQuery)) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Database kosong, tambahkan data sample
                addCommodity("COMM-001", "Beras Premium", 15000, "Bahan Pokok");
                addCommodity("COMM-002", "Cabai Merah", 45000, "Sayuran");
                addCommodity("COMM-003", "Gula Pasir", 17000, "Bahan Pokok");
                addCommodity("COMM-004", "Minyak Goreng", 18000, "Bahan Pokok");
                addCommodity("COMM-005", "Ayam Potong", 38000, "Lauk Pauk");
                addCommodity("COMM-006", "Tomat Merah", 12000, "Sayuran");
                addCommodity("COMM-007", "Bawang Merah", 42000, "Bumbu");
                addCommodity("COMM-008", "Telur Ayam", 28000, "Lauk Pauk");
                System.out.println("✅ Sample data initialized");
            }
        }
    }

    // ============================================
    // COMMODITY OPERATIONS
    // ============================================

    public List<CommodityData> getAllCommodities() {
        List<CommodityData> list = new ArrayList<>();
        String query = "SELECT * FROM commodities ORDER BY name";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                int price = rs.getInt("price");
                int yesterdayPrice = rs.getInt("yesterday_price");
                String category = rs.getString("category");
                
                double change = yesterdayPrice > 0 
                    ? ((double)(price - yesterdayPrice) / yesterdayPrice) * 100 
                    : 0.0;
                
                CommodityData commodity = new CommodityData(id, name, price, change, category);
                commodity.yesterdayPrice = yesterdayPrice;
                
                // Load price history
                List<Integer> history = getPriceHistory(id);
                for (int p : history) {
                    commodity.addPriceHistory(p);
                }
                
                list.add(commodity);
            }
        } catch (SQLException e) {
            System.err.println("Error getting commodities: " + e.getMessage());
        }
        
        return list;
    }

    public CommodityData addCommodity(String id, String name, int price, String category) {
        if (id == null) {
            id = "COMM-" + System.currentTimeMillis();
        }
        
        String insertQuery = "INSERT INTO commodities (id, name, price, yesterday_price, category) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
            pstmt.setString(1, id);
            pstmt.setString(2, name);
            pstmt.setInt(3, price);
            pstmt.setInt(4, price);
            pstmt.setString(5, category);
            pstmt.executeUpdate();
            
            // Add initial price history
            addPriceHistory(id, price);
            
            System.out.println("✅ Commodity added: " + name);
            return new CommodityData(id, name, price, 0.0, category);
            
        } catch (SQLException e) {
            System.err.println("Error adding commodity: " + e.getMessage());
            return null;
        }
    }

    public CommodityData updatePrice(String id, int newPrice) {
        String updateQuery = "UPDATE commodities SET yesterday_price = price, price = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {
            pstmt.setInt(1, newPrice);
            pstmt.setString(2, id);
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                addPriceHistory(id, newPrice);
                
                // Get updated commodity
                String selectQuery = "SELECT * FROM commodities WHERE id = ?";
                try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
                    selectStmt.setString(1, id);
                    ResultSet rs = selectStmt.executeQuery();
                    
                    if (rs.next()) {
                        String name = rs.getString("name");
                        int price = rs.getInt("price");
                        int yesterdayPrice = rs.getInt("yesterday_price");
                        String category = rs.getString("category");
                        
                        double change = yesterdayPrice > 0 
                            ? ((double)(price - yesterdayPrice) / yesterdayPrice) * 100 
                            : 0.0;
                        
                        CommodityData commodity = new CommodityData(id, name, price, change, category);
                        commodity.yesterdayPrice = yesterdayPrice;
                        
                        System.out.println("✅ Price updated: " + name + " -> Rp " + newPrice);
                        return commodity;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating price: " + e.getMessage());
        }
        
        return null;
    }

    public boolean deleteCommodity(String id) {
        String deleteQuery = "DELETE FROM commodities WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(deleteQuery)) {
            pstmt.setString(1, id);
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Commodity deleted: " + id);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting commodity: " + e.getMessage());
        }
        
        return false;
    }

    public boolean isCommodityNameExists(String name) {
        String query = "SELECT COUNT(*) FROM commodities WHERE LOWER(name) = LOWER(?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, name.trim());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking commodity name: " + e.getMessage());
        }
        
        return false;
    }

    // ============================================
    // PRICE HISTORY OPERATIONS
    // ============================================

    private void addPriceHistory(String commodityId, int price) {
        String insertQuery = "INSERT INTO price_history (commodity_id, price) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
            pstmt.setString(1, commodityId);
            pstmt.setInt(2, price);
            pstmt.executeUpdate();
            
            // Keep only last 7 entries
            String deleteOldQuery = """
                DELETE FROM price_history 
                WHERE commodity_id = ? 
                AND id NOT IN (
                    SELECT id FROM price_history 
                    WHERE commodity_id = ? 
                    ORDER BY timestamp DESC 
                    LIMIT 7
                )
            """;
            
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteOldQuery)) {
                deleteStmt.setString(1, commodityId);
                deleteStmt.setString(2, commodityId);
                deleteStmt.executeUpdate();
            }
            
        } catch (SQLException e) {
            System.err.println("Error adding price history: " + e.getMessage());
        }
    }

    private List<Integer> getPriceHistory(String commodityId) {
        List<Integer> history = new ArrayList<>();
        String query = "SELECT price FROM price_history WHERE commodity_id = ? ORDER BY timestamp ASC LIMIT 7";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, commodityId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                history.add(rs.getInt("price"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting price history: " + e.getMessage());
        }
        
        return history;
    }

    // ============================================
    // REPORT OPERATIONS
    // ============================================

    public void addReport(String commodityName, String message) {
        String insertQuery = "INSERT INTO reports (commodity_name, message, is_read, timestamp) VALUES (?, ?, 0, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
            pstmt.setString(1, commodityName);
            pstmt.setString(2, message);
            pstmt.setLong(3, System.currentTimeMillis());
            pstmt.executeUpdate();
            
            System.out.println("✅ Report added: " + commodityName);
            
        } catch (SQLException e) {
            System.err.println("Error adding report: " + e.getMessage());
        }
    }

    public List<ReportEntry> getAllReports() {
        List<ReportEntry> list = new ArrayList<>();
        String query = "SELECT * FROM reports ORDER BY timestamp DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String commodityName = rs.getString("commodity_name");
                String message = rs.getString("message");
                boolean isRead = rs.getInt("is_read") == 1;
                
                ReportEntry report = new ReportEntry(commodityName, message, isRead);
                list.add(report);
            }
        } catch (SQLException e) {
            System.err.println("Error getting reports: " + e.getMessage());
        }
        
        return list;
    }

    public boolean updateReportStatus(String commodityName, String message, boolean isRead) {
        String updateQuery = "UPDATE reports SET is_read = ? WHERE commodity_name = ? AND message = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {
            pstmt.setInt(1, isRead ? 1 : 0);
            pstmt.setString(2, commodityName);
            pstmt.setString(3, message);
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Report status updated");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating report status: " + e.getMessage());
        }
        
        return false;
    }

    // ============================================
    // CLOSE CONNECTION
    // ============================================

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }
}

