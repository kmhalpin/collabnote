package com.collabnote.server.collaborate;

import java.util.ArrayList;
import java.util.List;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTListener;
import com.collabnote.crdt.CRDTMaster;
import com.collabnote.crdt.CRDTMasterListener;
import com.collabnote.server.socket.ClientHandler;
import com.collabnote.socket.DataPayload;

public class Collaborate implements CRDTListener {
    private String shareID;
    private boolean isReady;
    private List<ClientHandler> clients;
    private CRDTMaster docMaster;
    private Object docMasterLock;
    private CRDT doc;

    public Collaborate(String shareID) {
        this.shareID = shareID;
        this.isReady = false;
        this.clients = new ArrayList<>();
        this.doc = new CRDT(this);
        this.docMasterLock = new Object();
        this.docMaster = new CRDTMaster(new CRDTMasterListener() {
            @Override
            public void onCRDTInsert(CRDTItem item) {
                doc.addInsertOperationToWaitList(item);
            }

            @Override
            public void onCRDTRemove(CRDTItem[] remove, CRDTItem[] change) {
                doc.AckDelete(remove, change);
            }
        });
    }

    public void broadcast(DataPayload data) {
        for (ClientHandler client : this.clients) {
            client.sendData(data);
        }
    }

    public void delete(CRDTItem item) {
        this.doc.addDeleteOperationToWaitList(item);
        synchronized (docMasterLock) {
            this.docMaster.addDeleteOperationToWaitList(item);
        }
    }

    public void insert(CRDTItem item) {
        synchronized (docMasterLock) {
            this.docMaster.addInsertOperationToWaitList(item);
        }
    }

    public List<CRDTItem> getCRDTItems() {
        return this.doc.returnCopy();
    }

    @Override
    public void onCRDTInsert(CRDTItem item) {
        broadcast(DataPayload.ackInsertPayload(shareID, item));
    }

    @Override
    public void onCRDTDelete(CRDTItem item) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCRDTRemove(CRDTItem[] remove, CRDTItem[] change) {
        broadcast(DataPayload.ackDeletePayload(shareID, remove, change));
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public boolean addClient(ClientHandler client) {
        return this.clients.add(client);
    }

    public boolean removeClient(ClientHandler client) {
        DataPayload payload = DataPayload.caretPayload(this.shareID, -1);
        payload.setAgent(client.getAgent());
        this.broadcast(payload);
        return this.clients.remove(client);
    }
}
