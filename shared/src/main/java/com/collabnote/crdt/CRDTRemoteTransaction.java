package com.collabnote.crdt;

public interface CRDTRemoteTransaction {
    public void onRemoteCRDTInsert(Transaction transaction);
    public void onRemoteCRDTDelete(Transaction transaction);
}
