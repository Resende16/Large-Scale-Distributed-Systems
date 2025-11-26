package edu.sdle.comms;

public enum ReplyCode {
    SUCCESS(0),
    FAILURE(1);
    //todo

    private final int code;

    ReplyCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ReplyCode fromCode(int code) {
        for (ReplyCode replyCode : ReplyCode.values()) {
            if (replyCode.getCode() == code) {
                return replyCode;
            }
        }
        return FAILURE; // default
    }
}
