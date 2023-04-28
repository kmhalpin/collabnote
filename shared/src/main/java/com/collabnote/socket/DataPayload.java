package com.collabnote.socket;

import java.io.Serializable;
import java.util.ArrayList;

import com.collabnote.crdt.CRDTItemSerializable;

public class DataPayload implements Serializable {
    private int agent;
    private Type type;
    private String shareID;
    private CRDTItemSerializable crdtItem;
    private ArrayList<CRDTItemSerializable> crdtList;
    private int caretIndex;

    public static DataPayload insertPayload(String shareID, CRDTItemSerializable crdtItem) {
        return new DataPayload(Type.INSERT, shareID, crdtItem, 0, null);
    }

    public static DataPayload deletePayload(String shareID, CRDTItemSerializable crdtItem) {
        return new DataPayload(Type.DELETE, shareID, crdtItem, 0, null);
    }

    public static DataPayload caretPayload(String shareID, int caretIndex) {
        return new DataPayload(Type.CARET, shareID, null, caretIndex, null);
    }

    public static DataPayload gcPayload(String shareID, ArrayList<CRDTItemSerializable> items) {
        return new DataPayload(Type.GC, shareID, null, 0, items);
    }

    public static DataPayload recoverPayload(String shareID, CRDTItemSerializable insertItem,
            ArrayList<CRDTItemSerializable> items) {
        return new DataPayload(Type.RECOVER, shareID, insertItem, 0, items);
    }

    public DataPayload(Type type, String shareID, CRDTItemSerializable crdtItem, int caretIndex,
            ArrayList<CRDTItemSerializable> crdtList) {
        this.type = type;
        this.shareID = shareID;
        this.crdtItem = crdtItem;
        this.caretIndex = caretIndex;
        this.crdtList = crdtList;
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

    public ArrayList<CRDTItemSerializable> getCrdtList() {
        return crdtList;
    }
}
