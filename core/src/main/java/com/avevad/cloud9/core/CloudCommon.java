package com.avevad.cloud9.core;

import com.avevad.cloud9.core.util.Holder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

import static com.avevad.cloud9.core.CloudConnection.Helper.recvExact;
import static com.avevad.cloud9.core.CloudConnection.Helper.sendExact;

public final class CloudCommon {
    private CloudCommon() {
    }

    public static final String CLOUD_CHARSET = "UTF-8";

    public static final short CLOUD_RELEASE_CODE = 2;
    public static final String CLOUD_RELEASE_NAME = "1.0.0";

    public static final byte[] CLOUD_HEADER = new byte[]{
            (byte) 0x89,
            (byte) 0x0D,
            (byte) 0x0A,
            (byte) 0x1A,
            (byte) 0xC1,
            (byte) 0xD9
    };
    public static final int CLOUD_HEADER_LENGTH = 6;
    public static final int CLOUD_FULL_HEADER_LENGTH = CLOUD_HEADER_LENGTH + Short.BYTES;

    public static final char CLOUD_PATH_SEP = '/', CLOUD_PATH_HOME = '~', CLOUD_PATH_NODE = '#';

    public static final int CLOUD_DEFAULT_PORT = 909;

    public static final int NODE_ID_SIZE = 16;

    public static final short INIT_CMD_AUTH = 1;
    public static final short INIT_CMD_REGISTER = 2;
    public static final short INIT_CMD_TOKEN = 3;

    public static final short INIT_OK = 0;
    public static final short INIT_ERR_BODY_TOO_LARGE = 1;
    public static final short INIT_ERR_INVALID_CMD = 2;
    public static final short INIT_ERR_AUTH_FAILED = 3;
    public static final short INIT_ERR_MALFORMED_CMD = 4;
    public static final short INIT_ERR_INVALID_INVITE_CODE = 5;
    public static final short INIT_ERR_USER_EXISTS = 6;
    public static final short INIT_ERR_INVALID_USERNAME = 7;
    public static final short INIT_ERR_INVALID_TOKEN = 8;

    public static final short REQUEST_CMD_GET_HOME = 1;
    public static final short REQUEST_CMD_LIST_DIRECTORY = 2;
    public static final short REQUEST_CMD_GOODBYE = 3;
    public static final short REQUEST_CMD_GET_PARENT = 4;
    public static final short REQUEST_CMD_MAKE_NODE = 5;
    public static final short REQUEST_CMD_GET_NODE_OWNER = 6;
    public static final short REQUEST_CMD_FD_OPEN = 7;
    public static final short REQUEST_CMD_FD_CLOSE = 8;
    public static final short REQUEST_CMD_FD_READ = 9;
    public static final short REQUEST_CMD_FD_WRITE = 10;
    public static final short REQUEST_CMD_GET_NODE_INFO = 11;
    public static final short REQUEST_CMD_FD_READ_LONG = 12;
    public static final short REQUEST_CMD_FD_WRITE_LONG = 13;
    public static final short REQUEST_CMD_SET_NODE_RIGHTS = 14;
    public static final short REQUEST_CMD_GET_NODE_GROUP = 15;
    public static final short REQUEST_CMD_SET_NODE_GROUP = 16;
    public static final short REQUEST_CMD_GROUP_INVITE = 17;
    public static final short REQUEST_CMD_REMOVE_NODE = 18;
    public static final short REQUEST_CMD_GROUP_KICK = 19;
    public static final short REQUEST_CMD_GROUP_LIST = 20;
    public static final short REQUEST_CMD_COPY_NODE = 21;
    public static final short REQUEST_CMD_MOVE_NODE = 22;
    public static final short REQUEST_CMD_RENAME_NODE = 23;
    public static final short REQUEST_CMD_GET_TOKEN = 24;

    public static final short REQUEST_OK = 0;
    public static final short REQUEST_ERR_BODY_TOO_LARGE = 1;
    public static final short REQUEST_ERR_INVALID_CMD = 2;
    public static final short REQUEST_ERR_MALFORMED_CMD = 3;
    public static final short REQUEST_ERR_NOT_FOUND = 4;
    public static final short REQUEST_ERR_NOT_A_DIRECTORY = 5;
    public static final short REQUEST_ERR_FORBIDDEN = 6;
    public static final short REQUEST_ERR_INVALID_NAME = 7;
    public static final short REQUEST_ERR_INVALID_TYPE = 8;
    public static final short REQUEST_ERR_EXISTS = 9;
    public static final short REQUEST_ERR_BUSY = 10;
    public static final short REQUEST_ERR_NOT_A_FILE = 11;
    public static final short REQUEST_ERR_TOO_MANY_FDS = 12;
    public static final short REQUEST_ERR_BAD_FD = 13;
    public static final short REQUEST_ERR_END_OF_FILE = 14;
    public static final short REQUEST_ERR_NOT_SUPPORTED = 15;
    public static final short REQUEST_ERR_READ_BLOCK_IS_TOO_LARGE = 16;
    public static final short REQUEST_SWITCH_OK = 17;
    public static final short REQUEST_ERR_DIRECTORY_IS_NOT_EMPTY = 18;

    public static final byte FD_MODE_READ = 0b10;
    public static final byte FD_MODE_WRITE = 0b01;

    public static final byte NODE_RIGHTS_GROUP_READ = 0b1000;
    public static final byte NODE_RIGHTS_GROUP_WRITE = 0b0100;
    public static final byte NODE_RIGHTS_ALL_READ = 0b0010;
    public static final byte NODE_RIGHTS_ALL_WRITE = 0b0001;

    public static final byte NODE_TYPE_FILE = 0x0;
    public static final byte NODE_TYPE_DIRECTORY = 0x1;

    public static final class Node {
        private final byte[] id;

        private Node(byte[] id) {
            this.id = id;
        }

        public static Node bufRecvNode(byte[] buffer, int offset) {
            return new Node(Arrays.copyOfRange(buffer, offset, offset + NODE_ID_SIZE));
        }

        public static Node recvNode(CloudConnection connection) throws IOException {
            byte[] buffer = new byte[NODE_ID_SIZE];
            recvExact(connection, buffer, 0, NODE_ID_SIZE);
            return bufRecvNode(buffer, 0);
        }

        public void bufSendNode(byte[] buffer, int offset) {
            System.arraycopy(id, 0, buffer, offset, NODE_ID_SIZE);
        }

        public void sendNode(CloudConnection connection) throws IOException {
            byte[] buffer = new byte[NODE_ID_SIZE];
            bufSendNode(buffer, 0);
            sendExact(connection, buffer, 0, NODE_ID_SIZE);
        }

        public static Node fromString(String id) {
            if (id.length() != NODE_ID_SIZE * 2) throw new IllegalArgumentException("invalid node id size");
            if (!IntStream.range(0, NODE_ID_SIZE).mapToObj(id::charAt).
                    allMatch(c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
                throw new IllegalArgumentException("invalid character in id");
            byte[] bytes = new byte[NODE_ID_SIZE];
            for (int i = 0; i < NODE_ID_SIZE; i++) {
                char c1 = id.charAt(i * 2);
                char c2 = id.charAt(i * 2 + 1);
                int b1 = (c1 >= '0' && c1 <= '9') ? (c1 - '0') : (c1 - 'a' + 0xA);
                int b2 = (c2 >= '0' && c2 <= '9') ? (c2 - '0') : (c2 - 'a' + 0xA);
                bytes[i] = (byte) ((b1 << 4) | b2);
            }
            return new Node(bytes);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(NODE_ID_SIZE * 2);
            for (byte b : id) {
                int b1 = (b & 0xF0) >> 4;
                int b2 = (b & 0x0F);
                builder.append((char) (b1 < 0xA ? ('0' + b1) : ('a' + (b1 - 0xA))));
                builder.append((char) (b2 < 0xA ? ('0' + b2) : ('a' + (b2 - 0xA))));
            }
            return builder.toString();
        }
    }

    enum NodeType {
        FILE(0), DIRECTORY(1);

        public final byte id;

        NodeType(int id) {
            this.id = (byte) id;
        }
    }

    public static Node parsePath(CloudClient client, Node start, String path) throws IOException, CloudClient.RequestException {
        Node cur = start;
        for (String dir : path.split(String.valueOf(CLOUD_PATH_SEP))) {
            if (dir.isEmpty()) continue;
            Holder<Node> next = new Holder<>();
            client.listDirectory(cur, (node, name) -> {
                if (name.equals(dir)) next.value = node;
            });
            if (next.value == null) throw new FileNotFoundException(dir);
            cur = next.value;
        }
        return cur;
    }
}
