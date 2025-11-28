package com.pasarlive.server.handler;

import com.pasarlive.common.Message;

public class GetReportsHandler implements RequestHandler {
    @Override
    public void handle(Message request, Message response) {
        response.addData("message", "GET_REPORTS handler not yet implemented");
    }
}
