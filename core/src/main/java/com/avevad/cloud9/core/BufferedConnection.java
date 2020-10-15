package com.avevad.cloud9.core;

import java.io.IOException;

public final class BufferedConnection implements CloudConnection {
    private final CloudConnection connection;
    private final byte[] buffer;
    private int bufferPos = 0;

    public BufferedConnection(CloudConnection connection, int bufferSize) {
        this.connection = connection;
        buffer = new byte[bufferSize];
    }

    @Override
    public int recv(byte[] data, int offset, int size) throws IOException {
        return connection.recv(data, offset, size);
    }

    @Override
    public int send(byte[] data, int offset, int size) throws IOException {
        if (bufferPos + size > buffer.length) {
            flush();
        }
        if (size > buffer.length) connection.send(data, offset, size);
        else {
            System.arraycopy(data, offset, buffer, bufferPos, size);
            bufferPos += size;
        }
        return size;
    }

    @Override
    public void flush() throws IOException {
        if (bufferPos != 0) connection.send(buffer, bufferPos);
    }

    @Override
    public boolean isOpen() {
        return connection.isOpen();
    }

    public CloudConnection reconnect() throws IOException {
        return new BufferedConnection(connection.reconnect(), buffer.length);
    }

    @Override
    public void close() {
        connection.close();
    }
}
