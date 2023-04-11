package com.collabnote.newcrdt;

import org.apache.commons.math3.util.Pair;

public interface Transaction {
    public Pair<Integer, CRDTItem> execute();
}
