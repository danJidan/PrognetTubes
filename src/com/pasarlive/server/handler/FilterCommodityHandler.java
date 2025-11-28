package com.pasarlive.server.handler;

import com.pasarlive.common.Message;

public class FilterCommodityHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        response.addData("message", "FILTER_COMMODITY handler not yet implemented");
    }
}
