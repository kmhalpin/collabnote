package com.collabnote.newcrdt.gc;

import com.collabnote.newcrdt.CRDTID;
import com.collabnote.newcrdt.CRDTItem;

public class GCCRDTItem extends CRDTItem {
    public GCCRDTItem rightDeleteGroup;
    public GCCRDTItem leftDeleteGroup;
    public boolean gc;

    public GCCRDTItem(String content, CRDTID id, CRDTItem originLeft, CRDTItem originRight, boolean isDeleted,
            CRDTItem left, CRDTItem right) {
        super(content, id, originLeft, originRight, isDeleted, left, right);
        this.rightDeleteGroup = this.leftDeleteGroup = null;
        this.gc = false;
    }

    public GCCRDTItem(CRDTItem item) {
        this(item.content, item.id, item.originLeft, item.originRight, item.isDeleted(), item.left, item.right);
    }

    public boolean isGarbageCollectable() {
        return super.isDeleted() && this.rightDeleteGroup == null && this.leftDeleteGroup == null;
    }

    @Override
    public void setDeleted() {
        super.setDeleted();
        if (super.right != null && super.right.isDeleted()) {
            GCCRDTItem gci = (GCCRDTItem) super.right;
            this.rightDeleteGroup = gci.rightDeleteGroup;
            gci.rightDeleteGroup = null;

            this.rightDeleteGroup.leftDeleteGroup = this;
        } else {
            this.rightDeleteGroup = this;
        }

        if (super.left != null && super.left.isDeleted()) {
            GCCRDTItem gci = (GCCRDTItem) super.left;
            this.leftDeleteGroup = gci.leftDeleteGroup;
            gci.leftDeleteGroup = null;

            this.leftDeleteGroup.rightDeleteGroup = this;
        } else {
            this.leftDeleteGroup = this;
        }

        if (this.leftDeleteGroup != this && this.rightDeleteGroup != this) {
            this.leftDeleteGroup.rightDeleteGroup = this.rightDeleteGroup;
            this.rightDeleteGroup = null;

            this.rightDeleteGroup.leftDeleteGroup = this.leftDeleteGroup;
            this.leftDeleteGroup = null;
        }
    }

}
