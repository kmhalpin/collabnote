package com.collabnote.newcrdt;

import java.io.Serializable;

public class CRDTItemSerializable implements Serializable {
    String content;
    CRDTID id;
    CRDTID originLeft;
    CRDTID originRight;
    boolean isDeleted;
    CRDTID left;
    CRDTID right;

    public CRDTItemSerializable(String content, CRDTID id, CRDTID originLeft, CRDTID originRight, boolean isDeleted,
            CRDTID left, CRDTID right) {
        this.content = content;
        this.id = id;
        this.originLeft = originLeft;
        this.originRight = originRight;
        this.isDeleted = isDeleted;
        this.left = left;
        this.right = right;
    }

}
