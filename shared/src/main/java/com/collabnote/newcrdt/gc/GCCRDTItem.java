package com.collabnote.newcrdt.gc;

import com.collabnote.newcrdt.CRDTID;
import com.collabnote.newcrdt.CRDTItem;

public class GCCRDTItem extends CRDTItem {
    int reference;

    public GCCRDTItem(String content, CRDTID id, CRDTItem originLeft, CRDTItem originRight, boolean isDeleted,
            CRDTItem left, CRDTItem right) {
        super(content, id, originLeft, originRight, isDeleted, left, right);
        this.reference = 0;
    }

    void increaseReference() {
        this.reference++;
    }

    void decreaseReference() {
        this.reference--;
    }

}
