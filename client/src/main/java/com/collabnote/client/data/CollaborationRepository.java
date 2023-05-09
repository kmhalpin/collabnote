package com.collabnote.client.data;

import java.io.IOException;
import java.util.ArrayList;

import com.collabnote.client.socket.ClientSocket;
import com.collabnote.client.socket.ClientSocketListener;
import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTItemSerializable;
import com.collabnote.socket.DataPayload;
import com.collabnote.socket.Type;

public class CollaborationRepository {
    private ClientSocket socket;

    public boolean isConnected() {
        return this.socket != null;
    }

    public void closeConnection() {
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.socket = null;
    }

    public void sendCaret(String shareID, int index) {
        if (!isConnected())
            return;

        this.socket.sendData(DataPayload.caretPayload(shareID, index));
    }

    public void sendInsert(String shareID, CRDTItemSerializable crdtItem) {
        if (!isConnected())
            return;

        this.socket.sendData(DataPayload.insertPayload(shareID, crdtItem));
    }

    public void sendDelete(String shareID, CRDTItemSerializable crdtItem) {
        if (!isConnected())
            return;

        this.socket.sendData(DataPayload.deletePayload(shareID, crdtItem));
    }

    // reconnect if replica state has been populated
    public void connectCRDT(ArrayList<CRDTItemSerializable> operationBuffer, String host, String shareID, int agent,
            ClientSocketListener mainListener) {
        if (isConnected())
            closeConnection();

        this.socket = new ClientSocket(host, agent, new ClientSocketListener() {
            boolean isReady = false;

            @Override
            public void onStart() {
                socket.sendData(new DataPayload(Type.CONNECT, shareID, null, 0, null));
                mainListener.onStart();
            }

            @Override
            public void onReceiveData(DataPayload data) {
                if (isReady || data.getType() == Type.INSERT || data.getType() == Type.DELETE) {
                    mainListener.onReceiveData(data);
                    return;
                }

                if (data.getType() == Type.SHARE) {
                    // if no operation to sent, send ready connect
                    if (operationBuffer.size() == 0) {
                        socket.sendData(new DataPayload(Type.CONNECT, data.getShareID(), null, 0, null));
                        return;
                    }
                    // upload crdt
                    for (CRDTItemSerializable crdtItem : operationBuffer) {
                        socket.sendData(DataPayload.insertPayload(data.getShareID(), crdtItem));
                    }
                }

                if (data.getType() == Type.DONE) {
                    mainListener.onReceiveData(data);
                    // if all operation sent, send ready connect
                    if (operationBuffer.size() == 0) {
                        socket.sendData(new DataPayload(Type.CONNECT, data.getShareID(), null, 0, null));
                    }
                }

                if (data.getType() == Type.CONNECT) {
                    isReady = true;
                    mainListener.onReceiveData(data);
                }
            }

            @Override
            public void onFinished() {
                socket = null;
                mainListener.onFinished();
            }

        });
    }

    public void shareCRDT(CRDT crdt, String host, int agent, ClientSocketListener mainListener) {
        if (crdt == null)
            return;

        if (isConnected())
            closeConnection();

        this.socket = new ClientSocket(host, agent, new ClientSocketListener() {
            boolean isReady = false;

            @Override
            public void onStart() {
                // create share space
                socket.sendData(new DataPayload(Type.SHARE, null, null, 0, null));
                mainListener.onStart();
            }

            @Override
            public void onReceiveData(DataPayload data) {
                if (isReady) {
                    mainListener.onReceiveData(data);
                    return;
                }

                if (data.getType() == Type.SHARE) {
                    mainListener.onReceiveData(data);
                    System.out.println(data.getShareID());
                    isReady = true;

                    // upload crdt
                    for (CRDTItem crdtItem : crdt.getItems()) {
                        socket.sendData(DataPayload.insertPayload(data.getShareID(), crdtItem.serialize()));
                    }
                    socket.sendData(new DataPayload(Type.SHARE, data.getShareID(), null, 0, null));
                }
            }

            @Override
            public void onFinished() {
                socket = null;
                mainListener.onFinished();
            }

        });
    }
}
