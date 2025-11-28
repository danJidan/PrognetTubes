package com.pasarlive.server.handler;

import com.pasarlive.common.Message;

public class GetCommodityListHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        response.addData("message", "GET_COMMODITY_LIST handler not yet implemented");
    }
}
