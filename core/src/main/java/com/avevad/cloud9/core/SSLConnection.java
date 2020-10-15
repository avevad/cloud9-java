package com.avevad.cloud9.core;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SSLConnection implements CloudConnection {
    private final String host;
    private final int port;
    private final SSLSocket socket;
    private final InputStream in;
    private final OutputStream out;

    public SSLConnection(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, port);
        socket.startHandshake();
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

    }

    @Override
    public boolean isOpen() {
        return !socket.isClosed();
    }

    @Override
    public CloudConnection reconnect() throws IOException {
        return new SSLConnection(host, port);
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
