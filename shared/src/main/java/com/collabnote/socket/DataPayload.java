package com.collabnote.socket;

import java.io.Serializable;

import com.collabnote.crdt.CRDTItem;

public class DataPayload implements Serializable {
    private String agent;
    private Type type;
    private String shareID;
    private CRDTItem crdtItem;
    private int caretIndex;

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
}
