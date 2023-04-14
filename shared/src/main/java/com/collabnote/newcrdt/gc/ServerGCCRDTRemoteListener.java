package com.collabnote.newcrdt.gc;

import com.collabnote.newcrdt.CRDTRemoteListener;

public interface ServerGCCRDTRemoteListener extends CRDTRemoteListener {
    public void onRemoteCRDTInsert(ServerTransaction transaction);
}
