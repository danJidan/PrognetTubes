package com.pasarlive.server.handler;

import com.pasarlive.common.Message;

public class SendReportHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        response.addData("message", "SEND_REPORT handler not yet implemented");
    }
}
