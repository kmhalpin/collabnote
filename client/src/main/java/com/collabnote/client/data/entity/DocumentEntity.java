package com.collabnote.client.data.entity;

import java.util.ArrayList;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItemSerializable;

public class DocumentEntity {
    // document state
    private CRDT crdtReplica;

    // collaborate document state
    private String shareID;
    private String serverHost;
    private ArrayList<CRDTItemSerializable> operationBuffer;

    public DocumentEntity(CRDT crdtReplica, String shareID, String serverHost,
            ArrayList<CRDTItemSerializable> operationBuffer) {
        this.crdtReplica = crdtReplica;
        this.shareID = shareID;
        this.operationBuffer = operationBuffer;
    }

    public DocumentEntitySerializable serialize() {
        return new DocumentEntitySerializable(crdtReplica.getVersionVector().serialize(), shareID, serverHost,
                operationBuffer);
    }

    public DocumentEntity(CRDT crdtReplica) {
        this(crdtReplica, null, null, null);
    }

    public boolean isCollaborating() {
        return this.shareID != null && this.serverHost != null;
    }

    public CRDT getCrdtReplica() {
        return crdtReplica;
    }

    public void setCrdtReplica(CRDT crdtReplica) {
        this.crdtReplica = crdtReplica;
    }

    public void setCollaboration(String shareID, String serverHost,
            ArrayList<CRDTItemSerializable> operationBuffer) {
        this.shareID = shareID;
        this.serverHost = serverHost;
        this.operationBuffer = operationBuffer;
    }

    public String getShareID() {
        return shareID;
    }

    public void setShareID(String shareID) {
        this.shareID = shareID;
    }

    public ArrayList<CRDTItemSerializable> getOperationBuffer() {
        return operationBuffer;
    }

    public void addOperationBuffer(CRDTItemSerializable item) {
        this.operationBuffer.add(item);
    }

    public void ackOperationBuffer(CRDTItemSerializable item) {
        for (int i = 0; i < this.operationBuffer.size(); i++) {
            if (this.operationBuffer.get(i).id
                    .equals(item.id)) {
                this.operationBuffer.remove(i);
                return;
            }
        }
    }

    public String getServerHost() {
        return serverHost;
    }

}
