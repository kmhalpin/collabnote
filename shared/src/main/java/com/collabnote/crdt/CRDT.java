package com.collabnote.crdt;

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
    // cache last local inserts (unused now)
    protected MarkerManager markerManager;

    ArrayList<CRDTItemSerializable> insertQueue;
    ArrayList<CRDTItemSerializable> deleteQueue;

    protected VersionVectors versionVector;
    protected CRDTItem start;

    protected CRDTRemoteTransaction remoteTransaction;
    protected CRDTLocalListener localListener;

    public CRDT(int agent, CRDTRemoteTransaction remoteTransaction, CRDTLocalListener localListener) {
        this.remoteTransaction = remoteTransaction;
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
                && !versionVector.exists(item.originLeft)) {
            return null;
        }
        if (item.originRight != null
                && !versionVector.exists(item.originRight)) {
            return null;
        }

        CRDTItem originLeft = null, originRight = null;
        if (item.originLeft != null) {
            bitem.left = originLeft = versionVector.find(item.originLeft);
        }
        if (item.originRight != null) {
            bitem.right = originRight = versionVector.find(item.originRight);
        }
        bitem.setOrigin(originLeft, originRight);

        return bitem;
    }

    public CRDTItem bindItem(CRDTItemSerializable item) {
        CRDTItem bitem = new CRDTItem(
                item.content,
                item.id,
                item.isDeleted,
                null,
                null);

        return this.bindItem(item, bitem);
    }

    public CRDTItem getStart() {
        return start;
    }

    public VersionVectors getVersionVector() {
        return versionVector;
    }

    protected int findIndex(CRDTItem item) {
        if (this.start == item || this.start == null) {
            return 0;
        }

        CRDTItem p = start;
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
            offset = 0;
            for (temp = p; item != temp && temp != null;) {
                if (!temp.isDeleted() || temp == item) {
                    offset -= 1;
                }
                temp = temp.left;
            }

            if (temp == null) {
                throw new NoSuchElementException("item gone");
            }
        }

        return offset;
    }

    protected Position findPosition(int index) {
        // check cache
        // Marker marker = markerManager.findMarker(this.start, index);
        // if (marker != null) {
        // Position pos = new Position(marker.item.left, marker.item, marker.index);
        // return findNextPosition(pos, index - marker.index);
        // } else {
        Position pos = new Position(null, this.start, 0);
        return findNextPosition(pos, index);
        // }
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
                false,
                null,
                null);
        return this.localInsert(pos, value, item);
    }

    public CRDTItem localInsert(int pos, String value, CRDTItem item) {
        try {
            this.lock.lock();
            Position p = findPosition(pos);
            item.left = p.left;
            item.right = p.right;
            item.setOrigin(p.left, p.right);
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
            if (item.isDeleted)
                tryRemoteDelete(item);
            return;
        }

        insertQueue.add(0, item);

        boolean cont = true;
        while (cont && insertQueue.size() > 0) {
            cont = false;
            ArrayList<CRDTItemSerializable> missing = new ArrayList<>(0);

            for (CRDTItemSerializable i : insertQueue) {
                if (versionVector.exists(i.id)) {
                    if (i.isDeleted)
                        tryRemoteDelete(i);
                    continue;
                }

                if (versionVector.get(i.id.agent) == i.id.seq - 1) {
                    CRDTItem bitem = this.bindItem(i);
                    if (bitem != null) {
                        remoteInsert(bitem, true);
                        cont = true;
                        continue;
                    }
                }

                missing.add(i);
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
            this.remoteTransaction.onRemoteCRDTDelete(new Transaction(item) {

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
        this.remoteTransaction.onRemoteCRDTInsert(new Transaction(item) {

            @Override
            public Pair<Integer, CRDTItem> execute() {
                try {
                    lock.lock();
                    integrate(item);
                    if (newItem)
                        versionVector.put(item);
                    return new Pair<Integer, CRDTItem>(newItem && !item.isDeleted() ? findIndex(item) : -1, item);
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
    }

    public List<CRDTItem> getItems() {
        List<CRDTItem> list = new ArrayList<>();
        CRDTItem i = start;
        while (i != null) {
            list.add(i);
            i = i.right;
        }
        return list;
    }

    public void loadCRDT(CRDTItem start, VersionVectors versionVectors) {
        this.insertQueue = new ArrayList<>(0);
        this.deleteQueue = new ArrayList<>(0);
        this.start = start;
        this.versionVector = versionVectors;

        Position p = findPosition(0);
        while (p.right != null) {
            this.remoteTransaction.onRemoteCRDTInsert(new Transaction(p.right) {
                @Override
                public Pair<Integer, CRDTItem> execute() {
                    try {
                        lock.lock();
                        return new Pair<Integer, CRDTItem>(p.index, p.right);
                    } finally {
                        lock.unlock();
                    }
                }
            });
            p.forward();
        }
    }

    // follow this alg:
    // https://github.com/josephg/reference-crdts/blob/9f4f9c3a97b497e2df8ae4473d1e521d3c3bf2d2/crdts.ts#L350
}
