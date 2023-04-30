package com.collabnote.client.data.entity;

import java.util.ArrayList;
import java.util.HashMap;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItemSerializable;

public class DocumentEntity {
    // document state
    private CRDT crdtReplica;

    // collaborate document state
    private String shareID;
    private String serverHost;
    private HashMap<Integer, Object> userCarets;
    private ArrayList<CRDTItemSerializable> operationBuffer;

    public DocumentEntity(CRDT crdtReplica, String shareID, String serverHost, HashMap<Integer, Object> userCarets,
            ArrayList<CRDTItemSerializable> operationBuffer) {
        this.crdtReplica = crdtReplica;
        this.shareID = shareID;
        this.serverHost = serverHost;
        this.userCarets = userCarets;
        this.operationBuffer = operationBuffer;
    }

    public DocumentEntitySerializable serialize() {
        return new DocumentEntitySerializable(crdtReplica.serialize(), shareID, serverHost, userCarets,
                operationBuffer);
    }

    public DocumentEntity(CRDT crdtReplica) {
        this(crdtReplica, null, null, null, null);
    }

    public boolean isCollaborating() {
        return this.shareID != null && this.serverHost != null && this.userCarets != null;
    }

    public CRDT getCrdtReplica() {
        return crdtReplica;
    }

    public void setCollaboration(String shareID, String serverHost, HashMap<Integer, Object> userCarets,
            ArrayList<CRDTItemSerializable> operationBuffer) {
        this.shareID = shareID;
        this.serverHost = serverHost;
        this.userCarets = userCarets;
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

    public HashMap<Integer, Object> getUserCarets() {
        return userCarets;
    }

    public String getServerHost() {
        return serverHost;
    }

}
