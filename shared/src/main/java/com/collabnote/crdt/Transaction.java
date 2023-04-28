package com.collabnote.crdt;

import org.apache.commons.math3.util.Pair;

// used for coordination with local lock
public interface Transaction {
    public Pair<Integer, CRDTItem> execute();
}
