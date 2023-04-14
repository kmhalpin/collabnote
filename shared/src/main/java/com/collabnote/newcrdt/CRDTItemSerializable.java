package com.collabnote.newcrdt;

import java.io.Serializable;

public class CRDTItemSerializable implements Serializable {
    public String content;
    public CRDTID id;
    public CRDTID originLeft;
    public CRDTID originRight;
    public boolean isDeleted;

    public CRDTItemSerializable(String content, CRDTID id, CRDTID originLeft, CRDTID originRight, boolean isDeleted) {
        this.content = content;
        this.id = id;
        this.originLeft = originLeft;
        this.originRight = originRight;
        this.isDeleted = isDeleted;
    }

    public CRDTItem bindItem(VersionVectors versionVector) {
        if (this.originLeft != null
                && this.originLeft.agent != this.id.agent
                && !versionVector.exists(this.originLeft)) {
            return null;
        }
        if (this.originRight != null
                && this.originRight.agent != this.id.agent
                && !versionVector.exists(this.originRight)) {
            return null;
        }

        CRDTItem bitem = new CRDTItem(
                this.content,
                this.id,
                null,
                null,
                this.isDeleted,
                null,
                null);

        if (this.originLeft != null) {
            bitem.left = bitem.originLeft = versionVector.find(this.originLeft);
        }
        if (this.originRight != null) {
            bitem.right = bitem.originRight = versionVector.find(this.originRight);
        }

        return bitem;
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
