package com.radaeli.copycatreplace.network;

/** Selection geometry; material, permissions and server limits apply independently. */
public enum SelectionMode {
    CONNECTED, PLANE, ALL_TYPES;
    public static SelectionMode fromId(int id) {
        return id >= 0 && id < values().length ? values()[id] : CONNECTED;
    }
    public SelectionMode cycle(int delta) {
        return values()[Math.floorMod(ordinal() + delta, values().length)];
    }
}
