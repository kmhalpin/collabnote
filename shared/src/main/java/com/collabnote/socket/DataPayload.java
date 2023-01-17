package com.collabnote.socket;

import java.io.Serializable;

import com.collabnote.crdt.CRDTItem;

public class DataPayload implements Serializable {
    private String agent;
    private Type type;
    private String shareID;
    private CRDTItem crdtItem;
    private int caretIndex;

    private CRDTItem[] removes;
    private CRDTItem[] changes;

    public static DataPayload insertPayload(String shareID, CRDTItem crdtItem) {
        return new DataPayload(Type.INSERT, shareID, crdtItem, 0);
    }

    public static DataPayload deletePayload(String shareID, CRDTItem crdtItem) {
        return new DataPayload(Type.DELETE, shareID, crdtItem, 0);
    }

    public static DataPayload caretPayload(String shareID, int caretIndex) {
        return new DataPayload(Type.CARET, shareID, null, caretIndex);
    }

    public static DataPayload ackDeletePayload(String shareID, CRDTItem[] removes, CRDTItem[] changes) {
        DataPayload data = new DataPayload(Type.ACKDELETE, shareID, null, 0);
        data.setRemoves(removes);
        data.setChanges(changes);
        return data;
    }

    public static DataPayload ackInsertPayload(String shareID, CRDTItem crdtItem) {
        return new DataPayload(Type.ACKINSERT, shareID, crdtItem, 0);
    }

    public DataPayload(Type type, String shareID, CRDTItem crdtItem, int caretIndex) {
        this.type = type;
        this.shareID = shareID;
        this.crdtItem = crdtItem;
        this.caretIndex = caretIndex;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public Type getType() {
        return type;
    }

    public String getShareID() {
        return shareID;
    }

    public CRDTItem getCrdtItem() {
        return crdtItem;
    }

    public int getCaretIndex() {
        return caretIndex;
    }

    public CRDTItem[] getRemoves() {
        return removes;
    }

    private void setRemoves(CRDTItem[] removes) {
        this.removes = removes;
    }

    public CRDTItem[] getChanges() {
        return changes;
    }

    private void setChanges(CRDTItem[] changes) {
        this.changes = changes;
    }
}
