package org.xbib.net.socket.v4.unix;

import com.sun.jna.Structure;
import org.xbib.net.socket.v4.Addressable;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class SocketStructure extends Structure implements Addressable {

    public static final int AF_INET = 2;

    public short sin_family;

    public byte[] sin_port;

    public byte[] sin_addr;

    public byte[] sin_zero = new byte[8];

    public SocketStructure() {
        this(AF_INET, null, 0);
    }

    public SocketStructure(int port) {
        this(AF_INET, null, port);
    }

    public SocketStructure(Inet4Address address, int port) {
        this(AF_INET, address, port);
    }

    public SocketStructure(int family, Inet4Address address, int port) {
        this.sin_family = (short) (0xffff & family);
        setAddress(address);
        setPort(port);
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("sin_family", "sin_port", "sin_addr", "sin_zero");
    }

    @Override
    public Inet4Address getAddress() {
        try {
            return (Inet4Address) Inet4Address.getByAddress(sin_addr);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public void setAddress(Inet4Address address) {
        if (address != null) {
            byte[] addr = address.getAddress();
            assertLen("address", addr, 4);
            this.sin_addr = addr;
        } else {
            this.sin_addr = new byte[] { 0,0,0,0 };
        }
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
        if (port >= 0) {
            byte[] p = new byte[]{(byte) (0xff & (port >> 8)), (byte) (0xff & port)};
            assertLen("port", p, 2);
            this.sin_port = p;
        }
    }

    private void assertLen(String field, byte[] addr, int len) {
        if (addr.length != len) {
            throw new IllegalArgumentException(field + " length must be " + len + " bytes");
        }
    }
}
