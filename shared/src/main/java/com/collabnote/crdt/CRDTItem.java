package com.collabnote.crdt;

import java.io.Serializable;

public class CRDTItem implements Serializable {
    public String content;
    public CRDTID id;
    private CRDTItem originLeft;
    private CRDTItem originRight;
    private boolean isDeleted;
    public CRDTItem left;
    public CRDTItem right;

    public CRDTItem(String content, CRDTID id, boolean isDeleted,
            CRDTItem left, CRDTItem right) {
        this.content = content;
        this.id = id;
        this.originLeft = null;
        this.originRight = null;
        this.isDeleted = isDeleted;
        this.left = left;
        this.right = right;
    }

    public CRDTItemSerializable serialize() {
        return new CRDTItemSerializable(this.content,
                this.id,
                this.originLeft == null ? null : this.originLeft.id,
                this.originRight == null ? null : this.originRight.id, isDeleted);
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted() {
        this.isDeleted = true;
    }

    public CRDTItem getOriginLeft() {
        return originLeft;
    }

    public void removeOriginLeft() {
        this.originLeft = null;
    }

    public void setOrigin(CRDTItem originLeft, CRDTItem originRight) {
        this.originLeft = originLeft;
        this.originRight = originRight;
    }

    public CRDTItem getOriginRight() {
        return originRight;
    }

    public void removeOriginRight() {
        this.originRight = null;
    }

}
