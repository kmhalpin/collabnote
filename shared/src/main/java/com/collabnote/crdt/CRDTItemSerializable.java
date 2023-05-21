package com.collabnote.crdt;

import java.io.Serializable;

public class CRDTItemSerializable implements Serializable {
    public String content;
    public CRDTID id;
    public CRDTID originLeft;
    public CRDTID originRight;
    public boolean isDeleted;
    public boolean isGC;

    public CRDTItemSerializable(String content, CRDTID id, CRDTID originLeft, CRDTID originRight, boolean isDeleted,
            boolean isGC) {
        this.content = content;
        this.id = id;
        this.originLeft = originLeft;
        this.originRight = originRight;
        this.isDeleted = isDeleted;
        this.isGC = isGC;
    }

    @Override
    public String toString() {
        return "{"
                + (this.originLeft == null ? "null" : (this.originLeft.agent + "-" + this.originLeft.seq)) + ", "
                + this.id.agent + "-" + this.id.seq + ", "
                + this.content + ", "
                + (this.isDeleted ? "1" : "0") + ", "
                + (this.originRight == null ? "null" : (this.originRight.agent + "-" + this.originRight.seq)) + ", "
                + "}";
    }
}
