package com.collabnote.newcrdt;

public interface CRDTRemoteListener {
    public void onRemoteCRDTInsert(Transaction transaction);
    public void onRemoteCRDTDelete(Transaction transaction);
}
