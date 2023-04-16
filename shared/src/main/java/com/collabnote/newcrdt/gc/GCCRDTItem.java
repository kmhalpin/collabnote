package com.collabnote.newcrdt.gc;

import java.util.NoSuchElementException;

import com.collabnote.newcrdt.CRDTID;
import com.collabnote.newcrdt.CRDTItem;

public class GCCRDTItem extends CRDTItem {
    public GCCRDTItem rightDeleteGroup;
    public GCCRDTItem leftDeleteGroup;
    // used to mark gc
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

    public boolean isDeleteGroupGCed() {
        return super.isDeleted() && !isGarbageCollectable() && gc;
    }

    public void setDeleteGroupFrom(GCCRDTItem item) {
        if (item.leftDeleteGroup != item) {
            this.leftDeleteGroup = item.leftDeleteGroup;
            this.leftDeleteGroup.rightDeleteGroup = this;
        } else
            this.leftDeleteGroup = this;

        if (item.rightDeleteGroup != item) {
            this.rightDeleteGroup = item.rightDeleteGroup;
            this.rightDeleteGroup.leftDeleteGroup = this;
        } else
            this.rightDeleteGroup = this;

        item.leftDeleteGroup = null;
        item.rightDeleteGroup = null;

    }

    @Override
    public void setDeleted() {
        super.setDeleted();
        this.rightDeleteGroup = this.leftDeleteGroup = this;

        if (super.right != null && super.right.isDeleted()) {
            GCCRDTItem gci = (GCCRDTItem) super.right;
            this.rightDeleteGroup = gci.rightDeleteGroup;

            if (gci != this.rightDeleteGroup) {
                gci.rightDeleteGroup = gci.leftDeleteGroup = null;
            }

            this.rightDeleteGroup.leftDeleteGroup = this;
        }

        if (super.left != null && super.left.isDeleted()) {
            GCCRDTItem gci = (GCCRDTItem) super.left;
            this.leftDeleteGroup = gci.leftDeleteGroup;

            if (gci != this.leftDeleteGroup) {
                gci.rightDeleteGroup = gci.leftDeleteGroup = null;
            }

            this.leftDeleteGroup.rightDeleteGroup = this;
        }

        if (this.leftDeleteGroup != this && this.rightDeleteGroup != this) {
            this.leftDeleteGroup.rightDeleteGroup = this.rightDeleteGroup;
            this.rightDeleteGroup.leftDeleteGroup = this.leftDeleteGroup;

            this.rightDeleteGroup = this.leftDeleteGroup = null;
        }
    }

    public void checkSplitGC() {
        checkSplitGC(null);
    }

    // split gc if item integrated inside delete group
    public void checkSplitGC(GCCRDTItem leftDeleteGroup) {
        GCCRDTItem gcItemLeft = (GCCRDTItem) this.left;
        GCCRDTItem gcItemRight = (GCCRDTItem) this.right;

        if (gcItemLeft != null && gcItemLeft.isDeleted()
                && gcItemRight != null && gcItemRight.isDeleted()) {
            // find left delete group
            GCCRDTItem oldLeftDeleteGroup = (GCCRDTItem) gcItemLeft.left;
            if (leftDeleteGroup != null && leftDeleteGroup.isDeleted()
                    && !leftDeleteGroup.isGarbageCollectable()) {
                oldLeftDeleteGroup = leftDeleteGroup;
            } else {
                while (oldLeftDeleteGroup.isGarbageCollectable()) {
                    oldLeftDeleteGroup = (GCCRDTItem) oldLeftDeleteGroup.left;
                }
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
