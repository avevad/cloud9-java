package com.avevad.cloud9.core;

import java.io.IOException;
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

    public CloudClient(AbstractConnection connection, String login, PasswordCallback passwordCallback) throws IOException, InitException {
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

    public Node getHome(String user) throws IOException, RequestException {
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, REQUEST_CMD_GET_HOME);
            sendInt64(connection, stringSize(user));
            sendString(connection, user);
            connection.flush();
            ServerResponse response = waitResponse(lastId);
            if (response.status != REQUEST_OK) {
                throw new RequestException(response.status);
            }
            return Node.bufRecvNode(response.body, 0);
        }
    }

    public Node getHome() throws IOException, RequestException {
        return getHome("");
    }

    public void listDirectory(Node node, DirectoryEntryCallback callback) throws IOException, RequestException {
        ServerResponse response;
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, REQUEST_CMD_LIST_DIRECTORY);
            sendInt64(connection, NODE_ID_SIZE);
            node.sendNode(connection);
            response = waitResponse(lastId);
            if (response.status != REQUEST_OK) {
                throw new RequestException(response.status);
            }
        }
        int pos = 0;
        while (pos < response.size) {
            Node child = Node.bufRecvNode(response.body, pos);
            pos += NODE_ID_SIZE;
            int nameSize = 0xFF & response.body[pos];
            pos++;
            String name = bufRecvString(response.body, pos, nameSize);
            pos += nameSize;
            callback.call(child, name);
        }
    }

    public Node getNodeParent(Node node) throws IOException, RequestException {
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, REQUEST_CMD_GET_PARENT);
            sendInt64(connection, NODE_ID_SIZE);
            node.sendNode(connection);
            ServerResponse response = waitResponse(lastId);
            if (response.status != REQUEST_OK) {
                throw new RequestException(response.status);
            }
            return response.size == 0 ? null : Node.bufRecvNode(response.body, 0);
        }
    }

    public void makeNode(Node parent, String name, NodeType type) throws IOException, RequestException {
        synchronized (apiLock) {
            int nameSize = stringSize(name);
            sendInt32(connection, ++lastId);
            sendInt16(connection, REQUEST_CMD_MAKE_NODE);
            sendInt64(connection, NODE_ID_SIZE + 1 + nameSize + 1);
            parent.sendNode(connection);
            sendByte(connection, (byte) nameSize);
            sendString(connection, name);
            sendByte(connection, type.id);
            connection.flush();
            ServerResponse response = waitResponse(lastId);
            if (response.status != REQUEST_OK) {
                throw new RequestException(response.status);
            }
        }
    }

    public String getNodeOwner(Node node) throws IOException, RequestException {
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, REQUEST_CMD_GET_NODE_OWNER);
            sendInt64(connection, NODE_ID_SIZE);
            node.sendNode(connection);
            connection.flush();
            ServerResponse response = waitResponse(lastId);
            if (response.status != REQUEST_OK) {
                throw new RequestException(response.status);
            }
            return bufRecvString(response.body, 0, response.size);
        }
    }

    public byte openFD(Node node, byte mode) throws IOException, RequestException {
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, REQUEST_CMD_FD_OPEN);
            sendInt64(connection, NODE_ID_SIZE + 1);
            node.sendNode(connection);
            sendByte(connection, mode);
            connection.flush();
            ServerResponse response = waitResponse(lastId);
            if (response.status != REQUEST_OK) {
                throw new RequestException(response.status);
            }
            return response.body[0];
        }
    }

    public void closeFD(byte fd) throws IOException, RequestException {
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, REQUEST_CMD_FD_CLOSE);
            sendInt64(connection, 1);
            sendByte(connection, fd);
            connection.flush();
            ServerResponse response = waitResponse(lastId);
            if (response.status != REQUEST_OK) {
                throw new RequestException(response.status);
            }
        }
    }

    public interface PasswordCallback {
        String promptPassword();
    }

    public interface DirectoryEntryCallback {
        void call(Node node, String name) throws IOException, RequestException;
    }

    public static final class ProtocolException extends RuntimeException {
        public ProtocolException(String message) {
            super(message);
        }
    }

    public static final class InitException extends Exception {
        public final short status;

        public InitException(short status) {
            super("init status code " + status);
            this.status = status;
        }
    }

    public static final class RequestException extends Exception {
        public final short status;

        public RequestException(short status) {
            super("request status code " + status);
            this.status = status;
        }
    }
}
