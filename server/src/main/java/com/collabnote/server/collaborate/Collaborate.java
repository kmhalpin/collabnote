package com.collabnote.server.collaborate;

import java.util.ArrayList;
import java.util.List;

import com.collabnote.crdt.CRDTItemSerializable;
import com.collabnote.crdt.gc.GCCRDT;
import com.collabnote.server.gc.GarbageCollectorManager;
import com.collabnote.server.socket.ClientHandler;
import com.collabnote.socket.DataPayload;

public class Collaborate {
    public String shareID;
    private boolean isReady;
    private List<ClientHandler> clients;
    private GCCRDT gccrdt;
    private GarbageCollectorManager gcManager;

    public Collaborate(String shareID) {
        this.shareID = shareID;
        this.isReady = false;
        this.clients = new ArrayList<>();
        this.gcManager = new GarbageCollectorManager(this);
        this.gccrdt = new GCCRDT(-1, gcManager, null);
        this.gcManager.setCrdt(gccrdt);
    }

    public void broadcast(DataPayload data) {
        for (ClientHandler client : this.clients) {
            client.sendData(data);
        }
    }

    public int getClients() {
        return clients.size();
    }

    public void delete(CRDTItemSerializable item) {
        this.gccrdt.tryRemoteDelete(item);
    }

    public void insert(CRDTItemSerializable item) {
        this.gccrdt.tryRemoteInsert(item);
    }

    public List<CRDTItemSerializable> getVersionVector() {
        return this.gccrdt.getVersionVector().serialize();
    }

    // @Override
    // public void onCRDTInsert(CRDTItem item) {
    // broadcast(DataPayload.ackInsertPayload(shareID, new CRDTItem(item)));
    // }

    // @Override
    // public void onCRDTRemove(CRDTItem[] remove) {
    // broadcast(DataPayload.ackDeletePayload(shareID, remove));
    // }

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
