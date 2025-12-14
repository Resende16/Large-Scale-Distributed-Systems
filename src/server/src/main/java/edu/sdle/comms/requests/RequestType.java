package edu.sdle.comms.requests;

public enum RequestType {
    GET,
    PUT,
    JOIN,
    LEAVE,
    STATE,
    GOSSIP;

    public static RequestType fromString(String type) {
        for (var t : RequestType.values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        return null;
    }
}
