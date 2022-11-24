package com.collabnote.server.collaborate;

import java.util.ArrayList;
import java.util.List;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTListener;
import com.collabnote.server.socket.ClientHandler;
import com.collabnote.socket.DataPayload;
import com.collabnote.socket.Type;

public class Collaborate implements CRDTListener {
    private String shareID;
    private boolean isReady;
    private List<ClientHandler> clients;
    private CRDT doc;

    public Collaborate(String shareID) {
        this.shareID = shareID;
        this.isReady = false;
        this.clients = new ArrayList<>();
        this.doc = new CRDT(this);
    }

    public void broadcast(DataPayload data) {
        for (ClientHandler client : this.clients) {
            client.sendData(data);
        }
    }

    public void delete(CRDTItem item) {
        this.doc.addDeleteOperationToWaitList(item);
    }

    public void insert(CRDTItem item) {
        this.doc.addInsertOperationToWaitList(item);
    }

    public List<CRDTItem> getCRDTItems() {
        return this.doc.returnCopy();
    }

    @Override
    public void onCRDTInsert(CRDTItem item, int pos) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCRDTDelete(CRDTItem item, int pos) {
        // TODO Auto-generated method stub

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
        DataPayload payload = new DataPayload(Type.CARET, this.shareID, null, -1);
        payload.setAgent(client.getAgent());
        this.broadcast(payload);
        return this.clients.remove(client);
    }
}
