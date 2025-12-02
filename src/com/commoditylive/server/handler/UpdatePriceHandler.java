package com.commoditylive.server.handler;

import com.commoditylive.common.Message;
import com.commoditylive.server.ServerDataManager;
import com.commoditylive.server.ServerMain;
import com.commoditylive.model.MarketDataModel.CommodityData;

public class UpdatePriceHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        try {
            String id = (String) request.getData("id");
            int newPrice = (Integer) request.getData("newPrice");

            CommodityData updated = ServerDataManager.getInstance().updatePrice(id, newPrice);

            if (updated != null) {
                response.setSuccess(true);
                
                // BROADCAST: Kabari semua orang harga berubah!
                Message broadcastMsg = new Message(Message.Type.BROADCAST, Message.Action.PRICE_UPDATE);
                broadcastMsg.addData("commodityName", updated.name);
                broadcastMsg.addData("newPrice", newPrice);
                ServerMain.broadcast(broadcastMsg);
            } else {
                response.setSuccess(false);
                response.setErrorMessage("ID Barang tidak ditemukan");
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("Error update: " + e.getMessage());
        }
    }
}
