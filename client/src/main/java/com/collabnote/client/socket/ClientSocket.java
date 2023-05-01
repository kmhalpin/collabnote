package com.collabnote.client.socket;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.collabnote.socket.Const;
import com.collabnote.socket.DataPayload;

public class ClientSocket implements Closeable {
    private Thread networkThread;
    private Socket clientSocket = null;
    private boolean closed = false;
    private ObjectOutputStream writer = null;

    private int agent;

    public ClientSocket(String host, int agent, ClientSocketListener clientSocketListener) {
        this.agent = agent;

        this.networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (Socket socket = new Socket(host, Const.PORT)) {
                    clientSocket = socket;
                    writer = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());

                    clientSocketListener.onStart();
                    do {
                        try {
                            DataPayload data = (DataPayload) reader.readObject();
                            if (data != null)
                                clientSocketListener.onReceiveData(data);
                        } catch (ClassNotFoundException e) {
                        }
                    } while (!closed);
                } catch (IOException e) {
                }
                writer = null;
                closed = true;
                clientSocket = null;
                networkThread = null;
                clientSocketListener.onFinished();
            }
        });
        networkThread.start();
    }

    @Override
    public void close() throws IOException {
        if (!this.closed)
            this.closed = true;
        if (clientSocket != null)
            clientSocket.close();
    }

    public void sendData(DataPayload data) {
        data.setAgent(this.agent);

        if (writer == null)
            return;
        try {
            writer.writeObject(data);
        } catch (IOException e) {
        }
    }
}
