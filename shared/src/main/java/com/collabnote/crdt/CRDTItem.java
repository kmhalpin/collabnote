package com.collabnote.crdt;

import java.io.Serializable;

public class CRDTItem implements Serializable {
    String value;
    CRDTID id;
    CRDTID originLeft;
    CRDTID originRight;
    boolean isDeleted;
    int reference = 0;

    public CRDTItem(CRDTItem other) {
        this.value = other.value;
        this.id = other.id;
        this.originLeft = other.originLeft;
        this.originRight = other.originRight;
        this.isDeleted = other.isDeleted;
    }

    CRDTItem(String value, CRDTID id, CRDTID originLeft, CRDTID originRight, boolean isDeleted) {
        this.value = value;
        this.id = id;
        this.originLeft = originLeft;
        this.originRight = originRight;
        this.isDeleted = isDeleted;
    }

    public boolean equals(CRDTItem obj) {
        return obj.id.equals(this.id);
    }

    public String getValue() {
        return value;
    }

    public void increaseReference() {
        this.reference++;
    }

    public void decreaseReference() {
        this.reference--;
    }

    // tombstone and no reference
    public boolean isRemovable() {
        return this.isDeleted && this.reference <= 0;
    }

    // tombstone and still has reference
    public boolean isActiveTombstone() {
        return this.isDeleted && this.reference > 0;
    }

    public void permaRemove() {
        this.isDeleted = true;
        this.reference = -1;
    }

    public boolean isPermaRemove() {
        return this.isDeleted && this.reference == -1;
    }

    @Override
    public String toString() {
        return "{" + this.value + ", " + this.id + ", " + this.originLeft + ", " + this.originRight + ", "
                + this.isDeleted + ", " + this.reference + "}";
    }

}
