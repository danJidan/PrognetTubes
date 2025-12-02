package com.commoditylive.server.handler;

import com.commoditylive.common.Message;

public class FilterCommodityHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        response.addData("message", "FILTER_COMMODITY handler not yet implemented");
    }
}

