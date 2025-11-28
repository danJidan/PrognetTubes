package com.pasarlive.server.handler;

import com.pasarlive.common.Message;

public class DeleteCommodityHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        response.addData("message", "DELETE_COMMODITY handler not yet implemented");
    }
}
