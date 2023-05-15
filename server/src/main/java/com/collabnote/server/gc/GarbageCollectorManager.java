package com.collabnote.server.gc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.util.Pair;

import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTItemSerializable;
import com.collabnote.crdt.CRDTRemoteTransaction;
import com.collabnote.crdt.Transaction;
import com.collabnote.crdt.gc.DeleteGroupSerializable;
import com.collabnote.crdt.gc.GCCRDT;
import com.collabnote.crdt.gc.GCCRDTItem;
import com.collabnote.server.collaborate.Collaborate;
import com.collabnote.socket.DataPayload;

public class GarbageCollectorManager extends Thread implements CRDTRemoteTransaction {
    private ReentrantLock lock;
    private GCCRDT crdt;
    private Collaborate collaborate;

    @Override
    public void run() {
        do {
            try {
                Thread.sleep(30000);

                try {
                    lock.lock();
                    System.out.println("GC START");
                    List<GCCRDTItem> gcDelimiters = new ArrayList<>();
                    GCCRDTItem ops = (GCCRDTItem) this.crdt.getStart();

                    boolean isInsideDeleteGroup = false;
                    boolean includeDeleteGroup = false;
                    while (ops != null) {
                        // group checking
                        if (ops.isGarbageCollectable()) {
                            // set selected delete group operations to gc
                            includeDeleteGroup = true;
                            ops.setGc(true);
                        } else if (ops.isDeleteGroupDelimiter()
                                && ops.leftDeleteGroup != ops.rightDeleteGroup // skip standalone delimiter
                        ) {
                            isInsideDeleteGroup = !isInsideDeleteGroup;
                            if (isInsideDeleteGroup) { // left delimiter
                                includeDeleteGroup = false;
                            } else { // right delimiter
                                if (includeDeleteGroup) {
                                    gcDelimiters.add(ops.leftDeleteGroup);
                                    gcDelimiters.add(ops);
                                    ops.leftDeleteGroup.setGc(true);
                                    ops.setGc(true);
                                }
                            }
                        }
                        ops = (GCCRDTItem) ops.right;
                    }

                    if (gcDelimiters.size() > 0) {
                        ArrayList<DeleteGroupSerializable> deleteGroupSerialize = new ArrayList<>();
                        for (int i = 0; i < gcDelimiters.size(); i += 2) {
                            System.out
                                    .print(gcDelimiters.get(i).content + " " + gcDelimiters.get(i + 1).content + ", ");
                            deleteGroupSerialize.add(
                                    new DeleteGroupSerializable(gcDelimiters.get(i).serialize(),
                                            gcDelimiters.get(i + 1).serialize(), null));
                        }
                        System.out.println();
                        // broadcast gc
                        this.collaborate
                                .broadcast(DataPayload.gcPayload(this.collaborate.shareID, deleteGroupSerialize));
                        this.collaborate.getClients();
                    }
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (true);
    }

    public GarbageCollectorManager(Collaborate collaborate) {
        this.lock = new ReentrantLock(true);
        this.collaborate = collaborate;
        this.start();
    }

    public void setCrdt(GCCRDT crdt) {
        this.crdt = crdt;
    }

    private ArrayList<DeleteGroupSerializable> findConflictingGC(CRDTItem originLeft, CRDTItem originRight) {
        ArrayList<GCCRDTItem> conflictGC = new ArrayList<>();

        GCCRDTItem o;

        // start from left origin
        if (originLeft != null) {
            o = (GCCRDTItem) originLeft;
        } else {
            o = (GCCRDTItem) this.crdt.getStart();
        }

        if (o == null) {
            return null;
        }

        while (o != null && o != originRight) {
            // collect conflicting or origin gc
            if (o.getGc()) {
                conflictGC.add(o);
            }
            o = (GCCRDTItem) o.right;
        }

        if (conflictGC.size() > 0) {
            GCCRDTItem l = conflictGC.get(0);
            // only scan if its not left delimiter
            if (!l.isDeleteGroupDelimiter() || l.leftDeleteGroup != l)
                while (l.left != null && l.left.isDeleted() && ((GCCRDTItem) l.left).getGc()) {
                    conflictGC.add(0, (GCCRDTItem) l.left);
                    if (((GCCRDTItem) l.left).isDeleteGroupDelimiter()) {
                        break;
                    }
                    l = (GCCRDTItem) l.left;
                }

            GCCRDTItem r = conflictGC.get(conflictGC.size() - 1);
            // only scan if its not right delimiter
            if (!r.isDeleteGroupDelimiter() || r.rightDeleteGroup != r)
                while (r.right != null && r.right.isDeleted() && ((GCCRDTItem) r.right).getGc()) {
                    conflictGC.add((GCCRDTItem) r.right);
                    if (((GCCRDTItem) r.right).isDeleteGroupDelimiter()) {
                        break;
                    }
                    r = (GCCRDTItem) r.right;
                }
        }

        // group
        ArrayList<DeleteGroupSerializable> deleteGroupSerialize = new ArrayList<>();

        boolean isInsideDeleteGroup = false;
        List<CRDTItemSerializable> gcItems = null;
        for (GCCRDTItem i : conflictGC) {
            i.setGc(false);
            if (i.isGarbageCollectable()) { // gc items
                gcItems.add(i.serialize());
            } else if (i.isDeleteGroupDelimiter()) {
                isInsideDeleteGroup = !isInsideDeleteGroup;
                if (isInsideDeleteGroup) { // left delimiter
                    gcItems = new ArrayList<>();
                    gcItems.add(i.serialize());
                } else { // right delimiter
                    gcItems.add(i.serialize());
                    deleteGroupSerialize.add(new DeleteGroupSerializable(null, null, gcItems));
                }
            }
        }

        return deleteGroupSerialize;
    }

    @Override
    public void onRemoteCRDTInsert(Transaction transaction) {
        lock.lock();
        ArrayList<DeleteGroupSerializable> items = this.findConflictingGC(transaction.transactItem.getOriginLeft(),
                transaction.transactItem.getOriginRight());

        Pair<Integer, CRDTItem> result = transaction.execute();
        lock.unlock();

        if (items != null && items.size() > 0) {
            // if conflict gc item found do recover
            this.collaborate.broadcast(
                    DataPayload.recoverPayload(this.collaborate.shareID, result.getSecond().serialize(),
                            items));
        } else {
            this.collaborate
                    .broadcast(DataPayload.insertPayload(this.collaborate.shareID, result.getSecond().serialize()));
        }
    }

    @Override
    public void onRemoteCRDTDelete(Transaction transaction) {
        lock.lock();
        Pair<Integer, CRDTItem> result = transaction.execute();
        lock.unlock();
        this.collaborate.broadcast(DataPayload.deletePayload(this.collaborate.shareID,
                result.getSecond().serialize()));
    }

}
