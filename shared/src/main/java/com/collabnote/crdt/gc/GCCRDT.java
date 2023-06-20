package com.collabnote.crdt.gc;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTID;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTItemSerializable;
import com.collabnote.crdt.CRDTLocalListener;
import com.collabnote.crdt.CRDTRemoteTransaction;

public class GCCRDT extends CRDT {
    public GCCRDT(int agent, CRDTRemoteTransaction remotelistener, CRDTLocalListener localListener) {
        super(agent, remotelistener, localListener);
    }

    @Override
    public void tryRemoteDelete(CRDTItemSerializable item) {
        if (versionVector.exists(item.id)) {
            try {
                CRDTItem bitem = versionVector.find(item.id);
                if (bitem != null)
                    delete(bitem);
            } catch (NoSuchElementException e) {
            }
        } else {
            deleteQueue.add(item);
        }
    }

    @Override
    public CRDTItem bindItem(CRDTItemSerializable item) {
        GCCRDTItem bitem = new GCCRDTItem(
                item.content,
                item.id,
                item.isDeleted,
                null,
                null);

        // make stable gc'ed item to increase vector version only
        if (item.isGC) {
            bitem.setServerGc(true);
            return bitem;
        }

        return this.bindItem(item, bitem);
    }

    @Override
    protected void integrate(CRDTItem item) {
        if (((GCCRDTItem) item).getServerGc()) {
            return;
        }
        super.integrate(item);
    }

    @Override
    public CRDTItem localInsert(int pos, String value) {
        CRDTItem item = new GCCRDTItem(value,
                new CRDTID(agent, versionVector.get(agent) + 1),
                false,
                null,
                null);
        return this.localInsert(pos, value, item);
    }

    // client gc items expected marked and sorted by deleted group first
    // runs by network thread
    public List<DeleteGroupSerializable> GC(List<DeleteGroupSerializable> items) {
        List<DeleteGroupSerializable> stableDeleteGroup = new ArrayList<>();

        ArrayList<GCCRDTItem> stableDelimiters = new ArrayList<>(items.size() * 2);

        for (DeleteGroupSerializable i : items) {
            GCCRDTItem leftDelimiter;
            GCCRDTItem rightDelimiter;
            try {
                leftDelimiter = (GCCRDTItem) versionVector.find(i.leftDeleteGroup.id);
                if (!leftDelimiter.isDeleted)
                    delete(leftDelimiter);

                rightDelimiter = (GCCRDTItem) versionVector.find(i.rightDeleteGroup.id);

                if (leftDelimiter.getGc() || rightDelimiter.getGc())
                    throw new NoSuchElementException("group has been removed");
            } catch (NoSuchElementException e) {
                continue;
            }

            boolean isDeleteGroupStable = true;

            GCCRDTItem o = (GCCRDTItem) leftDelimiter.right;
            // delete group stable if not splitted
            while (o != rightDelimiter && (isDeleteGroupStable = o.isGarbageCollectable())) {
                o = (GCCRDTItem) o.right;
            }

            if (isDeleteGroupStable) {
                stableDelimiters.add(leftDelimiter);
                stableDelimiters.add(rightDelimiter);
                stableDeleteGroup.add(i);
            }
        }

        // remove vector history without lock, version vector used by network threads
        // operation
        // should be safe
        ArrayList<GCCRDTItem> unremovable = new ArrayList<>(stableDelimiters.size());
        for (int i = 0; i < stableDelimiters.size(); i += 2) {
            GCCRDTItem leftDelimiter = stableDelimiters.get(i);
            GCCRDTItem rightdelimiter = stableDelimiters.get(i + 1);
            // delimiter, level base, conflicted item origin, must set gc to false
            leftDelimiter.setGc(false);
            rightdelimiter.setGc(false);

            GCCRDTItem o = (GCCRDTItem) leftDelimiter.right;

            unremovable.add(leftDelimiter);
            while (o != rightdelimiter) {
                // cannot remove level base, and conflicting reference item, only removing when
                // item is stable in every replica
                if (!o.getLevelBase() && o.getLeftRefrencer() < 2 && o.getRightRefrencer() < 2) {
                    o.setGc(true);
                    o.left.right = o.right;
                    if (o.right != null)
                        o.right.left = o.left;
                    versionVector.remove(o);
                } else {
                    o.setGc(false);
                    unremovable.add(o);
                }

                if (o == rightdelimiter)
                    break;

                o = (GCCRDTItem) o.right;
            }
            unremovable.add(rightdelimiter);
        }

        // change gc delete group item origin
        for (GCCRDTItem i : unremovable) {
            if (i.getOriginLeft() != null
                    && ((GCCRDTItem) i.getOriginLeft()).isGarbageCollectable()
                    && ((GCCRDTItem) i.getOriginLeft()).getGc()) {
                if (i.left.getOriginRight() != i)
                    i.changeOriginLeft(i.left);
                else
                    // set as a new level starter item
                    i.changeOriginLeft(i.left.getOriginLeft());
            }
            if (i.getOriginRight() != null
                    && ((GCCRDTItem) i.getOriginRight()).isGarbageCollectable()
                    && ((GCCRDTItem) i.getOriginRight()).getGc()) {
                i.changeOriginRight(i.right);
            }
        }

        return stableDeleteGroup;
    }

    // client recover can be run concurrently
    // runs by network thread
    public void recover(List<DeleteGroupSerializable> deleteGroupItems, CRDTItemSerializable newItem) {
        ArrayList<GCCRDTItem> recoveredItems = new ArrayList<>(deleteGroupItems.size());
        ArrayList<CRDTItemSerializable> newItems = new ArrayList<>();
        if (newItem != null)
            newItems.add(newItem);

        // recover state
        for (DeleteGroupSerializable deleteGroup : deleteGroupItems) {
            GCCRDTItem left = null;
            for (CRDTItemSerializable i : deleteGroup.gcItems) {
                GCCRDTItem gcItem = null;
                try {
                    if (versionVector.exists(i.id))
                        gcItem = (GCCRDTItem) versionVector.find(i.id);
                    else {
                        newItems.add(i);
                        deleteGroup.gcItems.remove(i);
                        continue;
                    }
                } catch (NoSuchElementException e) {
                }

                // expected first left is dg left delimiter
                if (left == null) {
                    if (gcItem == null) {
                        throw new RuntimeException("unexpected");
                    }
                    left = gcItem;
                } else {
                    if (gcItem == null) {
                        gcItem = new GCCRDTItem(
                                i.content,
                                i.id,
                                i.isDeleted,
                                left,
                                null);
                        // set gc to flag item was gc
                        gcItem.setGc(true);
                        versionVector.recover(gcItem);
                    } else {
                        gcItem.left = left;
                    }

                    left.right = gcItem;
                    left = gcItem;
                }

                recoveredItems.add(gcItem);
            }
        }

        // recover state origin
        int latestCounter = 0;
        for (DeleteGroupSerializable deleteGroup : deleteGroupItems) {
            for (int i = 0; i < deleteGroup.gcItems.size(); i++) {
                GCCRDTItem recoveredItem = recoveredItems.get(latestCounter + i);

                if (recoveredItem == null)
                    throw new NoSuchElementException("unexpected");

                CRDTID leftId = deleteGroup.gcItems.get(i).originLeft;
                if (leftId != null && !versionVector.exists(leftId))
                    throw new NoSuchElementException("unexpected");
                GCCRDTItem itemOriginLeft = leftId != null ? (GCCRDTItem) versionVector.find(leftId) : null;

                CRDTID rightId = deleteGroup.gcItems.get(i).originRight;
                if (rightId != null && !versionVector.exists(rightId))
                    throw new NoSuchElementException("unexpected");
                GCCRDTItem itemOriginRight = rightId != null ? (GCCRDTItem) versionVector.find(rightId) : null;

                // fix existing item, like delimiter
                recoveredItem.changeOriginLeft(itemOriginLeft);
                recoveredItem.changeOriginRight(itemOriginRight);

                // recover gc item level and its origin level base
                if (recoveredItem.getGc())
                    recoveredItem.setLevel();

                // recover gc item references status
                if (itemOriginLeft != null && itemOriginLeft.level == recoveredItem.level
                        && itemOriginLeft.getGc())
                    itemOriginLeft.increaseRightRefrencer();

                if (itemOriginRight != null && itemOriginRight.level == recoveredItem.level
                        && itemOriginRight.getGc())
                    itemOriginRight.increaseLeftRefrencer();

                // recover delete group
                if (recoveredItem.isGarbageCollectable())
                    this.setDeleted(recoveredItem);
            }
            latestCounter += deleteGroup.gcItems.size() - 1;
        }

        for (GCCRDTItem i : recoveredItems)
            i.setGc(false);

        // insert item
        for (CRDTItemSerializable i : newItems)
            tryRemoteInsert(i);
    }
}
