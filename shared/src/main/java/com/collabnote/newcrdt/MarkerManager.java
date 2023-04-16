package com.collabnote.newcrdt;

// cache last local inserts, used to optimize local insert index finding, can be
// updated every remote insert.
public class MarkerManager {
    public Marker marker;

    public MarkerManager() {
        this.marker = null;
    }

    Marker findMarker(CRDTItem start, int index) {
        if (start == null || index == 0) {
            return null;
        }

        Marker marker = this.marker;

        CRDTItem p = start;
        int pidx = 0;
        if (marker != null) {
            p = marker.item;
            pidx = marker.index;
        }

        // iterate right if possible
        while (p.right != null && pidx <= index) {
            if (!p.isDeleted()) {
                if (index < pidx + 1) {
                    break;
                }
                pidx += 1;
            }
            p = p.right;
        }

        // iterate left if necessary
        while (p.left != null && pidx >= index) {
            p = p.left;
            if (!p.isDeleted()) {
                pidx -= 1;
            }
        }

        // overwrite cache
        if (this.marker != null) {
            this.marker.item = p;
            this.marker.index = pidx;
        } else {
            this.marker = new Marker(p, pidx);
        }

        return this.marker;
    }

    public void updateMarker(int index, int length) {
        if (marker == null) {
            return;
        }

        CRDTItem i = marker.item;
        {// adjust position to not pointing deleted item
            while (i != null && (i.isDeleted())) {
                i = i.left;
                if (i != null && !i.isDeleted()) {
                    index -= 1;
                    // since i is not deleted loop will break
                }
            }
            if (i == null) {
                marker = null;
                return;
            }
            marker.item = i;
        }
        if (index < marker.index || (length > 0 && index == marker.index))
            marker.index = Math.max(index, marker.index + length);
    }
}
