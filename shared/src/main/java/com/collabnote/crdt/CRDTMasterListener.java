package com.collabnote.crdt;

public interface CRDTMasterListener {
    public void onCRDTInsert(CRDTItem item);
    public void onCRDTRemove(CRDTItem[] remove, CRDTItem[] change);
}
