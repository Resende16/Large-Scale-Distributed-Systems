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

    public static ReplyCode fromCode(String codeStr) {
        try {
            int code = Integer.parseInt(codeStr);
            return fromCode(code);
        }  catch (NumberFormatException e) {
            return FAILURE;
        }
    }
}