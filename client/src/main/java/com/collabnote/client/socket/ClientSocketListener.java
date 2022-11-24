package com.collabnote.client.socket;

import com.collabnote.socket.DataPayload;

public interface ClientSocketListener {
    public void onStart();
    public void onReceiveData(DataPayload data);
    public void onFinished();
}
