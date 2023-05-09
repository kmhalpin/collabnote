package com.collabnote.crdt.gc;

import java.util.NoSuchElementException;

import com.collabnote.crdt.CRDTID;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTItemSerializable;

public class GCCRDTItem extends CRDTItem {
    public GCCRDTItem rightDeleteGroup;
    public GCCRDTItem leftDeleteGroup;
    public int level;
    // count reference on same level
    public int conflictReferenceLeft;
    public int conflictReferenceRight;
    // used to mark gc
    public boolean gc;
    // mark item as base of other level
    public boolean levelBase;

    public GCCRDTItem(String content, CRDTID id, boolean isDeleted,
            CRDTItem left, CRDTItem right) {
        super(content, id, isDeleted, left, right);
        this.rightDeleteGroup = this.leftDeleteGroup = null;
        this.gc = this.levelBase = false;
        this.conflictReferenceLeft = this.conflictReferenceRight = 0;
    }

    private int increaseConflictReferenceLeft() {
        return this.conflictReferenceLeft++;
    }

    private int decreaseConflictReferenceLeft() {
        return this.conflictReferenceLeft--;
    }

    private int increaseConflictReferenceRight() {
        return this.conflictReferenceRight++;
    }

    private int decreaseConflictReferenceRight() {
        return this.conflictReferenceRight--;
    }

    public void increaseOriginConflictReference() {
        if (this.getOriginLeft() != null && ((GCCRDTItem) this.getOriginLeft()).level == this.level)
            ((GCCRDTItem) this.getOriginLeft()).increaseConflictReferenceRight();
        if (this.getOriginRight() != null && ((GCCRDTItem) this.getOriginRight()).level == this.level)
            ((GCCRDTItem) this.getOriginRight()).increaseConflictReferenceLeft();
    }

    public void decreaseOriginConflictReference() {
        if (this.getOriginLeft() != null && ((GCCRDTItem) this.getOriginLeft()).level == this.level)
            ((GCCRDTItem) this.getOriginLeft()).decreaseConflictReferenceRight();
        if (this.getOriginRight() != null && ((GCCRDTItem) this.getOriginRight()).level == this.level)
            ((GCCRDTItem) this.getOriginRight()).decreaseConflictReferenceLeft();
    }

    public void setLevel() {
        if (this.getOriginLeft() == null || this.getOriginRight() == null) {
            this.level = 0;
        } else {
            int originLeftLevel = ((GCCRDTItem) this.getOriginLeft()).level;
            int originRightLevel = ((GCCRDTItem) this.getOriginRight()).level;
            if (originLeftLevel == originRightLevel) {
                this.level = originLeftLevel + 1;
                ((GCCRDTItem) this.getOriginLeft()).levelBase = ((GCCRDTItem) this.getOriginRight()).levelBase = true;
            } else {
                if (originLeftLevel > originRightLevel) {
                    this.level = originLeftLevel;
                    ((GCCRDTItem) this.getOriginRight()).levelBase = true;
                } else {
                    this.level = originRightLevel;
                    ((GCCRDTItem) this.getOriginLeft()).levelBase = true;
                }
            }
        }
    }

    public boolean isGarbageCollectable() {
        return super.isDeleted() && this.rightDeleteGroup == null && this.leftDeleteGroup == null;
    }

    public boolean isDeleteGroupDelimiter() {
        return super.isDeleted() && this.rightDeleteGroup != null && this.leftDeleteGroup != null;
    }

    @Override
    public void setDeleted() {
        super.setDeleted();
        // if (super.getOriginLeft() != null
        // && ((GCCRDTItem) super.getOriginLeft()).level == this.level
        // && ((GCCRDTItem) super.getOriginLeft()).isGarbageCollectable()
        // && ((GCCRDTItem) super.getOriginLeft()).conflictReference > 1) {
        // }
        // if (super.getOriginRight() != null
        // && ((GCCRDTItem) super.getOriginRight()).level == this.level
        // && ((GCCRDTItem) super.getOriginRight()).isGarbageCollectable()
        // && ((GCCRDTItem) super.getOriginRight()).conflictReference > 1) {
        // }

        if ((super.right != null && ((GCCRDTItem) super.right).isGarbageCollectable())
                || (super.left != null && ((GCCRDTItem) super.left).isGarbageCollectable())) {
            return;
        }

        this.rightDeleteGroup = this.leftDeleteGroup = this;

        if (super.right != null && super.right.isDeleted() && ((GCCRDTItem) super.right).level == this.level) {
            GCCRDTItem gci = (GCCRDTItem) super.right;
            this.rightDeleteGroup = gci.rightDeleteGroup;

            if (gci != this.rightDeleteGroup) {
                gci.rightDeleteGroup = gci.leftDeleteGroup = null;
            }

            this.rightDeleteGroup.leftDeleteGroup = this;
        }

        if (super.left != null && super.left.isDeleted() && ((GCCRDTItem) super.left).level == this.level) {
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

    // remove entire delete group in a level if possible
    public void removeLevelDeleteGroup() {
        if (!this.isDeleteGroupDelimiter()) {
            return;
        }

        GCCRDTItem leftLevelBase = (GCCRDTItem) this.leftDeleteGroup.left;
        GCCRDTItem rightLevelBase = (GCCRDTItem) this.rightDeleteGroup.right;

        if (!(leftLevelBase != null
                && leftLevelBase.isDeleteGroupDelimiter()
                && leftLevelBase.level == this.level - 1)) {
            return;
        }
        if (!(rightLevelBase != null
                && rightLevelBase.isDeleteGroupDelimiter()
                && rightLevelBase.level == this.level - 1)) {
            return;
        }

        // connect level base
        leftLevelBase.right = rightLevelBase;
        rightLevelBase.left = leftLevelBase;

        // merge level base delete group
        leftLevelBase.leftDeleteGroup.rightDeleteGroup = rightLevelBase.rightDeleteGroup;
        rightLevelBase.rightDeleteGroup.leftDeleteGroup = leftLevelBase.leftDeleteGroup;

        leftLevelBase.leftDeleteGroup = leftLevelBase.rightDeleteGroup = rightLevelBase.leftDeleteGroup = rightLevelBase.rightDeleteGroup = null;

        // transform delete group to garbage collectable
        GCCRDTItem templeft = this.leftDeleteGroup;
        GCCRDTItem tempright = this.rightDeleteGroup;
        templeft.leftDeleteGroup = templeft.rightDeleteGroup = null;
        tempright.leftDeleteGroup = tempright.rightDeleteGroup = null;
        templeft = tempright = null;
    }

    public void checkSplitGC() {
        checkSplitGC(null);
    }

    // split gc if item integrated inside delete group
    public void checkSplitGC(GCCRDTItem leftDeleteGroup) {
        GCCRDTItem gcItemLeft = (GCCRDTItem) this.left;
        GCCRDTItem gcItemRight = (GCCRDTItem) this.right;

        if (gcItemLeft != null && gcItemLeft.isDeleted()
                && gcItemRight != null && gcItemRight.isDeleted()
                && ((gcItemLeft.isGarbageCollectable() || gcItemRight.isGarbageCollectable())
                        || gcItemLeft.rightDeleteGroup == gcItemRight)) {

            // find left delete group
            GCCRDTItem oldLeftDeleteGroup = gcItemLeft;
            if (leftDeleteGroup != null && leftDeleteGroup.isDeleteGroupDelimiter()) {
                oldLeftDeleteGroup = leftDeleteGroup;
            } else {
                while (!oldLeftDeleteGroup.isDeleteGroupDelimiter()) {
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
            gcItemLeft.rightDeleteGroup = gcItemLeft;

            oldRightDeleteGroup.leftDeleteGroup = gcItemRight;
            gcItemRight.rightDeleteGroup = oldRightDeleteGroup;
            gcItemRight.leftDeleteGroup = gcItemRight;
        }
    }

    @Override
    public CRDTItemSerializable serialize() {
        return new CRDTItemSerializable(this.content, this.id,
                this.getOriginLeft() != null ? this.getOriginLeft().id : null,
                this.getOriginRight() != null ? this.getOriginRight().id : null,
                this.isDeleted());
    }

    @Override
    public void setOrigin(CRDTItem originLeft, CRDTItem originRight) {
        super.setOrigin(originLeft, originRight);
        this.setLevel();
        this.increaseOriginConflictReference();
    }

}
