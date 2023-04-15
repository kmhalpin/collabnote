package com.collabnote.newcrdt.gc;

import java.util.ArrayList;
import java.util.List;
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
        if (gcItem.isDeleted()) {
            gcItem.setDeleted();
        } else {
            // check splitable
            gcItem.checkSplitGC();
        }
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
                    if (fitem != null && !((GCCRDTItem) fitem).isDeleteGroupGCed()) {
                        continue;
                    }
                } catch (NoSuchElementException e) {
                }

                CRDTItem bitem = i.bindItem(this.versionVector);
                if (bitem != null && !((bitem.originLeft != null
                        && ((GCCRDTItem) bitem.originLeft).isDeleteGroupGCed())
                        || (bitem.originRight != null
                                && ((GCCRDTItem) bitem.originRight).isDeleteGroupGCed()))) {
                    // re integrate delete group
                    if (fitem != null && ((GCCRDTItem) fitem).isDeleteGroupGCed()) {
                        try {
                            lock.lock();
                            integrate(bitem);
                            versionVector.recover(bitem);
                            ((GCCRDTItem) bitem).setDeleteGroup((GCCRDTItem) fitem); // move delete group
                            remove(fitem);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        // if garbage collected, recover normally
                        remoteInsert(bitem, false);
                        versionVector.recover(bitem);
                    }
                } else {
                    // if bitem not binded, or bitem's origins are isDeleteGroupGCed, queue to
                    // missing until their origins correct
                    missing.add(i);
                }
            }
            item = missing;
        }
    }

    // used by server
    public List<CRDTItem> findConflictingGC(CRDTItem item) {
        List<CRDTItem> conflictGC = new ArrayList<>();
        if ((item.left == null && (item.right == null || item.right.left != null))
                || (item.left != null && item.left.right != item.right)) {
            CRDTItem left = item.left;

            CRDTItem o;

            // start from conflicting item
            if (item.left != null) {
                o = left.right;
            } else {
                o = this.start;
            }

            while (o != null && o != item.right) {
                // collect conflicting gc
                if (o.isDeleted()) {
                    conflictGC.add(o);
                }
                o = o.right;
            }
        }
        return conflictGC;
    }

}
