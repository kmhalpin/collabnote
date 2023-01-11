package com.collabnote.otcrdt;

public interface CRDTListener {
    public void onCRDTInsert(CRDTItem item);
    public void onCRDTDelete(CRDTItem item);
}
