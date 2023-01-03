package com.collabnote.benchmark;

import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTListener;

public abstract class AbstractCRDT implements BenchmarkAbstractCRDT {

    private CRDTListener listener;

    public AbstractCRDT(CRDTListener listener) {
        this.listener = listener;
    }

    abstract public CRDTItem insertText(int index, String text);

    abstract public CRDTItem deleteText(int index, int length);

    abstract public void insertTextRemote(CRDTItem item);

    abstract public void deleteTextRemote(CRDTItem item);

    public void insertText(int index, String text, Object... args) {
        this.listener.onCRDTInsert(this.insertText(index, text));
    }

    public void applyUpdate(CRDTOperation operation) {
        switch (operation.getOperation()) {
            case INSERT:
                insertTextRemote(operation.getItem());
                break;
            case DELETE:
                deleteTextRemote(operation.getItem());
                break;
        }

    }

}
