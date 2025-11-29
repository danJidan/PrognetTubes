package com.pasarlive.server.handler;

import com.pasarlive.common.Message;
import com.pasarlive.model.MarketDataModel;
import com.pasarlive.server.ServerDataManager;
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