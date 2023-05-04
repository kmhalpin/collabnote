package com.collabnote.socket;

public enum Type {
    CARET(1), // Sync caret position
    INSERT(2), // Sent by client: Sync inserted text
    DELETE(3), // Sent by client: Sync deleted text
    DONE(4), // Used to acknowledge operations
    SHARE(5), // Share document to server, Client upload CRDT -> Server return share ID ->
              // sync
    CONNECT(6), // Connect to shared document in server, Client connect to server and share ID
                // -> Server send CRDT -> sync

    // these type used in garbage collect system
    RECOVER(7), // Recover operation from server
    GC(8), // Garbage collect operation
    RECONNECT(9); // used when reconnecting offline document to online

    // ACKINSERT(9), // Sent by server: Acknowledge inserted text
    // ACKDELETE(10); // Sent by server: Acknowledge deleted text

    public final int type;

    private Type(int type) {
        this.type = type;
    }
}
