package com.pasarlive.server.handler;

import com.pasarlive.common.Message;
import com.pasarlive.model.MarketDataModel;
import com.pasarlive.server.ServerDataManager;
import java.util.List; // Penting: Jangan lupa import ini

public class GetCommodityListHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        // PERBAIKAN: Kita tulis tipe datanya secara lengkap (tanpa 'var')
        List<MarketDataModel.CommodityData> list = ServerDataManager.getInstance().getAllCommodities();
        
        response.setSuccess(true);
        response.addData("list", list);
    }
}