package com.collabnote.newcrdt;

public class CRDTItem {
    public String content;
    CRDTID id;
    CRDTItem originLeft;
    CRDTItem originRight;
    boolean isDeleted;
    CRDTItem left;
    CRDTItem right;

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
}
