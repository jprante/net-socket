package org.xbib.net.socket.v4;

import org.junit.jupiter.api.Test;
import org.xbib.net.socket.v4.ping.Ping;
import java.net.Inet4Address;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

class PingTest {

    @Test
    void ping() throws Exception {
        Ping ping = new Ping(0);
        ping.execute(1234, (Inet4Address) Inet4Address.getByName("localhost"));
        ping.close();
        Logger.getAnonymousLogger().log(Level.INFO, ping.getMetric().getSummary(TimeUnit.MILLISECONDS));
    }
}
