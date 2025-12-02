package com.commoditylive.server.handler;

import com.commoditylive.common.Message;
import com.commoditylive.server.ServerDataManager;

public class UpdateReportStatusHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        try {
            String commodityName = (String) request.getData("commodityName");
            String message = (String) request.getData("message");
            Boolean isRead = (Boolean) request.getData("isRead");
            
            if (commodityName == null || message == null || isRead == null) {
                response.setSuccess(false);
                response.setErrorMessage("Data tidak lengkap");
                return;
            }
            
            boolean success = ServerDataManager.getInstance()
                .updateReportStatus(commodityName, message, isRead);
            
            if (success) {
                response.setSuccess(true);
                response.addData("message", "Status laporan berhasil diupdate");
            } else {
                response.setSuccess(false);
                response.setErrorMessage("Gagal update status laporan");
            }
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("Error: " + e.getMessage());
        }
    }
}

