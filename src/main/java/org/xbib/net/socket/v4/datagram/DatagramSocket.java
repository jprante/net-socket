package org.xbib.net.socket.v4.datagram;

import org.xbib.net.socket.NetworkUnreachableException;

import java.io.Closeable;
import java.io.IOException;
import java.net.UnknownHostException;

public interface DatagramSocket extends Closeable {

    int IPPROTO_IP = 0;

    int IPPROTO_IPV6 = 41;

    int IPPROTO_ICMP = 1;

    int IPPROTO_UDP = 17;

    int IPPROTO_ICMPV6 = 58;

    int IP_MTU_DISCOVER = 10;

    int IPV6_DONTFRAG = 62;

    int getSocket();

    void allowFragmentation(boolean frag) throws IOException;

    void setTrafficClass(int tc) throws IOException;

    int receive(DatagramPacket p) throws UnknownHostException;

    int send(DatagramPacket p) throws NetworkUnreachableException;

}
