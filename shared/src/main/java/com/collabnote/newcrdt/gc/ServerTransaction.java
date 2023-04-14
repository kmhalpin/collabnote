package com.collabnote.newcrdt.gc;

import java.util.List;

import org.apache.commons.math3.util.Pair;

import com.collabnote.newcrdt.CRDTItem;

public interface ServerTransaction {
    public Pair<CRDTItem, List<CRDTItem>> execute();
}
