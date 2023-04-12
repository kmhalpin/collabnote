package com.collabnote.newcrdt;

public class CRDTItem {
    public String content;
    public CRDTID id;
    public CRDTItem originLeft;
    public CRDTItem originRight;
    public boolean isDeleted;
    public CRDTItem left;
    public CRDTItem right;

    public CRDTItem(String content, CRDTID id, CRDTItem originLeft, CRDTItem originRight, boolean isDeleted,
            CRDTItem left, CRDTItem right) {
        this.content = content;
        this.id = id;
        this.originLeft = originLeft;
        this.originRight = originRight;
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
}
