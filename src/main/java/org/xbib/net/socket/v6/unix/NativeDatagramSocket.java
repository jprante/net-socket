package org.xbib.net.socket.v6.unix;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.xbib.net.socket.NetworkUnreachableException;
import org.xbib.net.socket.v6.datagram.DatagramPacket;
import org.xbib.net.socket.v6.datagram.DatagramSocket;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static org.xbib.net.socket.v6.unix.SocketStructure.AF_INET6;

public class NativeDatagramSocket implements DatagramSocket, AutoCloseable {

    static {
        Native.register((String) null);
    }

    private static final int IPV6_TCLASS = 67;

    private final int socket;

    private volatile boolean closed;

    public NativeDatagramSocket(int type, int protocol, int port) {
        this.socket = socket(AF_INET6, type, protocol);
        if (socket < 0) {
            throw new IllegalStateException("socket < 0");
        }
        SocketStructure socketStructure = new SocketStructure(port);
        bind(socket, socketStructure, socketStructure.size());
        closed = false;
    }

    public native int bind(int socket, SocketStructure address, int address_len) throws LastErrorException;

    public native int socket(int domain, int type, int protocol) throws LastErrorException;

    public native int setsockopt(int socket, int level, int option_name, Pointer value, int option_len);

    public native int sendto(int socket, Buffer buffer, int buflen, int flags, SocketStructure dest_addr, int dest_addr_len) throws LastErrorException;

    public native int recvfrom(int socket, Buffer buffer, int buflen, int flags, SocketStructure in_addr, int[] in_addr_len) throws LastErrorException;

    public native int close(int socket) throws LastErrorException;

    public native String strerror(int errnum);

    @Override
    public int setTrafficClass(int tc) throws LastErrorException {
        if (closed) {
            return -1;
        }
        IntByReference tc_ptr = new IntByReference(tc);
        try {
            return setsockopt(socket, IPPROTO_IPV6, IPV6_TCLASS, tc_ptr.getPointer(), Native.POINTER_SIZE);
        } catch (LastErrorException e) {
            throw new RuntimeException("setsockopt: " + strerror(e.getErrorCode()));
        }
    }

    @Override
    public int allowFragmentation(boolean frag) throws IOException {
        return allowFragmentation(IPPROTO_IPV6, IPV6_DONTFRAG, frag);
    }

    private int allowFragmentation(int level, int option_name, boolean frag) throws IOException {
        if (closed) {
            return -1;
        }
        IntByReference dontfragment = new IntByReference(frag ? 0 : 1);
        try {
            return setsockopt(socket, level, option_name, dontfragment.getPointer(), Native.POINTER_SIZE);
        } catch (LastErrorException e) {
            throw new IOException("setsockopt: " + strerror(e.getErrorCode()));
        }
    }

    @Override
    public int receive(DatagramPacket datagramPacket) {
        if (closed) {
            return -1;
        }
        SocketStructure in_addr = new SocketStructure();
        int[] szRef = new int[]{in_addr.size()};
        ByteBuffer buf = datagramPacket.getContent();
        int n = recvfrom(socket, buf, buf.capacity(), 0, in_addr, szRef);
        datagramPacket.setLength(n);
        datagramPacket.setAddressable(in_addr);
        return n;
    }

    @Override
    public int send(DatagramPacket p) throws NetworkUnreachableException {
        if (closed) {
            return -1;
        }
        try {
            ByteBuffer buf = p.getContent();
            SocketStructure destAddr = new SocketStructure(p.getAddress(), p.getPort());
            return sendto(socket, buf, buf.remaining(), 0, destAddr, destAddr.size());
        } catch (LastErrorException e) {
            if (e.getMessage().contains("[101]")) {
                throw new NetworkUnreachableException();
            }
            throw e;
        }
    }

    @Override
    public void close() {
        closed = true;
        close(socket);
    }
}
