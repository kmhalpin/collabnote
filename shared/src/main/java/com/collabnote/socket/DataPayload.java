package com.collabnote.socket;

import java.io.Serializable;
import java.util.List;

import com.collabnote.crdt.CRDTItemSerializable;
import com.collabnote.crdt.gc.DeleteGroupSerializable;

public class DataPayload implements Serializable {
    private int agent;
    private Type type;
    private String shareID;
    private CRDTItemSerializable crdtItem;
    private List<DeleteGroupSerializable> deleteGroupList;
    private int caretIndex;

    public static DataPayload insertPayload(String shareID, CRDTItemSerializable crdtItem) {
        return new DataPayload(Type.INSERT, shareID, crdtItem, 0, null);
    }

    public static DataPayload deletePayload(String shareID, CRDTItemSerializable item) {
        return new DataPayload(Type.DELETE, shareID, item, 0, null);
    }

    public static DataPayload donePayload(CRDTItemSerializable item) {
        return new DataPayload(Type.DONE, null, item, 0, null);
    }

    public static DataPayload caretPayload(String shareID, int caretIndex) {
        return new DataPayload(Type.CARET, shareID, null, caretIndex, null);
    }

    public static DataPayload gcPayload(String shareID, List<DeleteGroupSerializable> items) {
        return new DataPayload(Type.GC, shareID, null, 0, items);
    }

    public static DataPayload recoverPayload(String shareID, CRDTItemSerializable insertItem,
            List<DeleteGroupSerializable> items) {
        return new DataPayload(Type.RECOVER, shareID, insertItem, 0, items);
    }

    public DataPayload(Type type, String shareID, CRDTItemSerializable crdtItem, int caretIndex,
            List<DeleteGroupSerializable> deleteGroupList) {
        this.type = type;
        this.shareID = shareID;
        this.crdtItem = crdtItem;
        this.caretIndex = caretIndex;
        this.deleteGroupList = deleteGroupList;
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

    public List<DeleteGroupSerializable> getDeleteGroupList() {
        return deleteGroupList;
    }
}
