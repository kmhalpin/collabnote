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
    CRDTItem delimiter, tail;
    boolean changed;

    public TCRDT(int agent, CRDTRemoteTransaction remoteTransaction, CRDTLocalListener localListener) {
        super(agent, remoteTransaction, localListener);

        this.tail = this.delimiter = null;
        this.changed = false;
        this.gcTimer = Executors.newScheduledThreadPool(1);
        this.gcTimer.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    lock.lock();
                    if (!changed) {
                        tail = delimiter;
                        tail.right.left = null;
                        tail.right = null;
                    }
                } finally {
                    if (changed)
                        changed = false;
                    lock.unlock();
                }
            }

        }, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    protected void integrate(CRDTItem item) {
        super.integrate(item);
        if (item.right == null) {
            this.tail = this.delimiter = item;
        } else if (item.left != null
                && item.left.isDeleted()
                && item.right.isDeleted()
                && scanDelimiter(item.left) == this.delimiter) {
            this.delimiter = item.right;
        }
        this.changed = true;
    }

    @Override
    public void setDeleted(CRDTItem item) {
        super.delete(item);
        if (this.delimiter != null
                && this.delimiter.isDeleted()
                && item.right == this.delimiter) {
            this.delimiter = scanDelimiter(this.tail);
        }
        this.changed = true;
    }

    private CRDTItem scanDelimiter(CRDTItem p) {
        while (p.left != null && p.left.isDeleted()) {
            p = p.left;
        }
        return p;
    }

}
