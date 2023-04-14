package com.collabnote.newcrdt.gc;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import com.collabnote.newcrdt.CRDT;
import com.collabnote.newcrdt.CRDTItem;
import com.collabnote.newcrdt.CRDTItemSerializable;
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
    }

    @Override
    public void delete(CRDTItem item) {
        GCCRDTItem gcItem = new GCCRDTItem(item);
        super.delete(gcItem);
    }

    public void remove(CRDTItem item) {
        versionVector.remove(item);
        item.right.left = item.left;
        item.left.right = item.right;
    }

    // client recover can be run concurrently
    public void recover(ArrayList<CRDTItemSerializable> item) {
        // expected will 0
        while (item.size() > 0) {
            ArrayList<CRDTItemSerializable> missing = new ArrayList<>();
            for (CRDTItemSerializable i : item) {
                CRDTItem fitem = null;
                try {
                    fitem = versionVector.find(i.id);
                    if (fitem != null && ((GCCRDTItem) fitem).isGarbageCollectable()) {
                        continue;
                    }
                } catch (NoSuchElementException e) {
                }

                CRDTItem bitem = i.bindItem(this.versionVector);
                if (bitem != null) {
                    if (fitem != null && !((GCCRDTItem) fitem).isGarbageCollectable()
                            && (fitem.originLeft == null || fitem.originRight == null)) {
                        // re integrate delete group
                        try {
                            lock.lock();
                            integrate(bitem);
                            versionVector.recover(bitem);
                            remove(fitem);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        remoteInsert(bitem, false);
                        versionVector.recover(bitem);
                    }
                } else {
                    missing.add(i);
                }
            }
            item = missing;
        }
    }

}
