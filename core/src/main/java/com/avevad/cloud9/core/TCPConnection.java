package com.avevad.cloud9.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public final class TCPConnection implements CloudConnection {
    private final String host;
    private final int port;
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    public TCPConnection(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        socket = new Socket(host, port);
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }


    @Override
    public int recv(byte[] buffer, int offset, int size) throws IOException {
        int read = in.read(buffer, offset, size);
        if (read == -1) throw new IOException("end of stream");
        return read;
    }

    @Override
    public int send(byte[] buffer, int offset, int size) throws IOException {
        out.write(buffer, offset, size);
        return size;
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public boolean isOpen() {
        return !socket.isClosed();
    }

    @Override
    public CloudConnection reconnect() throws IOException {
        return new TCPConnection(host, port);
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
        }
    }
}
