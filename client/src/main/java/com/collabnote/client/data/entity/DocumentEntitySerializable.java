package com.collabnote.client.data.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItemSerializable;

public class DocumentEntitySerializable implements Serializable {
    private List<CRDTItemSerializable> serializedItems;

    private String shareID;
    private String serverHost;
    private ArrayList<CRDTItemSerializable> operationBuffer;

    public DocumentEntitySerializable(List<CRDTItemSerializable> serializedItems, String shareID,
            String serverHost, ArrayList<CRDTItemSerializable> operationBuffer) {
        this.serializedItems = serializedItems;
        this.shareID = shareID;
        this.serverHost = serverHost;
        this.operationBuffer = operationBuffer;
    }

    public void deserialize(DocumentEntity entity) {
        CRDT crdt = entity.getCrdtReplica();
        for (CRDTItemSerializable i : serializedItems) {
            crdt.tryRemoteInsert(i);
        }

        entity.setCollaboration(shareID, serverHost, operationBuffer);
    }
}
