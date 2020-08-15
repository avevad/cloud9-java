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

    private ServerResponse singleNodeRequest(short cmd, Node node) throws IOException, RequestException {
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, cmd);
            sendInt64(connection, NODE_ID_SIZE);
            node.sendNode(connection);
            connection.flush();
            return waitResponse(lastId);
        }
    }

    private ServerResponse singleStringRequest(short cmd, String string) throws IOException, RequestException {
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, cmd);
            sendInt64(connection, stringSize(string));
            sendString(connection, string);
            connection.flush();
            return waitResponse(lastId);
        }
    }

    public Node getHome(String user) throws IOException, RequestException {
        ServerResponse response = singleStringRequest(REQUEST_CMD_GET_HOME, user);
        if (response.status != REQUEST_OK) {
            throw new RequestException(response.status);
        }
        return Node.bufRecvNode(response.body, 0);
    }

    public Node getHome() throws IOException, RequestException {
        return getHome("");
    }

    public void listDirectory(Node node, DirectoryEntryCallback callback) throws IOException, RequestException {
        ServerResponse response = singleNodeRequest(REQUEST_CMD_LIST_DIRECTORY, node);
        if (response.status != REQUEST_OK) {
            throw new RequestException(response.status);
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
        ServerResponse response = singleNodeRequest(REQUEST_CMD_GET_PARENT, node);
        if (response.status != REQUEST_OK) {
            throw new RequestException(response.status);
        }
        return response.size == 0 ? null : Node.bufRecvNode(response.body, 0);
    }

    public Node makeNode(Node parent, String name, NodeType type) throws IOException, RequestException {
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
            return Node.bufRecvNode(response.body, 0);
        }
    }

    public String getNodeOwner(Node node) throws IOException, RequestException {
        ServerResponse response = singleNodeRequest(REQUEST_CMD_GET_NODE_OWNER, node);
        if (response.status != REQUEST_OK) {
            throw new RequestException(response.status);
        }
        return bufRecvString(response.body, 0, response.size);
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

    public int readFD(byte fd, byte[] buffer, int offset, int size) throws IOException, RequestException {
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, REQUEST_CMD_FD_READ);
            sendInt64(connection, 1 + Integer.BYTES);
            sendByte(connection, fd);
            sendInt32(connection, size);
            connection.flush();
            ServerResponse response = waitResponse(lastId);
            if (response.status != REQUEST_OK) {
                throw new RequestException(response.status);
            }
            System.arraycopy(response.body, 0, buffer, offset, response.size);
            return response.size;
        }
    }

    public void writeFD(byte fd, byte[] buffer, int offset, int size) throws IOException, RequestException {
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, REQUEST_CMD_FD_WRITE);
            sendInt64(connection, 1 + size);
            sendByte(connection, fd);
            sendExact(connection, buffer, offset, size);
            connection.flush();
            ServerResponse response = waitResponse(lastId);
            if (response.status != REQUEST_OK) {
                throw new RequestException(response.status);
            }
        }
    }

    public NodeInfo getNodeInfo(Node node) throws IOException, RequestException {
        ServerResponse response = singleNodeRequest(REQUEST_CMD_GET_NODE_INFO, node);
        if (response.status != REQUEST_OK) {
            throw new RequestException(response.status);
        }
        int pos = 0;
        byte type = response.body[pos++];
        long size = bufRecvInt64(response.body, pos);
        pos += Long.BYTES;
        byte rights = response.body[pos];
        return new NodeInfo(type, size, rights);
    }

    public void setNodeRights(Node node, byte rights) throws IOException, RequestException {
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, REQUEST_CMD_SET_NODE_RIGHTS);
            sendInt64(connection, NODE_ID_SIZE + 1);
            node.sendNode(connection);
            sendByte(connection, rights);
            connection.flush();
            ServerResponse response = waitResponse(lastId);
            if (response.status != REQUEST_OK) {
                throw new RequestException(response.status);
            }
        }
    }

    public String getNodeGroup(Node node) throws IOException, RequestException {
        ServerResponse response = singleNodeRequest(REQUEST_CMD_GET_NODE_GROUP, node);
        if (response.status != REQUEST_OK) {
            throw new RequestException(response.status);
        }
        return bufRecvString(response.body, 0, response.size);
    }

    public void setNodeGroup(Node node, String group) throws IOException, RequestException {
        synchronized (apiLock) {
            sendInt32(connection, ++lastId);
            sendInt16(connection, REQUEST_CMD_SET_NODE_GROUP);
            sendInt64(connection, NODE_ID_SIZE + stringSize(group));
            node.sendNode(connection);
            sendString(connection, group);
            connection.flush();
            ServerResponse response = waitResponse(lastId);
            if (response.status != REQUEST_OK) {
                throw new RequestException(response.status);
            }
        }
    }

    public void groupInvite(String user) throws IOException, RequestException {
        ServerResponse response = singleStringRequest(REQUEST_CMD_GROUP_INVITE, user);
        if (response.status != REQUEST_OK) {
            throw new RequestException(response.status);
        }
    }

    public void removeNode(Node node) throws IOException, RequestException {
        ServerResponse response = singleNodeRequest(REQUEST_CMD_REMOVE_NODE, node);
        if (response.status != REQUEST_OK) {
            throw new RequestException(response.status);
        }
    }

    public void groupKick(String user) throws IOException, RequestException {
        ServerResponse response = singleStringRequest(REQUEST_CMD_GROUP_KICK, user);
        if (response.status != REQUEST_OK) {
            throw new RequestException(response.status);
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

    public static class NodeInfo {
        public final byte type;
        public final long size;
        public final byte rights;

        public NodeInfo(byte type, long size, byte rights) {
            this.type = type;
            this.size = size;
            this.rights = rights;
        }

        @Override
        public String toString() {
            return "NodeInfo{" +
                    "type=" + type +
                    ", size=" + size +
                    ", rights=" + rights +
                    '}';
        }
    }
}
