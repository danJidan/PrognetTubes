# Data Model & Persistence Layer

This document covers how CommodityLive structures its market data on both the client and server sides, including the SQLite-backed persistence managers.

## Client-Side Model (`src/com/commoditylive/model/MarketDataModel.java`)
- Maintains a `Map<String, CommodityData>` so the UI can lookup or sort commodities quickly.
- Each `CommodityData` keeps a bounded price history for charts and calculates daily percentage changes.

```java
public class MarketDataModel implements Serializable {
    private final Map<String, CommodityData> commodities = new HashMap<>();

    public void setAllCommodities(List<CommodityData> list) {
        commodities.clear();
        for (CommodityData c : list) {
            commodities.put(c.id, c);
        }
    }

    public static class CommodityData implements Serializable {
        public final String id;
        public int price;
        public double change;
        public final List<Integer> priceHistory = new ArrayList<>();

        public void updatePrice(int newPrice) {
            priceHistory.add(newPrice);
            while (priceHistory.size() > 7) priceHistory.remove(0);
            if (yesterdayPrice > 0) {
                this.change = ((double)(newPrice - yesterdayPrice) / yesterdayPrice) * 100;
            }
        }
    }
}
```

## Database Bridge (`src/com/commoditylive/server/DatabaseManager.java`)
- Wraps a single SQLite connection (`jdbc:sqlite:commoditylive.db`).
- Bootstraps schema for commodities, reports, and price history, then seeds reference data if the DB is empty.
- CRUD helpers expose `CommodityData` objects directly to the rest of the server.

```java
private DatabaseManager() {
    Class.forName("org.sqlite.JDBC");
    connection = DriverManager.getConnection(DB_URL);
    initializeTables();
    initializeSampleData();
}

public CommodityData updatePrice(String id, int newPrice) {
    String updateQuery = "UPDATE commodities SET yesterday_price = price, price = ? WHERE id = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {
        pstmt.setInt(1, newPrice);
        pstmt.setString(2, id);
        if (pstmt.executeUpdate() > 0) {
            addPriceHistory(id, newPrice);
            return mapCommodity(id);
        }
    }
    return null;
}
```

## Server Facade (`src/com/commoditylive/server/ServerDataManager.java`)
- Thin singleton over `DatabaseManager` so handlers never touch JDBC directly.
- Exposes commodity CRUD plus report ingestion/acknowledgement and is the only place that closes the DB connection when the server stops.

```java
public class ServerDataManager {
    private static ServerDataManager instance;
    private final DatabaseManager db = DatabaseManager.getInstance();

    public List<CommodityData> getAllCommodities() {
        return db.getAllCommodities();
    }

    public CommodityData addCommodity(String id, String name, int price, String category) {
        return db.addCommodity(id, name, price, category);
    }

    public boolean deleteCommodity(String id) {
        return db.deleteCommodity(id);
    }
}
```

### Typical Request Flow
1. A handler (e.g., `UpdatePriceHandler`) validates the incoming payload.
2. Handler calls the corresponding `ServerDataManager` method.
3. Updated `CommodityData` is returned, broadcast to clients, and mirrored in the `MarketDataModel` on each dashboard.
