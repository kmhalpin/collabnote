package com.collabnote.newcrdt.gc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import com.collabnote.newcrdt.CRDT;
import com.collabnote.newcrdt.CRDTItem;
import com.collabnote.newcrdt.CRDTItemSerializable;
import com.collabnote.newcrdt.CRDTLocalListener;
import com.collabnote.newcrdt.CRDTRemoteListener;
import com.collabnote.newcrdt.Marker;

public class GCCRDT extends CRDT {

    public GCCRDT(int agent, CRDTRemoteListener remotelistener, CRDTLocalListener localListener) {
        super(agent, remotelistener, localListener);
    }

    // find index with optimized search, skips deleted group
    @Override
    protected int findIndex(CRDTItem item) {
        if (this.start == item || this.start == null) {
            return 0;
        }

        Marker marker = markerManager.marker;

        CRDTItem p = marker != null ? marker.item : start;
        int offset = 0;

        // iterate right if possible
        CRDTItem temp = null;
        for (temp = p; item != temp && temp != null;) {
            if (!temp.isDeleted() || temp == item) {
                offset += 1;
            } else if (temp.isDeleted()) {
                // skips deleted group
                temp = ((GCCRDTItem) temp).rightDeleteGroup.right;
                continue;
            }
            temp = temp.right;
        }

        // iterate left if right empty
        if (temp == null) {
            for (temp = p; item != temp && temp != null;) {
                if (!temp.isDeleted() || temp == item) {
                    offset -= 1;
                } else if (temp.isDeleted()) {
                    temp = ((GCCRDTItem) temp).leftDeleteGroup.left;
                    continue;
                }
                temp = temp.left;
            }

            if (temp == null) {
                throw new NoSuchElementException("item gone");
            }

            if (marker != null) {
                markerManager.updateMarker(marker.index + offset, item.isDeleted() ? -1 : 1);
                return item.isDeleted() ? marker.index + offset + 1 : marker.index + offset - 1;
            }
        }

        return marker != null ? marker.index + offset : offset;
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
                if (!item.isDeleted() && o.isDeleted() && !((GCCRDTItem) o).isGarbageCollectable()) {
                    lastGroup = (GCCRDTItem) o;
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

        return lastGroup;
    }

    @Override
    protected void integrate(CRDTItem item) {
        GCCRDTItem gcItem = new GCCRDTItem(item);
        GCCRDTItem lastGroup = this.GCIntegrate(gcItem);
        if (gcItem.isDeleted()) {
            gcItem.setDeleted();
        } else {
            // check splitable
            gcItem.checkSplitGC(lastGroup);
        }
    }

    @Override
    public void delete(CRDTItem item) {
        GCCRDTItem gcItem = new GCCRDTItem(item);
        super.delete(gcItem);
    }

    // gc item
    private void remove(GCCRDTItem item) {
        versionVector.remove(item);
        item.right.left = item.left;
        item.left.right = item.right;
    }

    // client gc items expected marked and sorted by deleted group first
    public void GC(ArrayList<CRDTItemSerializable> item) {
        ArrayList<GCCRDTItem> gcitems = new ArrayList<>();
        for (CRDTItemSerializable i : item) {
            GCCRDTItem gcitem = (GCCRDTItem) i.bindItem(versionVector);
            if (gcitem == null) {
                throw new NoSuchElementException("not expected");
            }
            gcitem.gc = true;
            gcitems.add(gcitem);
        }

        for (GCCRDTItem gcitem : gcitems) {
            if (gcitem.isGarbageCollectable()) {
                this.remove(gcitem);
            } else if (gcitem.isDeleted()) {
                // change origin of deleted group to null if possible
                if (gcitem.originLeft != null && ((GCCRDTItem) gcitem.originLeft).gc) {
                    gcitem.originLeft = null;
                }
                if (gcitem.originRight != null && ((GCCRDTItem) gcitem.originRight).gc) {
                    gcitem.originRight = null;
                }
            }
        }
    }

    private void recoverDeleteGroup(CRDTItem gcItem, CRDTItem recoverItem) {
        try {
            lock.lock();
            integrate(recoverItem);
            versionVector.recover(recoverItem);
            ((GCCRDTItem) recoverItem).setDeleteGroupFrom((GCCRDTItem) gcItem); // move delete group
            // force remove old delete group
            remove((GCCRDTItem) gcItem);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
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
                        recoverDeleteGroup(fitem, bitem);
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

}
