package com.avevad.cloud9.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public interface CloudConnection {
    int recv(byte[] buffer, int offset, int size) throws IOException;

    int send(byte[] buffer, int offset, int size) throws IOException;

    default int recv(byte[] buffer, int size) throws IOException {
        return recv(buffer, 0, size);
    }

    default int send(byte[] buffer, int size) throws IOException {
        return send(buffer, 0, size);
    }

    void flush() throws IOException;

    boolean isOpen();

    void close();

    final class Helper {
        private Helper() {
        }

        public static void recvExact(CloudConnection connection, byte[] buffer, int offset, int size) throws IOException {
            int pos = 0;
            while (pos < size) pos += connection.recv(buffer, offset + pos, size - pos);
        }

        public static void sendExact(CloudConnection connection, byte[] buffer, int offset, int size) throws IOException {
            int pos = 0;
            while (pos < size) pos += connection.send(buffer, offset + pos, size - pos);
        }

        public static byte recvByte(CloudConnection connection) throws IOException {
            byte[] buffer = new byte[1];
            recvExact(connection, buffer, 0, 1);
            return buffer[0];
        }

        public static void sendByte(CloudConnection connection, byte n) throws IOException {
            byte[] buffer = new byte[]{n};
            sendExact(connection, buffer, 0, 1);
        }

        public static short bufRecvInt16(byte[] buffer, int offset) {
            return ByteBuffer.wrap(buffer, offset, Short.BYTES).getShort();
        }

        public static short recvInt16(CloudConnection connection) throws IOException {
            byte[] buffer = new byte[Short.BYTES];
            recvExact(connection, buffer, 0, Short.BYTES);
            return bufRecvInt16(buffer, 0);
        }

        public static void bufSendInt16(byte[] buffer, int offset, short n) {
            ByteBuffer.wrap(buffer, offset, Short.BYTES).putShort(n);
        }

        public static void sendInt16(CloudConnection connection, short n) throws IOException {
            byte[] buffer = new byte[Short.BYTES];
            bufSendInt16(buffer, 0, n);
            sendExact(connection, buffer, 0, Short.BYTES);
        }

        public static int bufRecvInt32(byte[] buffer, int offset) {
            return ByteBuffer.wrap(buffer, offset, Integer.BYTES).getInt();
        }

        public static int recvInt32(CloudConnection connection) throws IOException {
            byte[] buffer = new byte[Integer.BYTES];
            recvExact(connection, buffer, 0, Integer.BYTES);
            return bufRecvInt32(buffer, 0);
        }

        public static void bufSendInt32(byte[] buffer, int offset, int n) {
            ByteBuffer.wrap(buffer, offset, Integer.BYTES).putInt(n);
        }

        public static void sendInt32(CloudConnection connection, int n) throws IOException {
            byte[] buffer = new byte[Integer.BYTES];
            bufSendInt32(buffer, 0, n);
            sendExact(connection, buffer, 0, Integer.BYTES);
        }

        public static long bufRecvInt64(byte[] buffer, int offset) {
            return ByteBuffer.wrap(buffer, offset, Long.BYTES).getLong();
        }

        public static long recvInt64(CloudConnection connection) throws IOException {
            byte[] buffer = new byte[Long.BYTES];
            recvExact(connection, buffer, 0, Long.BYTES);
            return bufRecvInt64(buffer, 0);
        }

        public static void bufSendInt64(byte[] buffer, int offset, long n) {
            ByteBuffer.wrap(buffer, offset, Long.BYTES).putLong(n);
        }

        public static void sendInt64(CloudConnection connection, long n) throws IOException {
            byte[] buffer = new byte[Long.BYTES];
            bufSendInt64(buffer, 0, n);
            sendExact(connection, buffer, 0, Long.BYTES);
        }

        public static int stringSize(String s) {
            try {
                return s.getBytes(CloudCommon.CLOUD_CHARSET).length;
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError("the platform does not support the cloud encoding");
            }
        }

        public static String bufRecvString(byte[] buffer, int offset, int size) {
            try {
                return new String(buffer, offset, size, CloudCommon.CLOUD_CHARSET);
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError("the platform does not support the cloud encoding");
            }
        }

        public static String recvString(CloudConnection connection, int size) throws IOException {
            byte[] buffer = new byte[size];
            recvExact(connection, buffer, 0, size);
            return bufRecvString(buffer, 0, size);
        }

        public static void bufSendString(byte[] buffer, int offset, String s) {
            try {
                byte[] bytes = s.getBytes(CloudCommon.CLOUD_CHARSET);
                System.arraycopy(s.getBytes(), 0, buffer, offset, bytes.length);
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError("the platform does not support the cloud encoding");
            }
        }

        public static void sendString(CloudConnection connection, String s) throws IOException {
            byte[] buffer = new byte[stringSize(s)];
            bufSendString(buffer, 0, s);
            sendExact(connection, buffer, 0, buffer.length);
        }
    }
}
