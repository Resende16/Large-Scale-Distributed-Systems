package edu.sdle.comms;

public enum RequestType {
    GET,
    PUT,
    COORDINATE_GET,
    COORDINATE_PUT;

    public static RequestType fromString(String type) {
        for (var t : RequestType.values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown request type: " + type);
    }
}
