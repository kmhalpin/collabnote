package com.collabnote.client.ui.document;

import com.collabnote.client.data.entity.DocumentEntity;
import com.collabnote.crdt.CRDTRemoteTransaction;

public interface CRDTDocumentBind extends CRDTRemoteTransaction {
    public DocumentEntity getEntity();
    public void bindCrdt(DocumentEntity entity);
}
