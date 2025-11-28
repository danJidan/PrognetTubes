package com.pasarlive.server.handler;

import com.pasarlive.common.Message;

public class UpdatePriceHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        response.addData("message", "UPDATE_PRICE handler not yet implemented");
    }
}
