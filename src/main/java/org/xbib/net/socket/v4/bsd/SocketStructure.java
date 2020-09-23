package org.xbib.net.socket.v4.bsd;

import com.sun.jna.Structure;
import org.xbib.net.socket.v4.Addressable;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class SocketStructure extends Structure implements Addressable {

    public static final int AF_INET = 2;

    public byte sin_len;
    public byte sin_family;
    public byte[] sin_port;
    public byte[] sin_addr;
    public byte[] sin_zero = new byte[8];

    public SocketStructure(int family, byte[] addr, byte[] port) {
        sin_family = (byte) (0xff & family);
        assertLen("port", port, 2);
        sin_port = port.clone();
        assertLen("address", addr, 4);
        sin_addr = addr.clone();
        sin_len =  15; // (byte) (0xff & size());
    }

    public SocketStructure() {
        this((byte) 0, new byte[4], new byte[2]);
    }

    public SocketStructure(InetAddress address, int port) {
        this(AF_INET,
                address.getAddress(),
                new byte[]{(byte) (0xff & (port >> 8)), (byte) (0xff & port)});
    }

    public SocketStructure(final int port) {
        this(AF_INET,
                new byte[4],
                new byte[]{(byte) (0xff & (port >> 8)), (byte) (0xff & port)});
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("sin_len", "sin_family", "sin_port", "sin_addr", "sin_zero");
    }

    private void assertLen(String field, byte[] addr, int len) {
        if (addr.length != len) {
            throw new IllegalArgumentException(field + " length must be " + len + " bytes");
        }
    }

    @Override
    public Inet4Address getAddress() {
        try {
            return (Inet4Address) InetAddress.getByAddress(sin_addr);
        } catch (UnknownHostException e) {
            // this can't happen because we ensure the sin_addr always has length 4
            return null;
        }
    }

    public void setAddress(InetAddress address) {
        byte[] addr = address.getAddress();
        assertLen("address", addr, 4);
        sin_addr = addr;
    }

    @Override
    public int getPort() {
        int port = 0;
        for (int i = 0; i < 2; i++) {
            port = ((port << 8) | (sin_port[i] & 0xff));
        }
        return port;
    }

    public void setPort(int port) {
        byte[] p = new byte[]{(byte) (0xff & (port >> 8)), (byte) (0xff & port)};
        assertLen("port", p, 2);
        sin_port = p;
    }
}
