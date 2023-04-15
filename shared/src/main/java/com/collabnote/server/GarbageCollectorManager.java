package com.collabnote.server;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.util.Pair;

import com.collabnote.newcrdt.CRDTItem;
import com.collabnote.newcrdt.CRDTRemoteListener;
import com.collabnote.newcrdt.gc.GCCRDT;
import com.collabnote.newcrdt.gc.GCCRDTItem;
import com.collabnote.newcrdt.Transaction;

public class GarbageCollectorManager implements CRDTRemoteListener {
    private ReentrantLock lock;
    private GCCRDT crdt;

    public GarbageCollectorManager(GCCRDT crdt) {
        this.crdt = crdt;
        this.lock = new ReentrantLock(true);
    }

    @Override
    public void onRemoteCRDTInsert(Transaction transaction) {
        lock.lock();
        Pair<Integer, CRDTItem> result = transaction.execute();

        List<CRDTItem> items = this.crdt.findConflictingGC(result.getSecond());

        if (items.size() > 0) {
            CRDTItem l = items.get(0);
            while (l.left != null && l.left.isDeleted()) {
                items.add(0, l.left);
                if (!((GCCRDTItem) l.left).isGarbageCollectable()) {
                    break;
                }
                l = l.left;
            }

            CRDTItem r = items.get(items.size() - 1);
            while (r.right != null && r.right.isDeleted()) {
                items.add(r.right);
                if (!((GCCRDTItem) r.right).isGarbageCollectable()) {
                    break;
                }
                r = r.right;
            }
        }
        lock.unlock();
    }

    @Override
    public void onRemoteCRDTDelete(Transaction transaction) {
        lock.lock();
        Pair<Integer, CRDTItem> r = transaction.execute();
        lock.unlock();
    }

}
