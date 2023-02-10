package com.collabnote.server.collaborate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTGC;
import com.collabnote.crdt.CRDTGCListener;
import com.collabnote.server.socket.ClientHandler;
import com.collabnote.socket.DataPayload;

public class Collaborate implements CRDTGCListener {
    private String shareID;
    private boolean isReady;
    private List<ClientHandler> clients;
    private CRDTGC docMaster;
    private ReentrantLock docMasterLock;

    public Collaborate(String shareID) {
        this.shareID = shareID;
        this.isReady = false;
        this.clients = new ArrayList<>();
        this.docMasterLock = new ReentrantLock(true);
        this.docMaster = new CRDTGC(this);
    }

    public void broadcast(DataPayload data) {
        for (ClientHandler client : this.clients) {
            client.sendData(data);
        }
    }

    public void delete(CRDTItem item) {
        docMasterLock.lock();
        this.docMaster.addDeleteOperationToWaitList(item);
        docMasterLock.unlock();
    }

    public void insert(CRDTItem item) {
        docMasterLock.lock();
        this.docMaster.addInsertOperationToWaitList(item);
        docMasterLock.unlock();
    }

    public List<CRDTItem> getCRDTItems() {
        return this.docMaster.returnCopy();
    }

    @Override
    public void onCRDTInsert(CRDTItem item) {
        broadcast(DataPayload.ackInsertPayload(shareID, new CRDTItem(item)));
    }

    @Override
    public void onCRDTRemove(CRDTItem[] remove) {
        broadcast(DataPayload.ackDeletePayload(shareID, remove));
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
