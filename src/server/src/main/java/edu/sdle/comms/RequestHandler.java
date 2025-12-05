package edu.sdle.comms;

import edu.sdle.UUIDUtils;
import edu.sdle.storage.Storage;
import org.zeromq.ZMsg;

import java.util.*;
import java.util.concurrent.*;

public class RequestHandler {
    private final Storage storage;

    public RequestHandler(Storage storage) {
        this.storage = storage;
    }

    public ZMsg handleRequest(ZMsg msg) {
        var typeStr = msg.pop().toString();
        var type = RequestType.fromString(typeStr);

        if (type == null)
            // Invalid type
            return ReplyUtils.failure();

        // Parse args + hand off to handler
        switch (type) {
            case GET -> {
                var key = parseKey(msg);
                if (key != null)
                    return handleGetRequest(key);
            }
            case PUT -> {
                var key = parseKey(msg);
                var value = msg.pop();
                if (key != null && value != null)
                    return handlePutRequest(key, value.toString());
            }
        }

        // Otherwise malformed
        return ReplyUtils.failure();
    }

    public UUID parseKey(ZMsg msg) {
        if (msg.isEmpty())
            return null;

        var bytes = msg.pop().getData();
        if (bytes.length != 16)
            // Invalid UUID
            return null;

        return UUIDUtils.uuidFromBytes(bytes);
    }

    public ZMsg handleGetRequest(UUID key) {
        var payload = storage.getAsJson(key);

        if (payload == null || payload.isEmpty())
            return ReplyUtils.failure();
        else
            return ReplyUtils.success(payload);
    }

    public ZMsg handlePutRequest(UUID key, String value) {
        if (storage.putJson(key, value))
            return ReplyUtils.success();
        else
            return ReplyUtils.failure();
    }
}