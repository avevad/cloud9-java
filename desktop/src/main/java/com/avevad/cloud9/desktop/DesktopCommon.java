package com.avevad.cloud9.desktop;

import java.io.*;
import java.util.ResourceBundle;

public final class DesktopCommon {
    private DesktopCommon() {
    }

    private static final String HOME_DIR = "Cloud9";

    public static File getHomeDir() {
        return new File(System.getProperty("user.home"), HOME_DIR);
    }

    public static final String CONFIG_FILE = "config.bin";

    public static final String STRINGS_BUNDLE = "strings";
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
}
