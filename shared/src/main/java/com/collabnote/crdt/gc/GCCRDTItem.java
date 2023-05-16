package com.collabnote.crdt.gc;

import java.util.NoSuchElementException;

import com.collabnote.crdt.CRDTID;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTItemSerializable;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.model.Node;

public class GCCRDTItem extends CRDTItem {
    public GCCRDTItem rightDeleteGroup;
    public GCCRDTItem leftDeleteGroup;
    public int level;

    // 1 = gc
    // 2 = levelBase
    // 4-8 = conflictReferenceLeft (mask = 12)
    // 16-32 = conflictReferenceRight (mask = 48)
    // 64 = server gc delete group marking
    public byte flags;

    // used by server to mark gc able item
    public boolean getServerGc() {
        return (this.flags & 64) > 0;
    }

    public void setServerGc(boolean gc) {
        if (this.getServerGc() != gc)
            this.flags ^= 64;
    }

    // used to mark gc
    public boolean getGc() {
        return (this.flags & 1) > 0;
    }

    public void setGc(boolean gc) {
        if (this.getGc() != gc)
            this.flags ^= 1;
    }

    // mark item as base of other level
    public boolean getLevelBase() {
        return (this.flags & 2) > 0;
    }

    public void setLevelBase(boolean levelBase) {
        if (this.getLevelBase() != levelBase)
            this.flags ^= 2;
    }

    // count reference on same level (1 (single reference), 2 (conflict reference))
    public int getLeftRefrencer() {
        return (this.flags & 12) / 4;
    }

    public void increaseLeftRefrencer() {
        if (this.getLeftRefrencer() < 2)
            this.flags ^= ((this.flags & 4) * 2) + 4;
    }

    public int getRightRefrencer() {
        return (this.flags & 48) / 16;
    }

    public void increaseRightRefrencer() {
        if (this.getRightRefrencer() < 2)
            this.flags ^= ((this.flags & 16) * 2) + 16;
    }

    public GCCRDTItem(String content, CRDTID id, boolean isDeleted,
            CRDTItem left, CRDTItem right) {
        super(content, id, isDeleted, left, right);
        this.rightDeleteGroup = this.leftDeleteGroup = null;
        this.flags = 0;
    }

    public void increaseOriginConflictReference() {
        if (this.getOriginLeft() != null && ((GCCRDTItem) this.getOriginLeft()).level == this.level)
            ((GCCRDTItem) this.getOriginLeft()).increaseRightRefrencer();
        if (this.getOriginRight() != null && ((GCCRDTItem) this.getOriginRight()).level == this.level)
            ((GCCRDTItem) this.getOriginRight()).increaseLeftRefrencer();
    }

    public void setLevel() {
        if (this.getOriginLeft() == null || this.getOriginRight() == null) {
            this.level = 0;
        } else {
            int originLeftLevel = ((GCCRDTItem) this.getOriginLeft()).level;
            int originRightLevel = ((GCCRDTItem) this.getOriginRight()).level;
            if (originLeftLevel == originRightLevel) {
                this.level = originLeftLevel + 1;
                ((GCCRDTItem) this.getOriginLeft()).setLevelBase(true);
                ((GCCRDTItem) this.getOriginRight()).setLevelBase(true);
            } else {
                if (originLeftLevel > originRightLevel) {
                    this.level = originLeftLevel;
                    ((GCCRDTItem) this.getOriginRight()).setLevelBase(true);
                } else {
                    this.level = originRightLevel;
                    ((GCCRDTItem) this.getOriginLeft()).setLevelBase(true);
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

    // remove entire delete group in a level and merge top level delete group
    public void mergeLevelDeleteGroup() {
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

    @Override
    public Node renderNode() {
        Node node = this.node;
        node = node.with(Label.of(content
                + "\nlv: " + this.level
                + "\nlb: " + this.getLevelBase()
                + "\nlc: " + (this.getLeftRefrencer() == 2)
                + "\nrc: " + (this.getRightRefrencer() == 2)));
        if (this.isGarbageCollectable())
            node = node.with(Color.RED);
        else if (this.isDeleteGroupDelimiter())
            node = node.with(Color.BLUE);
        return node;
    }

}
