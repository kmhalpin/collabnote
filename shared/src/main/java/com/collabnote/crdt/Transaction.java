package com.collabnote.crdt;

import org.apache.commons.math3.util.Pair;

// used for coordination with local lock
public abstract class Transaction {
    public CRDTItem transactItem;

    public Transaction(CRDTItem transactItem) {
        this.transactItem = transactItem;
    }

    public abstract Pair<Integer, CRDTItem> execute();
}
