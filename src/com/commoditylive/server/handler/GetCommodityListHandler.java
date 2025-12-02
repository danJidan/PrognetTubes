package com.commoditylive.server.handler;

import com.commoditylive.common.Message;
import com.commoditylive.model.MarketDataModel;
import com.commoditylive.server.ServerDataManager;
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
