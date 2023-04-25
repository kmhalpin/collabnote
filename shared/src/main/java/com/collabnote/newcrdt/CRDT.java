package com.collabnote.newcrdt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.util.Pair;

public class CRDT {
    protected int agent;

    // make sure crdt replica used by 1 thread only on each operation
    protected ReentrantLock lock;
    // cache last local inserts
    protected MarkerManager markerManager;

    ArrayList<CRDTItemSerializable> insertQueue;
    ArrayList<CRDTItemSerializable> deleteQueue;

    protected VersionVectors versionVector;
    protected CRDTItem start;

    protected CRDTRemoteListener remotelistener;
    protected CRDTLocalListener localListener;

    public CRDT(int agent, CRDTRemoteListener remotelistener, CRDTLocalListener localListener) {
        this.remotelistener = remotelistener;
        this.localListener = localListener;
        this.agent = agent;
        this.markerManager = new MarkerManager();
        this.lock = new ReentrantLock(true);
        this.versionVector = new VersionVectors();
        this.start = null;
        this.insertQueue = new ArrayList<>(0);
        this.deleteQueue = new ArrayList<>(0);
    }

    public CRDTItem bindItem(CRDTItemSerializable item, CRDTItem bitem) {
        if (item.originLeft != null
                && item.originLeft.agent != item.id.agent
                && !versionVector.exists(item.originLeft)) {
            return null;
        }
        if (item.originRight != null
                && item.originRight.agent != item.id.agent
                && !versionVector.exists(item.originRight)) {
            return null;
        }

        if (item.originLeft != null) {
            bitem.left = bitem.originLeft = versionVector.find(item.originLeft);
        }
        if (item.originRight != null) {
            bitem.right = bitem.originRight = versionVector.find(item.originRight);
        }

        return bitem;
    }

    public CRDTItem bindItem(CRDTItemSerializable item) {
        CRDTItem bitem = new CRDTItem(
                item.content,
                item.id,
                null,
                null,
                item.isDeleted,
                null,
                null);

        return this.bindItem(item, bitem);
    }

    public CRDTItem getStart() {
        return start;
    }

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
            }
            temp = temp.right;
        }

        // iterate left if right empty
        if (temp == null) {
            for (temp = p; item != temp && temp != null;) {
                if (!temp.isDeleted() || temp == item) {
                    offset -= 1;
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

    protected Position findPosition(int index) {
        // check cache
        Marker marker = markerManager.findMarker(this.start, index);
        if (marker != null) {
            Position pos = new Position(marker.item.left, marker.item, marker.index);
            return findNextPosition(pos, index - marker.index);
        } else {
            Position pos = new Position(null, this.start, 0);
            return findNextPosition(pos, index);
        }
    }

    Position findNextPosition(Position pos, int count) {
        while (pos.right != null && count > 0) {
            if (!pos.right.isDeleted()) {
                pos.index += 1;
                count -= 1;
            }
            pos.left = pos.right;
            pos.right = pos.right.right;
        }
        return pos;
    }

    public CRDTItem localInsert(int pos, String value) {
        CRDTItem item = new CRDTItem(value,
                new CRDTID(agent, versionVector.get(agent) + 1),
                null,
                null,
                false,
                null,
                null);
        return this.localInsert(pos, value, item);
    }

    public CRDTItem localInsert(int pos, String value, CRDTItem item) {
        try {
            this.lock.lock();
            Position p = findPosition(pos);
            item.left = item.originLeft = p.left;
            item.right = item.originRight = p.right;
            integrate(item);
            versionVector.put(item);
            if (markerManager.marker != null) {
                markerManager.updateMarker(p.index, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.lock.unlock();
        }
        this.localListener.afterLocalCRDTInsert(item);
        return item;
    }

    public ArrayList<CRDTItem> localDelete(int pos, int length) {
        ArrayList<CRDTItem> deleted = new ArrayList<>();
        try {
            this.lock.lock();
            Position p = findPosition(pos);
            int startLen = length;

            while (length > 0 && p.right != null) {
                if (!p.right.isDeleted()) {
                    length -= 1;
                    p.right.setDeleted();
                    deleted.add(p.right);
                }
                p.forward();
            }

            if (markerManager.marker != null) {
                markerManager.updateMarker(p.index, -startLen + length);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.lock.unlock();
        }
        this.localListener.afterLocalCRDTDelete(deleted);
        return deleted;
    }

    public void tryRemoteInsert(CRDTItemSerializable item) {
        if (versionVector.exists(item.id)) {
            return;
        }

        insertQueue.add(0, item);

        boolean cont = true;
        while (cont && insertQueue.size() > 0) {
            cont = false;
            ArrayList<CRDTItemSerializable> missing = new ArrayList<>(0);

            for (CRDTItemSerializable i : insertQueue) {
                if (versionVector.exists(i.id)) {
                    continue;
                }

                CRDTItem bitem = this.bindItem(item);
                if (bitem != null) {
                    remoteInsert(bitem, true);
                    cont = true;
                } else {
                    missing.add(i);
                }
            }

            insertQueue = missing;
        }

        // try clear delete queue
        ArrayList<CRDTItemSerializable> missing = new ArrayList<>(0);
        for (CRDTItemSerializable i : deleteQueue) {
            if (versionVector.exists(i.id)) {
                CRDTItem bitem = versionVector.find(i.id);
                delete(bitem);
            } else {
                missing.add(i);
            }
        }
        deleteQueue = missing;
    }

    public void tryRemoteDelete(CRDTItemSerializable item) {
        if (versionVector.exists(item.id)) {
            CRDTItem bitem = versionVector.find(item.id);
            delete(bitem);
        } else {
            deleteQueue.add(item);
        }
    }

    protected void delete(CRDTItem item) {
        if (!item.isDeleted()) {
            this.remotelistener.onRemoteCRDTDelete(new Transaction() {

                @Override
                public Pair<Integer, CRDTItem> execute() {
                    try {
                        lock.lock();
                        item.setDeleted();
                        int index = findIndex(item);
                        return new Pair<Integer, CRDTItem>(index, item);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                    return null;
                }

            });
        }
    }

    public void remoteInsert(CRDTItem item, boolean newItem) {
        this.remotelistener.onRemoteCRDTInsert(new Transaction() {

            @Override
            public Pair<Integer, CRDTItem> execute() {
                try {
                    lock.lock();
                    integrate(item);
                    if (newItem)
                        versionVector.put(item);
                    return new Pair<Integer, CRDTItem>(newItem ? findIndex(item) : -1, item);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
                return null;
            }

        });
    }

    protected void integrate(CRDTItem item) {
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

    public List<CRDTItemSerializable> serialize() {
        List<CRDTItemSerializable> list = new ArrayList<>();
        CRDTItem i = start;
        while (i != null) {
            list.add(i.serialize());
            i = i.right;
        }
        return list;
    }

    // follow this alg:
    // https://github.com/josephg/reference-crdts/blob/9f4f9c3a97b497e2df8ae4473d1e521d3c3bf2d2/crdts.ts#L350
    // private void integrate(CRDTItemSerializable item) {

    // int left = item.originLeft == null ? -1 : findItemIndex(item.originLeft);
    // int dst = left + 1;
    // int right = item.originRight == null ? operations.size() :
    // findItemIndex(item.originRight);

    // boolean scanning = true;

    // for (int i = dst;; i++) {
    // if (!scanning)
    // dst = i;
    // if (i == operations.size())
    // break;
    // if (i == right)
    // break;
    // CRDTItemSerializable o = operations.get(i);
    // int oleft = o.originLeft == null ? -1 : findItemIndex(o.originLeft);
    // int oright = o.originRight == null ? operations.size() :
    // findItemIndex(o.originRight);
    // if (oleft < left)
    // break;
    // else if (oleft == left) {
    // if (oright == right) { // conflicting
    // if (item.id.agent.compareTo(o.id.agent) > 0) // compare site winner
    // break;
    // else {
    // scanning = false;
    // continue;
    // }
    // } else if (oright < right) {
    // scanning = true;
    // continue;
    // } else {
    // scanning = false;
    // continue;
    // }
    // }
    // }
    // }

    // enum Resolve {
    // BREAK,
    // CONTINUE,
    // INCREASE_POS,
    // }

    // Resolve resolveConflict(int oleft, int o, int left, String oagent, String
    // itemagent) {
    // // Rule 2
    // // Search for the last operation
    // // that is to the left of i (left)
    // if ((o < left
    // || left <= oleft) // Rule 1: o1 < origin2 ∨ origin2 ≤ origin1
    // && (oleft != left
    // || oagent.compareTo(itemagent) > 0)) { // Rule 3: origin1 ≡ origin2 →
    // creator1 < creator2
    // // Rule 1 + 3
    // // If this formula is fulfiled
    // // item is successor of origin
    // return Resolve.INCREASE_POS; // item index = origin index + 1
    // } else {
    // if (left > oleft) {
    // // Rule 1 is no longer satisfied
    // // since otherwise origin connections would cross
    // return Resolve.BREAK;
    // }
    // }
    // return Resolve.CONTINUE;
    // }
}
