package com.collabnote.client.socket;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

import com.collabnote.socket.Const;
import com.collabnote.socket.DataPayload;

public class ClientSocket implements Closeable {
    private Thread networkThread;
    private Thread senderThread;
    private LinkedBlockingQueue<DataPayload> sendQueue;
    private Socket clientSocket = null;
    private boolean closed = false;
    private ObjectOutputStream writer = null;

    private int agent;

    public ClientSocket(String host, int agent, ClientSocketListener clientSocketListener) {
        this.sendQueue = new LinkedBlockingQueue<>();
        this.agent = agent;

        this.senderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!closed) {
                    if (writer == null || sendQueue.size() < 1) {
                        continue;
                    }
                    DataPayload data = sendQueue.remove();
                    try {
                        writer.writeObject(data);
                    } catch (SocketException e) {
                        e.printStackTrace();
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        // retry later
                        sendQueue.add(data);
                    }
                }
                senderThread = null;
            }
        });

        this.networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (Socket socket = new Socket(host, Const.PORT)) {
                    clientSocket = socket;
                    writer = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());

                    clientSocketListener.onStart();
                    senderThread.start();
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
        this.networkThread.start();
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
        this.sendQueue.add(data);
    }
}
