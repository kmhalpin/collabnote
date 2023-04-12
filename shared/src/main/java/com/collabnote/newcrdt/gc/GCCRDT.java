package com.collabnote.newcrdt.gc;

import com.collabnote.newcrdt.CRDT;
import com.collabnote.newcrdt.CRDTItem;
import com.collabnote.newcrdt.CRDTLocalListener;
import com.collabnote.newcrdt.CRDTRemoteListener;

public class GCCRDT extends CRDT {

    public GCCRDT(int agent, CRDTRemoteListener remotelistener, CRDTLocalListener localListener) {
        super(agent, remotelistener, localListener);
    }

    @Override
    protected void integrate(CRDTItem item) {
        GCCRDTItem gcItem = new GCCRDTItem(item);
        super.integrate(gcItem);
        if (gcItem.originLeft != null)
            ((GCCRDTItem) gcItem.originLeft).increaseReference();
        if (gcItem.originRight != null)
            ((GCCRDTItem) gcItem.originRight).increaseReference();
    }

    @Override
    public void delete(CRDTItem item) {
        GCCRDTItem gcItem = new GCCRDTItem(item);
        super.delete(gcItem);
        if (gcItem.originLeft != null)
            ((GCCRDTItem) gcItem.originLeft).decreaseReference();
        if (gcItem.originRight != null)
            ((GCCRDTItem) gcItem.originRight).decreaseReference();
    }

}
