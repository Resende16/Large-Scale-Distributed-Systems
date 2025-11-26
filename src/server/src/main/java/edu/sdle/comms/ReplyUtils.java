package edu.sdle.comms;

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
    // a payload as a byte array
    public static ZMsg success(byte[] payload) {
        ZMsg msg = new ZMsg();
        msg.add(SUCCESS.toString());
        msg.add(payload);
        return msg;
    }
}
