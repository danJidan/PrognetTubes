package com.pasarlive.server.handler;

import com.pasarlive.common.Message;
import com.pasarlive.server.ServerDataManager;
import com.pasarlive.server.ServerMain;
import com.pasarlive.model.MarketDataModel.CommodityData;

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