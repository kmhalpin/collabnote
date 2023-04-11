package com.collabnote.newcrdt;

// cache last local inserts, used to optimize local insert index finding, can be updated every remote insert.
public class Marker {
    CRDTItem item;
    int index;

    public Marker(CRDTItem item, int index) {
        this.item = item;
        this.index = index;
    }

    void updateMarker(int index, int length) {
        CRDTItem i = item;
        {// adjust position to not pointing deleted item
            while (i != null && (i.isDeleted)) {
                i = i.left;
                if (i != null && !i.isDeleted) {
                    index -= 1;
                    // since i is not deleted loop will break
                }
            }
            item = i;
        }
        if (index < this.index || (length > 0 && index == this.index))
            this.index = Math.max(index, this.index + length);
    }
}
