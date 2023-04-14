package com.collabnote.newcrdt.gc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import com.collabnote.newcrdt.CRDT;
import com.collabnote.newcrdt.CRDTItem;
import com.collabnote.newcrdt.CRDTLocalListener;

public class ServerGCCRDT extends CRDT {
    ServerGCCRDTRemoteListener remotelistener;

    public ServerGCCRDT(int agent, ServerGCCRDTRemoteListener remotelistener, CRDTLocalListener localListener) {
        super(agent, remotelistener, localListener);
    }

    @Override
    public void remoteInsert(CRDTItem item, boolean increaseClock) {
        this.remotelistener.onRemoteCRDTInsert(new ServerTransaction() {
            @Override
            public Pair<CRDTItem, List<CRDTItem>> execute() {
                try {
                    lock.lock();
                    List<CRDTItem> conflictGC = new ArrayList<>();
                    integrate(item, conflictGC);
                    if (increaseClock)
                        versionVector.put(item);
                    return new Pair<CRDTItem, List<CRDTItem>>(item, conflictGC);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
                return null;
            }

        });
    }

    protected void integrate(CRDTItem bitem, List<CRDTItem> conflictGC) {
        GCCRDTItem item = new GCCRDTItem(bitem);
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

            Set<CRDTItem> conflictingItems = new HashSet<>();
            Set<CRDTItem> itemsBeforeOrigin = new HashSet<>();

            // rule 1 as breaking condition
            while (o != null && o != item.right) {
                itemsBeforeOrigin.add(o);
                conflictingItems.add(o);

                // collect conflicting gc
                if (o.isDeleted()) {
                    conflictGC.add(o);
                }

                if (o.originLeft.equals(item.originLeft)) {
                    if (o.id.agent < item.id.agent) {
                        left = o;
                        conflictingItems.clear();
                    } else if (o.originRight.equals(item.originRight)) {
                        break;
                    }
                } else if (o.originLeft != null && itemsBeforeOrigin.contains(o.originLeft)) {
                    if (!conflictingItems.contains(o.originLeft)) {
                        left = o;
                        conflictingItems.clear();
                    }
                } else {
                    break;
                }
                o = o.right;
            }
            item.left = left;
        }

        // connect linked list
        if (item.left != null) {
            CRDTItem right = item.left.right;
            item.right = right;
            item.left.right = item;
        } else {
            CRDTItem right = this.start;
            start = item;
            item.right = right;
        }

        if (item.right != null) {
            item.right.left = item;
        }
    }

    @Override
    public void delete(CRDTItem item) {
        GCCRDTItem gcItem = new GCCRDTItem(item);
        super.delete(gcItem);
    }

}
