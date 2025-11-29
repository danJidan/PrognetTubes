package com.pasarlive.server.handler;

import com.pasarlive.common.Message;
import com.pasarlive.model.MarketDataModel.CommodityData;
import com.pasarlive.server.ServerDataManager;
import com.pasarlive.server.ServerMain;

public class CreateCommodityHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        try {
            String name = (String) request.getData("name");
            int price = (Integer) request.getData("price");
            String category = (String) request.getData("category");

            ServerDataManager.getInstance().addCommodity(null, name, price, category);

            response.setSuccess(true);
            response.addData("message", "Berhasil menambah komoditas");

            // Broadcast ke semua user kalau ada barang baru
            Message broadcastMsg = new Message(Message.Type.BROADCAST, Message.Action.PRICE_UPDATE);
            broadcastMsg.addData("commodityName", name); // Trigger refresh
            ServerMain.broadcast(broadcastMsg);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("Gagal tambah data: " + e.getMessage());
        }
    }
}