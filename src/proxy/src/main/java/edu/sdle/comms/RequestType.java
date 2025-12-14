package edu.sdle.comms;

public enum RequestType {
    GET,
    PUT;

    public static RequestType parse(String type) {
        for (var t : RequestType.values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        return null;
    }
}
