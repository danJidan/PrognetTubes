package com.commoditylive.server.handler;

import com.commoditylive.common.Message;
import com.commoditylive.model.MarketDataModel;
import com.commoditylive.server.ServerDataManager;
import java.util.List; // Penting: Jangan lupa import ini

public class GetReportsHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        // PERBAIKAN: Kita tulis tipe datanya secara lengkap (tanpa 'var')
        List<MarketDataModel.ReportEntry> list = ServerDataManager.getInstance().getAllReports();
        
        response.setSuccess(true);
        response.addData("list", list);
    }
}
