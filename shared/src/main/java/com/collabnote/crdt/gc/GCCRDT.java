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
    public boolean GC(List<DeleteGroupSerializable> items) {
        ArrayList<GCCRDTItem> delimiters = new ArrayList<>();
        boolean gcStable = true;

        boolean isLeft = true;
        for (int i = 0; i < items.size();) {
            CRDTItemSerializable item = isLeft ? items.get(i).leftDeleteGroup : items.get(i).rightDeleteGroup;
            GCCRDTItem fitem = (GCCRDTItem) versionVector.find(item.id);
            if (fitem == null || fitem.getGc())
                throw new NoSuchElementException("not expected");

            // make sure everything deleted
            if (!fitem.isDeleted())
                delete(fitem);

            delimiters.add(fitem);

            if (!isLeft) {
                i++;
            }
            isLeft = !isLeft;
        }

        ArrayList<GCCRDTItem> gcItems = new ArrayList<>(delimiters.size());
        for (int i = 0; i < delimiters.size(); i += 2) {
            GCCRDTItem gcItemLeftDelimiter = delimiters.get(i);
            GCCRDTItem gcItemRightDelimiter = delimiters.get(i + 1);
            boolean isDeleteGroupStable = true;

            GCCRDTItem o = (GCCRDTItem) gcItemLeftDelimiter.right;
            // delete group stable if not splitted
            while (o != gcItemRightDelimiter && (isDeleteGroupStable = !o.isDeleteGroupDelimiter())) {
                o = (GCCRDTItem) o.right;
            }

            if (isDeleteGroupStable) {
                gcItems.add(gcItemLeftDelimiter);
                gcItems.add(gcItemRightDelimiter);
            } else {
                gcStable = false;
            }
        }

        // remove vector history without lock, version vector used by network threads
        // operation
        // should be safe
        ArrayList<GCCRDTItem> gcDeleteGroup = new ArrayList<>(delimiters.size());
        for (int i = 0; i < gcItems.size(); i += 2) {
            GCCRDTItem leftDelimiter = delimiters.get(i);
            GCCRDTItem rightdelimiter = gcItems.get(i + 1);
            leftDelimiter.setGc(false);
            rightdelimiter.setGc(false);

            GCCRDTItem o = (GCCRDTItem) leftDelimiter.right;

            gcDeleteGroup.add(leftDelimiter);
            while (o != rightdelimiter) {
                // cannot remove level base, and conflicting reference item, only removing when
                // item is stable in every replica
                if (!o.getLevelBase() && o.getLeftRefrencer() < 2 && o.getRightRefrencer() < 2) {
                    o.setGc(true);
                    o.left.right = o.right;
                    o.right.left = o.left;
                    versionVector.remove(o);
                } else {
                    o.setGc(false);
                    // TODO: set flag item is not gc'ed
                    gcDeleteGroup.add(o);
                    System.out.println("WHAT " + o.content);
                }
                o = (GCCRDTItem) o.right;
            }
            gcDeleteGroup.add(rightdelimiter);
        }

        // change gc delete group item origin
        for (GCCRDTItem i : gcDeleteGroup) {
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

        return gcStable;
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

                CRDTID leftId = deleteGroup.gcItems.get(i).originLeft;
                if (leftId != null && !versionVector.exists(leftId))
                    throw new NoSuchElementException("unexpected");
                GCCRDTItem itemOriginLeft = leftId != null ? (GCCRDTItem) versionVector.find(leftId) : null;

                CRDTID rightId = deleteGroup.gcItems.get(i).originRight;
                if (rightId != null && !versionVector.exists(rightId))
                    throw new NoSuchElementException("unexpected");
                GCCRDTItem itemOriginRight = rightId != null ? (GCCRDTItem) versionVector.find(rightId) : null;

                if (recoveredItem == null)
                    throw new NoSuchElementException("unexpected");

                // fix existing item, like delimiter
                recoveredItem.changeOriginLeft(itemOriginLeft);
                recoveredItem.changeOriginRight(itemOriginRight);
                if (recoveredItem.isDeleteGroupDelimiter())
                    continue;

                recoveredItem.setLevel();
                if (itemOriginLeft != null && !itemOriginLeft.isDeleteGroupDelimiter()
                        && itemOriginLeft.level == recoveredItem.level) {
                    itemOriginLeft.increaseRightRefrencer();
                }
                if (itemOriginRight != null && !itemOriginRight.isDeleteGroupDelimiter()
                        && itemOriginRight.level == recoveredItem.level) {
                    itemOriginRight.increaseLeftRefrencer();
                }
                recoveredItem.setDeleted();
            }
            latestCounter += deleteGroup.gcItems.size() - 1;
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
