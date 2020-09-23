package org.xbib.net.socket.v4.unix;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.xbib.net.socket.NetworkUnreachableException;
import org.xbib.net.socket.v4.datagram.DatagramPacket;
import org.xbib.net.socket.v4.datagram.DatagramSocket;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.xbib.net.socket.v4.unix.SocketStructure.AF_INET;

public class NativeDatagramSocket implements DatagramSocket, AutoCloseable {

    static {
        Native.register((String) null);
    }

    private static final Logger logger = Logger.getLogger(NativeDatagramSocket.class.getName());

    private static final int IP_TOS = 1;

    private int socket;

    public NativeDatagramSocket(int type, int protocol, int port) {
        try {
            this.socket = socket(AF_INET, type, protocol);
            SocketStructure socketStructure = new SocketStructure(port);
            bind(socket, socketStructure, socketStructure.size());
        } catch (LastErrorException e) {
            if (e.getMessage().contains("[13]")) {
                logger.log(Level.SEVERE, "Check if sysctl -w net.ipv4.ping_group_range=\"0 65535\" and check for security:\n" +
                        "allow unconfined_t node_t:icmp_socket node_bind;\n" +
                        "allow unconfined_t port_t:icmp_socket name_bind;\n");
            }
            throw e;
        }
    }

    public native int bind(int socket, SocketStructure address, int address_len) throws LastErrorException;

    public native int socket(int domain, int type, int protocol) throws LastErrorException;

    public native int setsockopt(int socket, int level, int option_name, Pointer value, int option_len);

    public native int sendto(int socket, Buffer buffer, int buflen, int flags, SocketStructure dest_addr, int dest_addr_len) throws LastErrorException;

    public native int recvfrom(int socket, Buffer buffer, int buflen, int flags, SocketStructure in_addr, int[] in_addr_len) throws LastErrorException;

    public native int close(int socket) throws LastErrorException;

    public native String strerror(int errnum);

    @Override
    public int getSocket() {
        return socket;
    }

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
        SocketStructure in_addr = new SocketStructure();
        int[] szRef = new int[]{in_addr.size()};
        ByteBuffer buf = p.getContent();
        int n = recvfrom(getSocket(), buf, buf.capacity(), 0, in_addr, szRef);
        p.setLength(n);
        p.setAddressable(in_addr);
        return n;
    }

    @Override
    public int send(DatagramPacket p) throws NetworkUnreachableException {
        SocketStructure destAddr = new SocketStructure(p.getAddress(), p.getPort());
        ByteBuffer buf = p.getContent();
        try {
            return sendto(getSocket(), buf, buf.remaining(), 0, destAddr, destAddr.size());
        } catch (LastErrorException e) {
            if (e.getMessage().contains("[101]")) {
                throw new NetworkUnreachableException();
            }
            throw e;
        }
    }

    @Override
    public void close() {
        close(getSocket());
    }
}
