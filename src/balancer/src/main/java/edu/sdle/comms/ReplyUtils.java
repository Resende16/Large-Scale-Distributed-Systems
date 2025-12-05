package edu.sdle.comms;

import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

import static edu.sdle.comms.ReplyCode.*;

public class ReplyUtils {

    // Represents a failure of some sort, i.e.
    // the request did not succeed
    public static ZMsg failure() {
        ZMsg msg = new ZMsg();
        msg.add(FAILURE.toString());
        return msg;
    }

    // Represents a successful request, where
    // no payload is attached to the reply
    public static ZMsg success() {
        ZMsg msg = new ZMsg();
        msg.add(SUCCESS.toString());
        return msg;
    }

    // A successful request that returns
    // a payload as a String
    public static ZMsg success(String payload) {
        ZMsg msg = new ZMsg();
        msg.add(SUCCESS.toString());
        msg.add(payload);
        return msg;
    }

    // A successful request that returns
    // a payload as a ZFrame
    public static ZMsg success(ZFrame payload) {
        ZMsg msg = new ZMsg();
        msg.add(SUCCESS.toString());
        msg.add(payload);
        return msg;
    }

    // Parse a ZMsg reply message and return
    // true if the request succeeded
    public static boolean didSucceed(ZMsg reply) {
        if (reply == null || reply.isEmpty())
            return false;

        var codeStr = reply.pop().toString();
        var code = ReplyCode.fromCode(codeStr);
        return code == SUCCESS;
    }
}