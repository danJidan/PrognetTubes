package com.commoditylive.server.handler;

import com.commoditylive.common.Message;
import com.commoditylive.server.ServerDataManager;
import com.commoditylive.server.ServerMain;

public class DeleteCommodityHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        try {
            String id = (String) request.getData("id");
            
            if (id == null || id.trim().isEmpty()) {
                response.setSuccess(false);
                response.setErrorMessage("ID tidak valid");
                return;
            }
            
            boolean success = ServerDataManager.getInstance().deleteCommodity(id);
            
            if (success) {
                response.setSuccess(true);
                response.addData("message", "Berhasil menghapus komoditas dengan ID: " + id);
                
                // Broadcast agar tabel user lain juga terhapus (refresh)
                Message broadcastMsg = new Message(Message.Type.BROADCAST, Message.Action.PRICE_UPDATE);
                broadcastMsg.addData("commodityName", "DELETED"); 
                ServerMain.broadcast(broadcastMsg);
            } else {
                response.setSuccess(false);
                response.setErrorMessage("Gagal menghapus: ID '" + id + "' tidak ditemukan");
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("Error saat menghapus: " + e.getMessage());
        }
    }
}
