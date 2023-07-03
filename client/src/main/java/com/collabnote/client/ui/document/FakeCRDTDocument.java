package com.collabnote.client.ui.document;

import com.collabnote.client.data.entity.DocumentEntity;
import com.collabnote.crdt.Transaction;

public class FakeCRDTDocument implements CRDTDocumentBind {
    private DocumentEntity entity;

    @Override
    public void onRemoteCRDTInsert(Transaction transaction) {
        transaction.execute();
    }

    @Override
    public void onRemoteCRDTDelete(Transaction transaction) {
        transaction.execute();
    }

    @Override
    public DocumentEntity getEntity() {
        return entity;
    }

    @Override
    public void bindCrdt(DocumentEntity entity) {
        this.entity = entity;
    }

}
