package com.collabnote.crdt.gc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTID;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTItemSerializable;
import com.collabnote.crdt.CRDTLocalListener;
import com.collabnote.crdt.CRDTRemoteTransaction;

public class GCCRDT extends CRDT {
    protected boolean clientReplica;

    public GCCRDT(int agent, CRDTRemoteTransaction remotelistener, CRDTLocalListener localListener,
            boolean clientReplica) {
        super(agent, remotelistener, localListener);
        this.clientReplica = clientReplica;
    }

    public GCCRDT(int agent, CRDTRemoteTransaction remotelistener, CRDTLocalListener localListener) {
        this(agent, remotelistener, localListener, true);
    }

    @Override
    public CRDTItem bindItem(CRDTItemSerializable item) {
        GCCRDTItem bitem = new GCCRDTItem(
                item.content,
                item.id,
                item.isDeleted,
                null,
                null);

        return this.bindItem(item, bitem);
    }

    // find index with optimized search, skips deleted group
    @Override
    protected int findIndex(CRDTItem item) {
        return super.findIndex(item);
        // if (!this.clientReplica || this.start == item || this.start == null) {
        // return 0;
        // }

        // Marker marker = markerManager.marker;

        // CRDTItem p = marker != null ? marker.item : start;
        // int offset = 0;

        // // iterate right if possible
        // CRDTItem temp = null;
        // for (temp = p; item != temp && temp != null;) {
        // if (!temp.isDeleted() || temp == item) {
        // offset += 1;
        // } else if (temp.isDeleted()) {
        // // skips deleted group
        // temp = ((GCCRDTItem) temp).rightDeleteGroup;
        // }
        // temp = temp.right;
        // }

        // // iterate left if right empty
        // if (temp == null) {
        // offset = 0;
        // for (temp = p; item != temp && temp != null;) {
        // if (!temp.isDeleted() || temp == item) {
        // offset -= 1;
        // } else if (temp.isDeleted()) {
        // temp = ((GCCRDTItem) temp).leftDeleteGroup;
        // }
        // temp = temp.left;
        // }

        // if (temp == null) {
        // throw new NoSuchElementException("item gone");
        // }

        // if (marker != null) {
        // markerManager.updateMarker(marker.index + offset, item.isDeleted() ? -1 : 1);
        // return item.isDeleted() ? marker.index + offset + 1 : marker.index + offset -
        // 1;
        // }
        // }

        // return marker != null ? marker.index + offset : offset;
    }

    // integrate with returning last conflicting delete group
    private GCCRDTItem GCIntegrate(CRDTItem item) {
        GCCRDTItem lastGroup = null;
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

                // if item is not deleted, find latest conflicting delete group
                if (!item.isDeleted() && ((GCCRDTItem) o).isDeleteGroupDelimiter()) {
                    lastGroup = (GCCRDTItem) o;
                }

                if (o.getOriginLeft() == item.getOriginLeft()
                        || (o.getOriginLeft() != null && item.getOriginLeft() != null
                                && o.getOriginLeft().id.agent == item.getOriginLeft().id.agent
                                && o.getOriginLeft().id.seq == item.getOriginLeft().id.seq)) {
                    if (o.id.agent < item.id.agent) {
                        left = o;
                        conflictingItems.clear();
                    } else if (o.getOriginRight() == item.getOriginRight()
                            || (o.getOriginRight() != null && item.getOriginRight() != null
                                    && o.getOriginRight().id.agent == item.getOriginRight().id.agent
                                    && o.getOriginRight().id.seq == item.getOriginRight().id.seq)) {
                        break;
                    }
                } else if (o.getOriginLeft() != null && itemsBeforeOrigin.contains(o.getOriginLeft())) {
                    if (!conflictingItems.contains(o.getOriginLeft())) {
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

        return lastGroup;
    }

    @Override
    protected void integrate(CRDTItem item) {
        GCCRDTItem lastGroup = this.GCIntegrate(item);
        ((GCCRDTItem) item).checkSplitGC(lastGroup);
        if (item.isDeleted()) {
            item.setDeleted();
        }
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
    public boolean GC(List<CRDTItemSerializable> item) {
        ArrayList<GCCRDTItem> delimiters = new ArrayList<>();
        boolean gcSuccess = true;

        for (CRDTItemSerializable i : item) {
            GCCRDTItem fitem = (GCCRDTItem) versionVector.find(i.id);
            if (fitem == null)
                throw new NoSuchElementException("not expected");

            // make sure everything deleted
            if (!fitem.isDeleted())
                delete(fitem);

            delimiters.add(fitem);
        }

        ArrayList<GCCRDTItem> gcItems = new ArrayList<>(delimiters.size());
        for (int i = 0; i < delimiters.size(); i += 2) {
            GCCRDTItem gcItemLeftDelimiter = delimiters.get(i);
            GCCRDTItem gcItemStart = (GCCRDTItem) gcItemLeftDelimiter.right;
            GCCRDTItem gcItemRightDelimiter = delimiters.get(i + 1);
            boolean isDelimiter;

            // gc delete group if delimiter not changed
            this.lock.lock();
            isDelimiter = gcItemLeftDelimiter.isDeleteGroupDelimiter() && gcItemRightDelimiter.isDeleteGroupDelimiter()
                    && gcItemLeftDelimiter.rightDeleteGroup == gcItemRightDelimiter
                    && gcItemRightDelimiter.leftDeleteGroup == gcItemLeftDelimiter;
            if (isDelimiter) {
                gcItemLeftDelimiter.right = gcItemRightDelimiter;
                gcItemRightDelimiter.left = gcItemLeftDelimiter;
            }
            this.lock.unlock();

            // separate from lock
            if (isDelimiter) {
                gcItems.add(gcItemStart);
                gcItems.add(gcItemRightDelimiter);
            } else {
                gcSuccess = false;
            }
        }

        // remove vector history without lock, version vector used by network threads
        // operation
        // should be safe
        ArrayList<GCCRDTItem> conflictedGCItems = new ArrayList<>(delimiters.size());
        for (int i = 0; i < gcItems.size(); i += 2) {
            GCCRDTItem o = (GCCRDTItem) gcItems.get(i);
            GCCRDTItem rightdelimiter = gcItems.get(i + 1);
            while (o != rightdelimiter) {
                o.setGc(true);
                // cannot remove level base, and conflicting reference item, only removing when
                // item is stable in every replica
                if (!o.getLevelBase() && o.getLeftRefrencer() <= 1 && o.getRightRefrencer() <= 1) {
                    versionVector.remove(o);
                } else
                    conflictedGCItems.add(o);
                o = (GCCRDTItem) o.right;
            }
        }

        // scan delimiter gc origin
        for (int i = 0; i < delimiters.size(); i += 2) {
            GCCRDTItem gcItemLeftDelimiter = delimiters.get(i);
            GCCRDTItem gcItemRightDelimiter = delimiters.get(i + 1);
            if (gcItemLeftDelimiter.getOriginRight() != null
                    && ((GCCRDTItem) gcItemLeftDelimiter.getOriginRight()).isGarbageCollectable()
                    && ((GCCRDTItem) gcItemLeftDelimiter.getOriginRight()).getGc()) {
                gcItemLeftDelimiter.removeOriginRight();
            }
            if (gcItemRightDelimiter.getOriginLeft() != null
                    && ((GCCRDTItem) gcItemRightDelimiter.getOriginLeft()).isGarbageCollectable()
                    && ((GCCRDTItem) gcItemRightDelimiter.getOriginLeft()).getGc()) {
                gcItemRightDelimiter.removeOriginLeft();
            }
        }

        // scan conflicted gc item origin
        for (GCCRDTItem i : conflictedGCItems) {
            if (i.getOriginRight() != null
                    && ((GCCRDTItem) i.getOriginRight()).isGarbageCollectable()
                    && ((GCCRDTItem) i.getOriginRight()).getGc()) {
                i.removeOriginRight();
            }
            if (i.getOriginLeft() != null
                    && ((GCCRDTItem) i.getOriginLeft()).isGarbageCollectable()
                    && ((GCCRDTItem) i.getOriginLeft()).getGc()) {
                i.removeOriginLeft();
            }
            i.left = i.right = null;
        }

        return gcSuccess;
    }

    // client recover can be run concurrently
    // runs by network thread
    public void recover(List<CRDTItemSerializable> gcItems, CRDTItemSerializable item) {
        ArrayList<CRDTItem> recoveredItems = new ArrayList<>(gcItems.size());
        ArrayList<CRDTItemSerializable> newItems = new ArrayList<>();
        if (item != null)
            newItems.add(item);

        // recover state
        GCCRDTItem left = null;
        for (CRDTItemSerializable i : gcItems) {
            GCCRDTItem gcItem = null;
            try {
                if (versionVector.exists(i.id))
                    gcItem = (GCCRDTItem) versionVector.find(i.id);
                else {
                    newItems.add(i);
                    gcItems.remove(i);
                    continue;
                }
            } catch (NoSuchElementException e) {
            }

            if (gcItem != null) {
                gcItem.setGc(false);
            }

            if (gcItem != null && gcItem.isDeleteGroupDelimiter() && gcItem.leftDeleteGroup == gcItem) {
                // left delimiter
                left = gcItem;
            } else if (gcItem != null && gcItem.isDeleteGroupDelimiter() && gcItem.rightDeleteGroup == gcItem) {
                // right delimiter
                left.right = gcItem;
                gcItem.left = left;
                left = null;
            } else {
                // gc item
                if (gcItem == null) {
                    gcItem = new GCCRDTItem(
                            i.content,
                            i.id,
                            i.isDeleted,
                            left,
                            null);
                    versionVector.recover(gcItem);
                } else {
                    gcItem.left = left;
                }
                left.right = gcItem;
                left = gcItem;
            }

            recoveredItems.add(gcItem);
        }

        // recover state origin
        for (int i = 0; i < gcItems.size(); i++) {
            GCCRDTItem bItem = (GCCRDTItem) this.bindItem(gcItems.get(i));
            CRDTItem recoveredItem = recoveredItems.get(i);

            if (bItem == null || recoveredItem == null)
                throw new NoSuchElementException("unexpected");

            // fix existing item, like delimiter
            recoveredItem.setOrigin(bItem.getOriginLeft(), bItem.getOriginRight());
            recoveredItem.setDeleted();
        }

        // concurrently split gc item to optimize soon

        // insert item
        for (CRDTItemSerializable i : newItems)
            tryRemoteInsert(i);
    }

    @Override
    public List<CRDTItem> getItems() {
        List<CRDTItem> list = new ArrayList<>();
        GCCRDTItem i = (GCCRDTItem) start;
        while (i != null) {
            // TODO: remove print
            System.out.print("{ "
                    + (i.getOriginLeft() != null ? (i.getOriginLeft().id.agent + "-" + i.getOriginLeft().id.seq) : null)
                    + ", " + i.content + ", "
                    + (i.rightDeleteGroup != null && i.leftDeleteGroup != null ? "DG" : null) + ", "
                    + (i.isDeleted() ? "DELETED" : null) + ", " + i.level
                    + ", "
                    + (i.getOriginRight() != null ? (i.getOriginRight().id.agent + "-" + i.getOriginRight().id.seq)
                            : null)
                    + " }");
            list.add(i);
            i = (GCCRDTItem) i.right;
        }
        System.out.println();
        return list;
    }

}
