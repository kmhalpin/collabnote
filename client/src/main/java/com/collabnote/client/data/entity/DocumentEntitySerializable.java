package com.collabnote.client.data.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItemSerializable;

public class DocumentEntitySerializable implements Serializable {
    private List<CRDTItemSerializable> serializedItems;

    private String shareID;
    private String serverHost;
    private HashMap<Integer, Object> userCarets;
    private ArrayList<CRDTItemSerializable> operationBuffer;

    public DocumentEntitySerializable(List<CRDTItemSerializable> serializedItems, String shareID,
            String serverHost, HashMap<Integer, Object> userCarets, ArrayList<CRDTItemSerializable> operationBuffer) {
        this.serializedItems = serializedItems;
        this.shareID = shareID;
        this.serverHost = serverHost;
        this.userCarets = userCarets;
        this.operationBuffer = operationBuffer;
    }

    public DocumentEntity deserialize(CRDT crdt) {
        for (CRDTItemSerializable i : serializedItems) {
            crdt.tryRemoteInsert(i);
        }

        return new DocumentEntity(crdt, shareID, serverHost, userCarets, operationBuffer);
    }
}
