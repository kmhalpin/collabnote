package com.collabnote.crdt;

public interface CRDTListener {
    public void onCRDTInsert(CRDTItem item, int pos);
    public void onCRDTDelete(CRDTItem item, int pos);
}
