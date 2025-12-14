package edu.sdle.comms;

public enum ReplyCode {
    SUCCESS("0"),
    FAILURE("1"),
    FAILURE_TIMEOUT("2"),
    FAILURE_BUSY("3");

    private final String _code;

    ReplyCode(String code) {
        _code = code;
    }

    public String code() {
        return _code;
    }

    public static ReplyCode fromCode(String code) {
        for (ReplyCode replyCode : ReplyCode.values()) {
            if (replyCode.code().equals(code)) {
                return replyCode;
            }
        }
        return FAILURE;
    }
}
