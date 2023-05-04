package com.collabnote.client.data.entity;

import java.io.Serializable;
import java.util.ArrayList;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTItemSerializable;
import com.collabnote.crdt.VersionVectors;

public class DocumentEntitySerializable implements Serializable {
    private CRDTItem crdtStart;
    private VersionVectors versionVectors;

    private String shareID;
    private String serverHost;
    private ArrayList<CRDTItemSerializable> operationBuffer;

    public DocumentEntitySerializable(CRDTItem crdtStart, VersionVectors versionVectors, String shareID,
            String serverHost, ArrayList<CRDTItemSerializable> operationBuffer) {
        this.crdtStart = crdtStart;
        this.versionVectors = versionVectors;
        this.shareID = shareID;
        this.serverHost = serverHost;
        this.operationBuffer = operationBuffer;
    }

    public void deserialize(DocumentEntity entity) {
        CRDT crdt = entity.getCrdtReplica();
        crdt.loadCRDT(this.crdtStart, this.versionVectors);

        entity.setCollaboration(shareID, serverHost, operationBuffer);
    }
}
