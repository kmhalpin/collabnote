package com.collabnote.otcrdt;

import java.io.Serializable;

public class CRDTItem implements Serializable {
    String value;
    CRDTID id;
    CRDTID originLeft;
    CRDTID originRight;
    boolean isDeleted;
    boolean isAck;

    CRDTItem(String value, CRDTID id, CRDTID originLeft, CRDTID originRight, boolean isDeleted, boolean isAck) {
        this.value = value;
        this.id = id;
        this.originLeft = originLeft;
        this.originRight = originRight;
        this.isDeleted = isDeleted;
        this.isAck = isAck;
    }

    public String getValue() {
        return value;
    }

}
