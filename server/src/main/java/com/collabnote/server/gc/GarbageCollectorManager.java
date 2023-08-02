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
                Thread.sleep(1500);

                try {
                    lock.lock();
                    // System.out.println("GC START");
                    ArrayList<DeleteGroupSerializable> deleteGroupSerialize = new ArrayList<>();
                    GCCRDTItem ops = (GCCRDTItem) this.crdt.getStart();

                    boolean isInsideDeleteGroup = false,
                            includeDeleteGroup = false;
                    GCCRDTItem leftDelimiter = null;
                    while (ops != null) {
                        // group checking
                        if (isInsideDeleteGroup && !ops.isDeleteGroupDelimiter()) {
                            if (ops.isGarbageCollectable()) {
                                includeDeleteGroup = true;
                                ops.setServerGc(true);
                            } else if (ops.isRightDeleteGroupDelimiter()) {
                                if (includeDeleteGroup) {
                                    leftDelimiter.setServerGc(true);
                                    ops.setServerGc(true);

                                    // debug
                                    // System.out.print(leftDelimiter.content + " " + ops.content + ", ");

                                    deleteGroupSerialize.add(new DeleteGroupSerializable(leftDelimiter.serialize(),
                                            ops.serialize(), null));

                                    isInsideDeleteGroup = includeDeleteGroup = false;
                                    leftDelimiter = null;
                                } else {
                                    isInsideDeleteGroup = false;
                                    leftDelimiter = null;
                                }
                            }
                        } else if (ops.isDeleteGroupDelimiter()) {
                            isInsideDeleteGroup = true;
                            leftDelimiter = ops;
                        }
                        ops = (GCCRDTItem) ops.right;
                    }

                    if (deleteGroupSerialize.size() > 0) {
                        System.out.println(deleteGroupSerialize.size());
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
            if (o.getServerGc()) {
                conflictGC.add(o);
            }
            o = (GCCRDTItem) o.right;
        }

        if (conflictGC.size() > 0) {
            GCCRDTItem l = conflictGC.get(0);
            // only scan if its not left delimiter
            if (!l.isDeleteGroupDelimiter())
                while (l.left != null && ((GCCRDTItem) l.left).getServerGc()) {
                    conflictGC.add(0, (GCCRDTItem) l.left);
                    if (((GCCRDTItem) l.left).isDeleteGroupDelimiter()) {
                        break;
                    }
                    l = (GCCRDTItem) l.left;
                }

            GCCRDTItem r = conflictGC.get(conflictGC.size() - 1);
            // only scan if its not right delimiter
            if (!r.isRightDeleteGroupDelimiter())
                while (r.right != null && ((GCCRDTItem) r.right).getServerGc()) {
                    conflictGC.add((GCCRDTItem) r.right);
                    if (((GCCRDTItem) r.right).isRightDeleteGroupDelimiter()) {
                        break;
                    }
                    r = (GCCRDTItem) r.right;
                }
        }

        // group
        ArrayList<DeleteGroupSerializable> deleteGroupSerialize = new ArrayList<>();

        List<CRDTItemSerializable> gcItems = null;
        boolean isInsideDeleteGroup = false;
        for (GCCRDTItem i : conflictGC) {
            i.setServerGc(false);

            if (isInsideDeleteGroup && !i.isDeleteGroupDelimiter()) {
                if (i.isGarbageCollectable()) { // gc items
                    gcItems.add(i.serialize());
                } else if (i.isRightDeleteGroupDelimiter()) {// right delimiter
                    gcItems.add(i.serialize());
                    deleteGroupSerialize.add(new DeleteGroupSerializable(null, null, gcItems));
                    isInsideDeleteGroup = false;
                }
            } else if (i.isDeleteGroupDelimiter()) { // left delimiter
                gcItems = new ArrayList<>();
                gcItems.add(i.serialize());
                isInsideDeleteGroup = true;
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
            DataPayload data = DataPayload.recoverPayload(this.collaborate.shareID, result.getSecond().serialize(),
                    items);
            data.setAgent(result.getSecond().serialize().id.agent);
            this.collaborate.broadcast(data);
        } else {
            DataPayload data = DataPayload.insertPayload(this.collaborate.shareID, result.getSecond().serialize());
            data.setAgent(result.getSecond().serialize().id.agent);
            this.collaborate.broadcast(data);
        }
    }

    @Override
    public void onRemoteCRDTDelete(Transaction transaction) {
        lock.lock();
        Pair<Integer, CRDTItem> result = transaction.execute();
        lock.unlock();
        DataPayload data = DataPayload.deletePayload(this.collaborate.shareID,
                result.getSecond().serialize());
        // data.setAgent(result.getSecond().serialize().id.agent);
        this.collaborate.broadcast(data);
    }

}
