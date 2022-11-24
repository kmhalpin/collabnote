package com.collabnote.crdt;

import java.io.Serializable;

public class CRDTItem implements Serializable {
    String value;
    CRDTID id;
    CRDTID originLeft;
    CRDTID originRight;
    boolean isDeleted;

    CRDTItem(String value, CRDTID id, CRDTID originLeft, CRDTID originRight, boolean isDeleted) {
        this.value = value;
        this.id = id;
        this.originLeft = originLeft;
        this.originRight = originRight;
        this.isDeleted = isDeleted;
    }

    public String getValue() {
        return value;
    }

}
