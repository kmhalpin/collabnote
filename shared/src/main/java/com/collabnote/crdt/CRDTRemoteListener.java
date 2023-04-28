package com.collabnote.crdt;

public interface CRDTRemoteListener {
    public void onRemoteCRDTInsert(Transaction transaction);
    public void onRemoteCRDTDelete(Transaction transaction);
}
