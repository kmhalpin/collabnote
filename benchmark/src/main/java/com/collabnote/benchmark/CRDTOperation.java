package com.collabnote.benchmark;

import com.collabnote.crdt.CRDTItem;

public class CRDTOperation {
    private CRDTItem item;
    private Operation operation;

    public CRDTOperation(CRDTItem item, Operation operation) {
        this.item = item;
        this.operation = operation;
    }

    public static enum Operation {
        INSERT,
        DELETE
    }

    public CRDTItem getItem() {
        return item;
    }

    public Operation getOperation() {
        return operation;
    }
}
