package com.commoditylive.server.handler;

import com.commoditylive.common.Message;

/**
 * Contract for handling a single Message action.
 */
public interface RequestHandler {
    void handle(Message request, Message response);
}

