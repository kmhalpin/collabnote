package com.collabnote.client.data.entity;

import java.io.Serializable;
import java.util.ArrayList;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItemSerializable;

public class DocumentEntitySerializable implements Serializable {
    private ArrayList<CRDTItemSerializable> state;

    private String shareID;
    private String serverHost;
    private ArrayList<CRDTItemSerializable> operationBuffer;

    public DocumentEntitySerializable(ArrayList<CRDTItemSerializable> state, String shareID,
            String serverHost, ArrayList<CRDTItemSerializable> operationBuffer) {
        this.state = state;
        this.shareID = shareID;
        this.serverHost = serverHost;
        this.operationBuffer = operationBuffer;
    }

    public void deserialize(DocumentEntity entity) {
        CRDT crdt = entity.getCrdtReplica();
        for (CRDTItemSerializable i : state) {
            crdt.tryRemoteInsert(i);
        }

        entity.setCollaboration(shareID, serverHost, operationBuffer);
    }
}
