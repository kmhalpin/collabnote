package com.collabnote.benchmark;

import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTListener;

public abstract class AbstractCRDT implements BenchmarkAbstractCRDT {

    private CRDTListener listener;

    public AbstractCRDT(CRDTListener listener) {
        this.listener = listener;
    }

    abstract public CRDTItem insertText(int index, String text);

    public void insertText(int index, String text, Object... args) {
        this.listener.onCRDTInsert(this.insertText(index, text));
    }

}
