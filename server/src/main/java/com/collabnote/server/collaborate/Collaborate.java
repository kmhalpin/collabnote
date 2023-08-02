package com.collabnote.server.collaborate;

import java.util.ArrayList;
import java.util.List;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItemSerializable;
import com.collabnote.crdt.CRDTRemoteTransaction;
import com.collabnote.crdt.Transaction;
import com.collabnote.crdt.gc.GCCRDT;
import com.collabnote.server.gc.GarbageCollectorManager;
import com.collabnote.server.socket.ClientHandler;
import com.collabnote.socket.DataPayload;

public class Collaborate {
    public static boolean withGC = true;
    public String shareID;
    private boolean isReady;
    private List<ClientHandler> clients;
    private CRDT crdt;
    private GarbageCollectorManager gcManager;

    public Collaborate(String shareID) {
        this.shareID = shareID;
        this.isReady = false;
        this.clients = new ArrayList<>();
        if (withGC) {
            this.gcManager = new GarbageCollectorManager(this);
            GCCRDT gccrdt = new GCCRDT(-1, gcManager, null);
            this.crdt = gccrdt;
            this.gcManager.setCrdt(gccrdt);
        } else {
            this.crdt = new CRDT(-1, new CRDTRemoteTransaction() {

                @Override
                public void onRemoteCRDTInsert(Transaction transaction) {
                    transaction.execute();
                }

                @Override
                public void onRemoteCRDTDelete(Transaction transaction) {
                    transaction.execute();
                }

            }, null);
        }
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
        this.crdt.tryRemoteDelete(item);
    }

    public void insert(CRDTItemSerializable item) {
        this.crdt.tryRemoteInsert(item);
    }

    public List<CRDTItemSerializable> getVersionVector() {
        return this.crdt.getVersionVector().serialize();
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
