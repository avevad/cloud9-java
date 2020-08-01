package com.avevad.cloud9.core;

public final class CloudCommon {
    private CloudCommon() {
    }

    public static final String CLOUD_CHARSET = "UTF-8";

    public static final short CLOUD_RELEASE_CODE = 1;
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

    public static final int NODE_ID_SIZE = 16;

    public static final short INIT_CMD_AUTH = 1;
    public static final short INIT_CMD_REGISTER = 2;

    public static final short INIT_OK = 0;
    public static final short INIT_ERR_BODY_TOO_LARGE = 1;
    public static final short INIT_ERR_INVALID_CMD = 2;
    public static final short INIT_ERR_AUTH_FAILED = 3;
    public static final short INIT_ERR_MALFORMED_CMD = 4;
    public static final short INIT_ERR_INVALID_INVITE_CODE = 5;
    public static final short INIT_ERR_USER_EXISTS = 6;
    public static final short INIT_ERR_INVALID_USERNAME = 7;

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
}
