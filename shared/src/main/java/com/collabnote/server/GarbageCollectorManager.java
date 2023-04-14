package com.collabnote.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.util.Pair;

import com.collabnote.newcrdt.CRDTItem;
import com.collabnote.newcrdt.CRDTItemSerializable;
import com.collabnote.newcrdt.gc.GCCRDTItem;
import com.collabnote.newcrdt.gc.ServerGCCRDTRemoteListener;
import com.collabnote.newcrdt.gc.ServerTransaction;
import com.collabnote.newcrdt.CRDTRemoteListener;
import com.collabnote.newcrdt.Transaction;

public class GarbageCollectorManager implements ServerGCCRDTRemoteListener {
    private ReentrantLock lock;

    public GarbageCollectorManager() {
        this.lock = new ReentrantLock(true);
    }

    void splitGC(CRDTItem item) {
        GCCRDTItem gcItemOriginLeft = (GCCRDTItem) item.originLeft;
        GCCRDTItem gcItemOriginRight = (GCCRDTItem) item.originLeft;

        if (item.originLeft != null && gcItemOriginLeft.isGarbageCollectable()
                || item.originRight != null && gcItemOriginRight.isGarbageCollectable()) {
            // find left delete group
            GCCRDTItem oldLeftDeleteGroup = (GCCRDTItem) item.originLeft.left;
            while (oldLeftDeleteGroup.isGarbageCollectable()) {
                oldLeftDeleteGroup = (GCCRDTItem) oldLeftDeleteGroup.left;
            }

            if (!oldLeftDeleteGroup.isDeleted()) {
                throw new NoSuchElementException("not expected");
            }

            GCCRDTItem oldRightDeleteGroup = oldLeftDeleteGroup.rightDeleteGroup;

            // split group
            oldLeftDeleteGroup.rightDeleteGroup = gcItemOriginLeft;
            gcItemOriginLeft.leftDeleteGroup = oldLeftDeleteGroup;

            oldRightDeleteGroup.leftDeleteGroup = gcItemOriginRight;
            gcItemOriginRight.rightDeleteGroup = oldRightDeleteGroup;
        }
    }

    @Override
    public void onRemoteCRDTInsert(Transaction transaction) {
    }

    @Override
    public void onRemoteCRDTDelete(Transaction transaction) {
        lock.lock();
        Pair<Integer, CRDTItem> r = transaction.execute();
        lock.unlock();
    }

    @Override
    public void onRemoteCRDTInsert(ServerTransaction transaction) {
        lock.lock();
        Pair<CRDTItem, List<CRDTItem>> result = transaction.execute();

        splitGC(result.getFirst());

        if (result.getSecond().size() > 0) {
            List<CRDTItem> items = result.getSecond();

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

}
