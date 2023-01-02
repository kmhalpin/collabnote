package com.collabnote.socket;

public enum Type {
    CARET(1),       // Sync caret position
    INSERT(2),      // Sync inserted text
    DELETE(3),      // Sync deleted text
    DONE(4),        // Not used
    SHARE(5),       // Share document to server, Client upload CRDT -> Server return share ID -> sync
    CONNECT(6);     // Connect to shared document in server, Client connect to server and share ID -> Server send CRDT -> sync

    public final int type;

    private Type(int type) {
        this.type = type;
    }
}
