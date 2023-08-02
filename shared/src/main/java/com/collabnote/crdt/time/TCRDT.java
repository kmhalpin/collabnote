package com.collabnote.crdt.time;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTLocalListener;
import com.collabnote.crdt.CRDTRemoteTransaction;

public class TCRDT extends CRDT {
    ScheduledExecutorService gcTimer;
    CRDTItem tail;
    boolean changed;

    public TCRDT(int agent, CRDTRemoteTransaction remoteTransaction, CRDTLocalListener localListener) {
        super(agent, remoteTransaction, localListener);

        // oooooo|bbbb\/ddd
        // b = first buffer, d = second buffer, | = most right non gc (separate non gc
        // and first buffer),
        // \/ = tail (separate first and second buffer).
        this.tail = null;
        this.changed = false;
        // thread not shutdown
        this.gcTimer = Executors.newScheduledThreadPool(1);
        this.gcTimer.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                if (tail != null)
                    try {
                        lock.lock();
                        if (!changed) {
                            // if no changes, scan new first buffer, old first buffer moved to second buffer
                            tail = scanDelimiter(tail);
                        }
                    } finally {
                        if (changed)
                            changed = false;

                        lock.unlock();

                        // eventually remove second buffer
                        CRDTItem tailRight = tail.right;
                        if (tailRight != null) {
                            // null tail
                            tail.right.left = null;
                            tail.right = null;
                            // remove from version vector
                            while (tailRight != null) {
                                versionVector.remove(tailRight);
                                tailRight = tailRight.right;
                            }
                        }
                    }
            }

        }, 0, 15, TimeUnit.SECONDS);
    }

    @Override
    protected void integrate(CRDTItem item) {
        super.integrate(item);
        this.changed = true;
        if (item.right == null)
            tail = item;
    }

    @Override
    public void setDeleted(CRDTItem item) {
        super.setDeleted(item);
        this.changed = true;
    }

    private CRDTItem scanDelimiter(CRDTItem p) {
        while (p.left != null && p.left.isDeleted) {
            p = p.left;
        }
        return p;
    }

}
