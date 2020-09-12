package com.avevad.cloud9.desktop;

import javax.swing.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static com.avevad.cloud9.core.CloudCommon.*;

public final class DesktopCommon {
    private DesktopCommon() {
    }

    private static final String HOME_DIR = "Cloud9";

    public static File getHomeDir() {
        return new File(System.getProperty("user.home"), HOME_DIR);
    }

    public static final String CONFIG_FILE = "config.bin";

    public static final String STRINGS_BUNDLE = "assets/bundles/strings";
    private static final ResourceBundle stringsBundle = ResourceBundle.getBundle(STRINGS_BUNDLE);
    private static Config config = null;

    public static void loadConfig() {
        File dir = getHomeDir();
        if (!dir.isDirectory()) if (!dir.mkdir()) throw new RuntimeException("failed to create home directory");
        if (DesktopCommon.config != null) return;
        File configFile = new File(getHomeDir(), CONFIG_FILE);
        Config config = new Config();
        try {
            FileInputStream fis = new FileInputStream(configFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            config = (Config) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException ignored) {
        }
        DesktopCommon.config = config;
    }

    public static void saveConfig() {
        File configFile = new File(getHomeDir(), CONFIG_FILE);
        try {
            FileOutputStream fos = new FileOutputStream(configFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(config);
            oos.close();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static Config getConfig() {
        return config;
    }

    public static String string(String key, Object... args) {
        return String.format(stringsBundle.getString(key), args);
    }

    public static final String STRING_EXIT = "exit";
    public static final String STRING_FILE = "file";
    public static final String STRING_QUICK_CONNECTION = "quick_connection";
    public static final String STRING_HOST = "host";
    public static final String STRING_PORT = "port";
    public static final String STRING_LOGIN_AND_PASSWORD = "login_and_password";
    public static final String STRING_CONNECT = "connect";
    public static final String STRING_FATAL_ERROR = "fatal_error";
    public static final String STRING_FATAL_ERROR_ALERT = "fatal_error_alert";
    public static final String STRING_CONNECTING = "connecting";
    public static final String STRING_NET_CONNECTION_FAILED = "net_connection_failed";
    public static final String STRING_CLOUD_CONNECTION_FAILED = "cloud_connection_failed";
    public static final String STRING_ERROR = "error";
    public static final String STRING_UNKNOWN_HOST = "unknown_host";
    public static final String STRING_NEGOTIATION_ERROR = "negotiation_error";
    public static final String STRING_SECURE_CONNECTION = "secure_connection";
    public static final String STRING_AUTH_FAILED = "auth_failed";
    public static final String STRING_FILE_TYPE = "file_type";
    public static final String STRING_FILE_NAME = "file_name";
    public static final String STRING_LOADING = "loading";

    private static final Map<Short, String> INIT_STATUS_STRINGS = new HashMap<>();
    private static final String STRING_INIT_ERROR_UNKNOWN = "init_error_unknown";

    static {
        INIT_STATUS_STRINGS.put(INIT_OK, "init_ok");
        INIT_STATUS_STRINGS.put(INIT_ERR_BODY_TOO_LARGE, "init_error_body_too_large");
        INIT_STATUS_STRINGS.put(INIT_ERR_INVALID_CMD, "init_error_invalid_cmd");
        INIT_STATUS_STRINGS.put(INIT_ERR_AUTH_FAILED, "init_error_auth_failed");
        INIT_STATUS_STRINGS.put(INIT_ERR_MALFORMED_CMD, "init_error_malformed_cmd");
        INIT_STATUS_STRINGS.put(INIT_ERR_INVALID_INVITE_CODE, "init_error_invalid_invite_code");
        INIT_STATUS_STRINGS.put(INIT_ERR_USER_EXISTS, "init_error_user_exists");
        INIT_STATUS_STRINGS.put(INIT_ERR_INVALID_USERNAME, "init_error_invalid_username");
    }

    public static String init_status_string(short status) {
        return INIT_STATUS_STRINGS.getOrDefault(status, STRING_INIT_ERROR_UNKNOWN);
    }

    private static final Map<Short, String> REQUEST_STATUS_STRINGS = new HashMap<>();
    private static final String STRING_REQUEST_ERROR_UNKNOWN = "request_error_unknown";

    static {
        REQUEST_STATUS_STRINGS.put(REQUEST_OK, "request_ok");
        REQUEST_STATUS_STRINGS.put(REQUEST_SWITCH_OK, "request_switch_ok");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_BODY_TOO_LARGE, "request_error_body_too_large");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_INVALID_CMD, "request_error_invalid_cmd");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_MALFORMED_CMD, "request_error_malformed_cmd");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_NOT_FOUND, "request_error_not_found");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_NOT_A_DIRECTORY, "request_error_not_a_directory");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_FORBIDDEN, "request_error_forbidden");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_INVALID_NAME, "request_error_invalid_name");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_INVALID_TYPE, "request_error_invalid_type");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_EXISTS, "request_error_exists");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_BUSY, "request_error_busy");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_NOT_A_FILE, "request_error_not_a_file");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_TOO_MANY_FDS, "request_error_too_many_fds");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_BAD_FD, "request_error_bad_fd");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_END_OF_FILE, "request_error_end_of_file");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_NOT_SUPPORTED, "request_error_not_supported");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_READ_BLOCK_IS_TOO_LARGE, "request_error_read_block_is_too_large");
        REQUEST_STATUS_STRINGS.put(REQUEST_ERR_DIRECTORY_IS_NOT_EMPTY, "request_error_directory_is_not_empty");
    }

    public static String request_status_string(short status) {
        return REQUEST_STATUS_STRINGS.getOrDefault(status, STRING_REQUEST_ERROR_UNKNOWN);
    }


    public static final String ICONS_PATH = "/assets/icons/";

    public static final String ICON_FOLDER = "folder.png";
    public static final String ICON_FILE = "file.png";

    public static ImageIcon icon(String name) {
        return new ImageIcon(DesktopCommon.class.getResource(ICONS_PATH + name));
    }
}
