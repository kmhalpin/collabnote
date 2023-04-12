package com.collabnote.newcrdt;

public class Marker {
    CRDTItem item;
    int index;

    public Marker(CRDTItem item, int index) {
        this.item = item;
        this.index = index;
    }
}
