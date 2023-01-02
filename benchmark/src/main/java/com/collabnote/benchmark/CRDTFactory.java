package com.collabnote.benchmark;

import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTListener;

public abstract class CRDTFactory {
    abstract public AbstractCRDT create(CRDTListener listener);

    public AbstractCRDT create() {
        return this.create(new CRDTListener() {
            @Override
            public void onCRDTInsert(CRDTItem item) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onCRDTDelete(CRDTItem item) {
                // TODO Auto-generated method stub
            }
        });
    }
}
