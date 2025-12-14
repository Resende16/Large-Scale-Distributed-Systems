package edu.sdle.comms;

public enum RequestType {
    GET,
    PUT,
    REPLICA_GET,
    REPLICA_PUT,
    STATE,
    GOSSIP;

    public static RequestType parse(String type) {
        for (var t : RequestType.values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        return null;
    }
}
