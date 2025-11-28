package com.pasarlive.server.handler;

import com.pasarlive.common.Message;

public class CreateCommodityHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        response.addData("message", "CREATE_COMMODITY handler not yet implemented");
    }
}
