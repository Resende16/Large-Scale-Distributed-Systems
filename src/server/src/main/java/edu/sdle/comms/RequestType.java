package edu.sdle.comms;

public enum RequestType {
    GET,
    PUT,
    JOIN,
    LEAVE;

    public static RequestType fromString(String type) {
        for (var t : RequestType.values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        return null;
    }
}
