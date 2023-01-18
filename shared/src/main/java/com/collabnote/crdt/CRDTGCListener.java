package com.collabnote.crdt;

public interface CRDTGCListener {
    public void onCRDTInsert(CRDTItem item);
    public void onCRDTRemove(CRDTItem[] remove);
}
