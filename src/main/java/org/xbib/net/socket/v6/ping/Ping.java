package org.xbib.net.socket.v6.ping;

import org.xbib.net.socket.Metric;
import org.xbib.net.socket.NetworkUnreachableException;
import org.xbib.net.socket.v6.SocketFactory;
import org.xbib.net.socket.v6.datagram.DatagramPacket;
import org.xbib.net.socket.v6.datagram.DatagramSocket;
import org.xbib.net.socket.v6.icmp.Packet;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet6Address;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Ping implements Runnable, Closeable {

    private static final Logger logger = Logger.getLogger(Ping.class.getName());

    public static final long COOKIE = StandardCharsets.US_ASCII.encode("org.xbib").getLong(0);

    private final DatagramSocket datagramSocket;

    private final AtomicReference<Throwable> throwable;

    private final Metric metric;

    private final List<PingResponseListener> listeners;

    private Thread thread;

    private volatile boolean closed;

    public Ping(int id) throws Exception {
        this(SocketFactory.createDatagramSocket(DatagramSocket.IPPROTO_ICMPV6, id));
    }

    public Ping(DatagramSocket pingSocket) {
        datagramSocket = pingSocket;
        this.throwable = new AtomicReference<>(null);
        this.metric = new Metric();
        this.listeners = new ArrayList<>();
        this.closed = false;
    }

    public DatagramSocket getPingSocket() {
        return datagramSocket;
    }

    public int getCount() {
        return metric.getCount();
    }

    public boolean isFinished() {
        return closed;
    }

    public void start() {
        thread = new Thread(this, "PingThreadTest:PingListener");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() throws InterruptedException {
        closed = true;
        if (thread != null) {
            thread.interrupt();
            //m_thread.join();
        }
        thread = null;
    }

    @Override
    public void close() throws IOException {
        if (getPingSocket() != null) {
            getPingSocket().close();
        }
    }

    protected List<PingResponseListener> getListeners() {
        return listeners;
    }

    public void addPingReplyListener(PingResponseListener listener) {
        listeners.add(listener);
    }

    public PingMetric execute(int id, Inet6Address addr) throws InterruptedException, NetworkUnreachableException {
        Thread t = new Thread(this);
        t.start();
        return execute(id, addr, 1, 10, 1000);
    }

    public PingMetric execute(int id,
                              Inet6Address addr,
                              int sequenceNumber,
                              int count,
                              long interval) throws InterruptedException, NetworkUnreachableException {
        PingMetric metric = new PingMetric(count, interval);
        addPingReplyListener(metric);
        DatagramSocket socket = getPingSocket();
        for (int i = sequenceNumber; i < sequenceNumber + count; i++) {
            PingRequest request = new PingRequest(id, i);
            request.send(socket, addr);
            Thread.sleep(interval);
        }
        return metric;
    }

    @Override
    public void run() {
        try {
            DatagramPacket datagram = new DatagramPacket(65535);
            while (!isFinished()) {
                getPingSocket().receive(datagram);
                final long received = System.nanoTime();
                final Packet icmpPacket = new Packet(getPayload(datagram));
                final PingResponse echoReply = icmpPacket.getType() == Packet.Type.EchoReply ? new PingResponse(icmpPacket, received) : null;
                if (echoReply != null && echoReply.isValid()) {
                    // 64 bytes from 127.0.0.1: icmp_seq=0 time=0.069 ms
                    logger.log(Level.INFO, String.format("%d bytes from [%s]: tid=%d icmp_seq=%d time=%.3f ms%n",
                        echoReply.getPacketLength(),
                        datagram.getAddress().getHostAddress(),
                        echoReply.getIdentifier(),
                        echoReply.getSequenceNumber(),
                        echoReply.elapsedTime(TimeUnit.MILLISECONDS)));
                    for (PingResponseListener listener : getListeners()) {
                        listener.onPingResponse(datagram.getAddress(), echoReply);
                    }
                }
            }
        } catch(final Throwable e) {
            throwable.set(e);
            e.printStackTrace();
        }
    }

    private ByteBuffer getPayload(final DatagramPacket datagram) {
        return datagram.getContent();
    }
}
