package com.collabnote.socket;

import java.io.Serializable;

import com.collabnote.newcrdt.CRDTItemSerializable;

public class DataPayload implements Serializable {
    private int agent;
    private Type type;
    private String shareID;
    private CRDTItemSerializable crdtItem;
    private int caretIndex;

    private CRDTItemSerializable[] removes;

    public static DataPayload insertPayload(String shareID, CRDTItemSerializable crdtItem) {
        return new DataPayload(Type.INSERT, shareID, crdtItem, 0);
    }

    public static DataPayload deletePayload(String shareID, CRDTItemSerializable crdtItem) {
        return new DataPayload(Type.DELETE, shareID, crdtItem, 0);
    }

    public static DataPayload caretPayload(String shareID, int caretIndex) {
        return new DataPayload(Type.CARET, shareID, null, caretIndex);
    }

    public static DataPayload ackDeletePayload(String shareID, CRDTItemSerializable[] removes) {
        DataPayload data = new DataPayload(Type.ACKDELETE, shareID, null, 0);
        data.setRemoves(removes);
        return data;
    }

    public static DataPayload ackInsertPayload(String shareID, CRDTItemSerializable crdtItem) {
        return new DataPayload(Type.ACKINSERT, shareID, crdtItem, 0);
    }

    public DataPayload(Type type, String shareID, CRDTItemSerializable crdtItem, int caretIndex) {
        this.type = type;
        this.shareID = shareID;
        this.crdtItem = crdtItem;
        this.caretIndex = caretIndex;
    }

    public int getAgent() {
        return agent;
    }

    public void setAgent(int agent) {
        this.agent = agent;
    }

    public Type getType() {
        return type;
    }

    public String getShareID() {
        return shareID;
    }

    public CRDTItemSerializable getCrdtItem() {
        return crdtItem;
    }

    public int getCaretIndex() {
        return caretIndex;
    }

    public CRDTItemSerializable[] getRemoves() {
        return removes;
    }

    private void setRemoves(CRDTItemSerializable[] removes) {
        this.removes = removes;
    }
}
