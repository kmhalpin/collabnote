package com.collabnote.server.gc;

import java.util.ArrayList;
import java.util.List;
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

                List<GCCRDTItem> gcDelimiters = new ArrayList<>();

                lock.lock();
                GCCRDTItem ops = (GCCRDTItem) this.crdt.getStart();

                boolean isInsideDeleteGroup = false;
                int opsCounter = 0;
                while (ops != null) {
                    if (ops.isGarbageCollectable()) {
                        opsCounter++;
                    } else if (ops.isDeleteGroupDelimiter()
                            && ops.leftDeleteGroup != ops.rightDeleteGroup // skip standalone delimiter
                            && !ops.gc && !ops.leftDeleteGroup.gc) {
                        isInsideDeleteGroup = !isInsideDeleteGroup;
                        if (isInsideDeleteGroup) { // right delimiter
                            opsCounter = 0;
                        } else { // left delimiter
                            if (opsCounter > 0) {
                                gcDelimiters.add(ops.leftDeleteGroup);
                                gcDelimiters.add(ops);
                                ops.leftDeleteGroup.gc = ops.gc = true;
                            }
                        }
                    }
                    ops = (GCCRDTItem) ops.right;
                }

                // set selected delete group operations to gc
                for (int i = 0; i < gcDelimiters.size(); i += 2) {
                    GCCRDTItem o = (GCCRDTItem) gcDelimiters.get(i).right;
                    GCCRDTItem rightdelimiter = o.rightDeleteGroup;
                    while (o != rightdelimiter) {
                        o.gc = true;
                    }
                }

                if (gcDelimiters.size() > 0) {
                    // broadcast gc
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        } while (true);
    }

    public GarbageCollectorManager() {
        this.lock = new ReentrantLock(true);
    }

    public void setCrdt(GCCRDT crdt) {
        this.crdt = crdt;
    }

    private List<GCCRDTItem> findConflictingGC(CRDTItem item) {
        List<GCCRDTItem> conflictGC = new ArrayList<>();

        GCCRDTItem o;

        // start from left origin
        if (item.originLeft != null) {
            o = (GCCRDTItem) item.originLeft;
        } else {
            o = (GCCRDTItem) this.crdt.getStart();
        }

        while (o != null && ((item.originRight != null && o != item.originRight.right) || o != null)) {
            // collect conflicting or origin gc
            if (o.isDeleted() && o.gc) {
                conflictGC.add(o);
                o.gc = false;
            }
            o = (GCCRDTItem) o.right;
        }

        if (conflictGC.size() > 0) {
            CRDTItem l = conflictGC.get(0);
            while (l.left != null && l.left.isDeleted() && ((GCCRDTItem) l.left).gc) {
                conflictGC.add(0, (GCCRDTItem) l.left);
                ((GCCRDTItem) l.left).gc = false;
                if (((GCCRDTItem) l.left).isDeleteGroupDelimiter()) {
                    break;
                }
                l = l.left;
            }

            CRDTItem r = conflictGC.get(conflictGC.size() - 1);
            while (r.right != null && r.right.isDeleted() && ((GCCRDTItem) r.right).gc) {
                conflictGC.add((GCCRDTItem) r.right);
                ((GCCRDTItem) r.right).gc = false;
                if (((GCCRDTItem) r.right).isDeleteGroupDelimiter()) {
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

        List<GCCRDTItem> items = this.findConflictingGC(result.getSecond());
        for (GCCRDTItem i : items) {
        }
    }

    @Override
    public void onRemoteCRDTDelete(Transaction transaction) {
        lock.lock();
        Pair<Integer, CRDTItem> result = transaction.execute();
        lock.unlock();
    }

}
