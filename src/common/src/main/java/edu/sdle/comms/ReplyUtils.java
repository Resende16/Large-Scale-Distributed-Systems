package edu.sdle.comms;

import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

import static edu.sdle.comms.ReplyCode.*;

public class ReplyUtils {

    private static ZMsg makeReply(ReplyCode code) {
        ZMsg msg = new ZMsg();
        msg.add(code.code());
        return msg;
    }

    public static ZMsg failure() {
        return makeReply(FAILURE);
    }

    // Represents a request failure
    public static ZMsg failure(ReplyCode code) {
        return makeReply(code);
    }

    // Represents a successful request
    public static ZMsg success() {
        return makeReply(SUCCESS);
    }

    // A successful request that returns
    // a payload as a String
    public static ZMsg success(String payload) {
        ZMsg msg = new ZMsg();
        msg.add(SUCCESS.code());
        msg.add(payload);
        return msg;
    }

    // A successful request that returns
    // a payload as a ZFrame
    public static ZMsg success(ZFrame payload) {
        ZMsg msg = new ZMsg();
        msg.add(SUCCESS.code());
        msg.add(payload);
        return msg;
    }


    public static boolean didSucceed(ZMsg reply) {
        if (reply == null || reply.isEmpty())
            return false;

        var codeStr = reply.pop().toString();
        var code = ReplyCode.fromCode(codeStr);
        return code == SUCCESS;
    }


}
