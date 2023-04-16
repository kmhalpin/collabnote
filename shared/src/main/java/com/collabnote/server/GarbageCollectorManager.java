package com.collabnote.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.util.Pair;

import com.collabnote.newcrdt.CRDTItem;
import com.collabnote.newcrdt.CRDTRemoteListener;
import com.collabnote.newcrdt.gc.GCCRDT;
import com.collabnote.newcrdt.gc.GCCRDTItem;
import com.collabnote.newcrdt.Transaction;

public class GarbageCollectorManager extends Thread implements CRDTRemoteListener {
    private ReentrantLock lock;
    private GCCRDT crdt;

    @Override
    public void run() {
        do {
            try {
                Thread.sleep(15000);

                List<GCCRDTItem> broadcastItems = new ArrayList<>();
                Set<GCCRDTItem> gcable = new HashSet<>();
                lock.lock();

                GCCRDTItem ops = (GCCRDTItem) this.crdt.getStart();
                while (ops != null) {
                    if (ops.isGarbageCollectable()) {
                        gcable.add(ops);
                        ops.gc = true;
                        if (gcable.remove(ops.originLeft)) {
                            ((GCCRDTItem) ops.originLeft).gc = false;
                        }
                        if (gcable.remove(ops.originRight)) {
                            ((GCCRDTItem) ops.originRight).gc = false;
                        }
                    } else if (ops.isDeleted()) {
                        ops.gc = true;
                        broadcastItems.add(ops);
                    }
                    ops = (GCCRDTItem) ops.right;
                }
                broadcastItems.addAll(gcable);
                if (broadcastItems.size() > 0) {
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        } while (true);
    }

    public GarbageCollectorManager(GCCRDT crdt) {
        this.crdt = crdt;
        this.lock = new ReentrantLock(true);
    }

    private List<CRDTItem> findConflictingGC(CRDTItem item) {
        List<CRDTItem> conflictGC = new ArrayList<>();

        CRDTItem o;

        // start from left origin
        if (item.originLeft != null) {
            o = item.originLeft;
        } else {
            o = this.crdt.getStart();
        }

        while (o != null && ((item.originRight != null && o != item.originRight.right) || o != null)) {
            // collect conflicting or origin gc
            if (o.isDeleted()) {
                conflictGC.add(o);
            }
            o = o.right;
        }

        if (conflictGC.size() > 0) {
            CRDTItem l = conflictGC.get(0);
            while (l.left != null && l.left.isDeleted()) {
                conflictGC.add(0, l.left);
                if (!((GCCRDTItem) l.left).isGarbageCollectable()) {
                    break;
                }
                l = l.left;
            }

            CRDTItem r = conflictGC.get(conflictGC.size() - 1);
            while (r.right != null && r.right.isDeleted()) {
                conflictGC.add(r.right);
                if (!((GCCRDTItem) r.right).isGarbageCollectable()) {
                    break;
                }
                r = r.right;
            }
        }

        return conflictGC;
    }

    @Override
    public void onRemoteCRDTInsert(Transaction transaction) {
        lock.lock();
        Pair<Integer, CRDTItem> result = transaction.execute();
        lock.unlock();

        List<CRDTItem> items = this.findConflictingGC(result.getSecond());
    }

    @Override
    public void onRemoteCRDTDelete(Transaction transaction) {
        lock.lock();
        Pair<Integer, CRDTItem> result = transaction.execute();
        lock.unlock();
    }

}
