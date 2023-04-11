package com.collabnote.newcrdt;

public interface CRDTListener {
    public void onCRDTInsert(Transaction transaction);
    public void onCRDTDelete(Transaction transaction);
}
