package com.avevad.cloud9.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static com.avevad.cloud9.core.CloudCommon.*;
import static com.avevad.cloud9.core.AbstractConnection.Helper.*;

public final class CloudClient {
    private static final class ServerResponse {
        public final short status;
        public final int size;
        public final byte[] body;

        public ServerResponse(short status, byte[] body) {
            this.status = status;
            this.body = body;
            this.size = body.length;
        }
    }

    private final AbstractConnection connection;
    private final Thread listener;
    private final Map<Integer, ServerResponse> responses = new HashMap<>();
    private boolean connected = true;
    private final Object notifier = new Object();
    private final Object apiLock = new Object();
    private int lastId = 0;

    public CloudClient(AbstractConnection connection, String login, PasswordCallback passwordCallback) throws IOException {
        listener = new Thread(this::listenerRoutine);
        this.connection = connection;
        negotiate();
        sendInt16(connection, INIT_CMD_AUTH);
        String password = passwordCallback.promptPassword();
        int loginSize = stringSize(login);
        int passwordSize = stringSize(password);
        int size = 1 + loginSize + passwordSize;
        sendInt64(connection, size);
        sendByte(connection, (byte) loginSize);
        sendString(connection, login);
        sendString(connection, password);
        connection.flush();
        short status = recvInt16(connection);
        if (status != INIT_OK) {
            throw new InitException(status);
        }
        listener.start();
    }

    private void listenerRoutine() {
        try {
            while (true) {
                int id = recvInt32(connection);
                short status = recvInt16(connection);
                int size = (int) recvInt64(connection);
                byte[] body = new byte[size];
                recvExact(connection, body, 0, size);
                synchronized (notifier) {
                    responses.put(id, new ServerResponse(status, body));
                    notifier.notifyAll();
                }
            }
        } catch (IOException e) {
            connected = false;
            synchronized (notifier) {
                notifier.notifyAll();
            }
        }
    }

    private void negotiate() throws IOException {
        byte[] clientHeader = new byte[CLOUD_FULL_HEADER_LENGTH];
        System.arraycopy(CLOUD_HEADER, 0, clientHeader, 0, CLOUD_HEADER_LENGTH);
        bufSendInt16(clientHeader, CLOUD_HEADER_LENGTH, CLOUD_RELEASE_CODE);
        sendExact(connection, clientHeader, 0, CLOUD_FULL_HEADER_LENGTH);
        connection.flush();
        byte[] serverHeader = new byte[CLOUD_FULL_HEADER_LENGTH];
        recvExact(connection, serverHeader, 0, CLOUD_FULL_HEADER_LENGTH);
        if (!IntStream.range(0, CLOUD_HEADER_LENGTH).allMatch(i -> clientHeader[i] == serverHeader[i]))
            throw new ProtocolException("header mismatch");
        if (!IntStream.range(0, CLOUD_FULL_HEADER_LENGTH).allMatch(i -> clientHeader[i] == serverHeader[i]))
            throw new ProtocolException("version mismatch");
    }

    private ServerResponse waitResponse(int id) throws IOException {
        synchronized (notifier) {
            while (connected && !responses.containsKey(id)) {
                try {
                    notifier.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        if (!connected) throw new IOException("not connected");
        return responses.remove(id);
    }

    public void disconnect() {
        try {
            synchronized (apiLock) {
                sendInt32(connection, ++lastId);
                sendInt16(connection, REQUEST_CMD_GOODBYE);
                sendInt64(connection, 0);
                connection.flush();
                waitResponse(lastId);
            }
            connection.close();
        } catch (IOException e) {
        }
    }



    public static final class Node {
        private final byte[] id;

        private Node(byte[] id) {
            this.id = id;
        }

        public static Node bufRecvNode(byte[] buffer, int offset) {
            return new Node(Arrays.copyOfRange(buffer, offset, offset + NODE_ID_SIZE));
        }

        public static Node recvNode(AbstractConnection connection) throws IOException {
            byte[] buffer = new byte[NODE_ID_SIZE];
            recvExact(connection, buffer, 0, NODE_ID_SIZE);
            return bufRecvNode(buffer, 0);
        }

        public void bufSendNode(byte[] buffer, int offset) {
            System.arraycopy(id, 0, buffer, offset, NODE_ID_SIZE);
        }

        public void sendNode(AbstractConnection connection) throws IOException {
            byte[] buffer = new byte[NODE_ID_SIZE];
            bufSendNode(buffer, 0);
            sendExact(connection, buffer, 0, NODE_ID_SIZE);;
        }
    }

    public interface PasswordCallback {
        String promptPassword();
    }

    public static final class ProtocolException extends RuntimeException {
        public ProtocolException(String message) {
            super(message);
        }
    }

    public static final class InitException extends RuntimeException {
        public final short status;

        public InitException(short status) {
            super("init status code " + status);
            this.status = status;
        }
    }


}
