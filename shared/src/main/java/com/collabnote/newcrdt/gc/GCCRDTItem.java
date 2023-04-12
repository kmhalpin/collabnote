package com.collabnote.newcrdt.gc;

import com.collabnote.newcrdt.CRDTID;
import com.collabnote.newcrdt.CRDTItem;

public class GCCRDTItem extends CRDTItem {
    int reference;
    boolean gc;

    public GCCRDTItem(String content, CRDTID id, CRDTItem originLeft, CRDTItem originRight, boolean isDeleted,
            CRDTItem left, CRDTItem right) {
        super(content, id, originLeft, originRight, isDeleted, left, right);
        this.reference = 0;
    }

    public GCCRDTItem(CRDTItem item) {
        this(item.content, item.id, item.originLeft, item.originRight, item.isDeleted, item.left, item.right);
    }

    int increaseReference() {
        return this.reference++;
    }

    int decreaseReference() {
        return this.reference--;
    }

    boolean isGarbageCollectable() {
        return super.isDeleted && this.reference == 0;
    }

}
