package com.pasarlive.server.handler;

import com.pasarlive.common.Message;
import com.pasarlive.server.ServerDataManager;
import com.pasarlive.server.ServerMain;

public class DeleteCommodityHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        String id = (String) request.getData("id");
        
        boolean success = ServerDataManager.getInstance().deleteCommodity(id);
        
        if (success) {
            response.setSuccess(true);
            // Broadcast agar tabel user lain juga terhapus (refresh)
            Message broadcastMsg = new Message(Message.Type.BROADCAST, Message.Action.PRICE_UPDATE);
            broadcastMsg.addData("commodityName", "DELETED"); 
            ServerMain.broadcast(broadcastMsg);
        } else {
            response.setSuccess(false);
            response.setErrorMessage("Gagal menghapus, ID tidak ditemukan.");
        }
    }
}