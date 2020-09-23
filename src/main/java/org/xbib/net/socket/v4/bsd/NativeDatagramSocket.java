package org.xbib.net.socket.v4.bsd;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.xbib.net.socket.v4.datagram.DatagramPacket;
import org.xbib.net.socket.v4.datagram.DatagramSocket;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static org.xbib.net.socket.v4.bsd.SocketStructure.AF_INET;

public class NativeDatagramSocket implements DatagramSocket, AutoCloseable {

    static {
        Native.register((String) null);
    }

    private static final int IP_TOS = 3;

    private int socket;

    public NativeDatagramSocket(final int type, final int protocol, final int listenPort) {
        socket = socket(AF_INET, type, protocol);
        final SocketStructure in_addr = new SocketStructure(listenPort);
        bind(socket, in_addr, in_addr.size());
    }

    public native int bind(int socket, SocketStructure address, int address_len) throws LastErrorException;

    public native int socket(int domain, int type, int protocol) throws LastErrorException;

    public native int setsockopt(int socket, int level, int option_name, Pointer value, int option_len);

    public native int sendto(int socket, Buffer buffer, int buflen, int flags, SocketStructure dest_addr, int dest_addr_len) throws LastErrorException;

    public native int recvfrom(int socket, Buffer buffer, int buflen, int flags, SocketStructure in_addr, int[] in_addr_len) throws LastErrorException;

    public native int close(int socket) throws LastErrorException;

    public native String strerror(int errnum);

    @Override
    public void setTrafficClass(final int tc) throws IOException {
        final IntByReference tc_ptr = new IntByReference(tc);
        try {
            setsockopt(getSocket(), IPPROTO_IP, IP_TOS, tc_ptr.getPointer(), Native.POINTER_SIZE);
        } catch (final LastErrorException e) {
            throw new IOException("setsockopt: " + strerror(e.getErrorCode()));
        }
    }

    @Override
    public void allowFragmentation(final boolean frag) throws IOException {
        allowFragmentation(IPPROTO_IP, IP_MTU_DISCOVER, frag);
    }


    private void allowFragmentation(final int level, final int option_name, final boolean frag) throws IOException {
        final int socket = getSocket();
        if (socket < 0) {
            throw new IOException("Invalid socket!");
        }
        final IntByReference dontfragment = new IntByReference(frag ? 0 : 1);
        try {
            setsockopt(socket, level, option_name, dontfragment.getPointer(), Native.POINTER_SIZE);
        } catch (final LastErrorException e) {
            throw new IOException("setsockopt: " + strerror(e.getErrorCode()));
        }
    }

    @Override
    public int receive(DatagramPacket p) {
        final SocketStructure in_addr = new SocketStructure();
        final int[] szRef = new int[]{in_addr.size()};
        final ByteBuffer buf = p.getContent();
        final int socket = getSocket();
        //SocketUtils.assertSocketValid(socket);
        final int n = recvfrom(socket, buf, buf.capacity(), 0, in_addr, szRef);
        p.setLength(n);
        p.setAddressable(in_addr);
        //p.setAddress(in_addr.getAddress());
        //p.setPort(in_addr.getPort());
        return n;
    }

    @Override
    public int send(DatagramPacket p) {
        final SocketStructure destAddr = new SocketStructure(p.getAddress(), p.getPort());
        final ByteBuffer buf = p.getContent();
        final int socket = getSocket();
        //SocketUtils.assertSocketValid(socket);
        return sendto(socket, buf, buf.remaining(), 0, destAddr, destAddr.size());
    }

    @Override
    public void close() {
        close(socket);
        socket = -1;
    }

    @Override
    public int getSocket() {
        return socket;
    }
}
