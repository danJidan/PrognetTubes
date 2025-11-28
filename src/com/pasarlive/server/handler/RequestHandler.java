package com.pasarlive.server.handler;

import com.pasarlive.common.Message;

/**
 * Contract for handling a single Message action.
 */
public interface RequestHandler {
    void handle(Message request, Message response);
}
