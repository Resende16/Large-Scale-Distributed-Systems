package edu.sdle.comms;

public enum RequestType {
    GET,
    PUT;

    public static RequestType fromString(String type) {
        for (var t : RequestType.values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        return null; // purposefully, to avoid an exception
    }
}