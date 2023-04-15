package com.collabnote.newcrdt.gc;

import java.util.NoSuchElementException;

import com.collabnote.newcrdt.CRDTID;
import com.collabnote.newcrdt.CRDTItem;

public class GCCRDTItem extends CRDTItem {
    public GCCRDTItem rightDeleteGroup;
    public GCCRDTItem leftDeleteGroup;

    public GCCRDTItem(String content, CRDTID id, CRDTItem originLeft, CRDTItem originRight, boolean isDeleted,
            CRDTItem left, CRDTItem right) {
        super(content, id, originLeft, originRight, isDeleted, left, right);
        this.rightDeleteGroup = this.leftDeleteGroup = null;
    }

    public GCCRDTItem(CRDTItem item) {
        this(item.content, item.id, item.originLeft, item.originRight, item.isDeleted(), item.left, item.right);
    }

    public boolean isGarbageCollectable() {
        return super.isDeleted() && this.rightDeleteGroup == null && this.leftDeleteGroup == null;
    }

    public boolean isDeleteGroupGCed() {
        return super.isDeleted() && !isGarbageCollectable()
                && ((this.rightDeleteGroup == this && this.originLeft == null)
                        || (this.leftDeleteGroup == this && this.originRight == null));
    }

    public void setDeleteGroup(GCCRDTItem item) {
        this.leftDeleteGroup = item.leftDeleteGroup;
        this.rightDeleteGroup = item.rightDeleteGroup;
        item.rightDeleteGroup.leftDeleteGroup = this;
        item.leftDeleteGroup.rightDeleteGroup = this;
    }

    @Override
    public void setDeleted() {
        super.setDeleted();
        this.rightDeleteGroup = this;
        this.leftDeleteGroup = this;

        if (super.right != null && super.right.isDeleted()) {
            GCCRDTItem gci = (GCCRDTItem) super.right;
            this.rightDeleteGroup = gci.rightDeleteGroup;
            gci.rightDeleteGroup = null;

            this.rightDeleteGroup.leftDeleteGroup = this;
        }

        if (super.left != null && super.left.isDeleted()) {
            GCCRDTItem gci = (GCCRDTItem) super.left;
            this.leftDeleteGroup = gci.leftDeleteGroup;
            gci.leftDeleteGroup = null;

            this.leftDeleteGroup.rightDeleteGroup = this;
        }

        if (this.leftDeleteGroup != this && this.rightDeleteGroup != this) {
            this.leftDeleteGroup.rightDeleteGroup = this.rightDeleteGroup;
            this.rightDeleteGroup = null;

            this.rightDeleteGroup.leftDeleteGroup = this.leftDeleteGroup;
            this.leftDeleteGroup = null;
        }
    }

    // split gc if item integrated inside delete group
    public void checkSplitGC() {
        GCCRDTItem gcItemLeft = (GCCRDTItem) this.left;
        GCCRDTItem gcItemRight = (GCCRDTItem) this.right;

        if (gcItemLeft != null && gcItemLeft.isGarbageCollectable()
                || gcItemRight != null && gcItemRight.isGarbageCollectable()) {
            // find left delete group
            GCCRDTItem oldLeftDeleteGroup = (GCCRDTItem) gcItemLeft.left;
            while (oldLeftDeleteGroup.isGarbageCollectable()) {
                oldLeftDeleteGroup = (GCCRDTItem) oldLeftDeleteGroup.left;
            }

            if (!oldLeftDeleteGroup.isDeleted()) {
                throw new NoSuchElementException("not expected");
            }

            GCCRDTItem oldRightDeleteGroup = oldLeftDeleteGroup.rightDeleteGroup;

            // split group
            oldLeftDeleteGroup.rightDeleteGroup = gcItemLeft;
            gcItemLeft.leftDeleteGroup = oldLeftDeleteGroup;

            oldRightDeleteGroup.leftDeleteGroup = gcItemRight;
            gcItemRight.rightDeleteGroup = oldRightDeleteGroup;
        }
    }

}
