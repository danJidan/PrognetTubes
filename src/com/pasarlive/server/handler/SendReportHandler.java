package com.pasarlive.server.handler;

import com.pasarlive.common.Message;
import com.pasarlive.server.ServerDataManager;

public class SendReportHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        String msg = (String) request.getData("reportText");
        String commName = (String) request.getData("commodityName");
        if(commName == null) commName = "Umum";

        // Simpan ke database server
        ServerDataManager.getInstance().addReport(commName, msg);

        response.setSuccess(true);
        response.addData("message", "Laporan diterima server.");
    }
}