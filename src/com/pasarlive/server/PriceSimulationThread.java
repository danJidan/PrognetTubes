package com.pasarlive.server;

import com.pasarlive.common.Message;
import com.pasarlive.model.MarketDataModel.CommodityData;
import java.util.List;
import java.util.Random;

public class PriceSimulationThread extends Thread {
    private boolean running = true;
    private final Random random = new Random();

    @Override
    public void run() {
        System.out.println("📈 SIMULASI HARGA DIMULAI (Update tiap 10 detik)...");

        while (running) {
            try {
                // 1. Tunggu 10 Detik
                Thread.sleep(10000); 

                System.out.println("⚡ [MESIN] Sedang mengacak harga...");
                
                // 2. Ambil data barang
                List<CommodityData> commodities = ServerDataManager.getInstance().getAllCommodities();
                
                if (commodities.isEmpty()) {
                    System.out.println("⚠️ [MESIN] Tidak ada barang untuk diupdate!");
                    continue;
                }

                for (CommodityData c : commodities) {
                    try {
                        // 3. Acak harga
                        int fluctuation = (random.nextInt(11) - 5) * 100; // -500 s.d +500
                        if (fluctuation == 0) fluctuation = (random.nextBoolean() ? 100 : -100);

                        int oldPrice = c.price;
                        int newPrice = oldPrice + fluctuation;
                        if (newPrice < 1000) newPrice = 1000;

                        // 4. Update Database
                        ServerDataManager.getInstance().updatePrice(c.id, newPrice);
                        
                        // LOG DEBUG (Biar kita tahu dia kerja)
                        System.out.println("   -> " + c.name + ": Rp " + oldPrice + " -> Rp " + newPrice);

                        // 5. Broadcast
                        Message broadcastMsg = new Message(Message.Type.BROADCAST, Message.Action.PRICE_UPDATE);
                        broadcastMsg.addData("commodityName", c.name);
                        broadcastMsg.addData("newPrice", newPrice);
                        ServerMain.broadcast(broadcastMsg);
                        
                        Thread.sleep(50); // Jeda dikit
                        
                    } catch (Exception e) {
                        System.err.println("❌ Error saat update barang " + c.name + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                System.out.println("✅ [MESIN] Update Selesai. Menunggu 10 detik lagi...\n");

            } catch (InterruptedException e) {
                running = false;
                System.out.println("Mesin simulasi berhenti.");
            } catch (Exception e) {
                System.err.println("❌ CRITICAL ERROR DI MESIN SIMULASI: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void stopSimulation() {
        running = false;
    }
}