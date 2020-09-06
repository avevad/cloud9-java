package com.avevad.cloud9.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public final class TCPConnection implements CloudConnection {
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    public TCPConnection(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }


    @Override
    public int recv(byte[] buffer, int offset, int size) throws IOException {
        return in.read(buffer, offset, size);
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
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
        }
    }
}
