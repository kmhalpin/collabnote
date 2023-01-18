package com.collabnote.crdt;

public interface CRDTListener {
    public void onCRDTInsert(CRDTItem item);
    public void onCRDTDelete(CRDTItem item);
    public void onCRDTRemove(CRDTItem[] remove);
}
