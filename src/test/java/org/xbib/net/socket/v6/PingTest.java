package org.xbib.net.socket.v6;

import org.junit.jupiter.api.Test;
import org.xbib.net.socket.v6.ping.Ping;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

class PingTest {

    @Test
    void ping() throws Exception {
        Ping ping = new Ping(0);
        ping.execute(1234, getAddress("fl-test.hbz-nrw.de"));
    }

    private Inet6Address getAddress(String host) throws UnknownHostException {
        for (InetAddress addr : InetAddress.getAllByName(host)) {
            if (addr instanceof Inet6Address) {
                return (Inet6Address) addr;
            }
        }
        return null;
    }
}
