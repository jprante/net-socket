package org.xbib.net.socket.v4.ping;

import org.xbib.net.socket.Metric;
import org.xbib.net.socket.NetworkUnreachableException;
import org.xbib.net.socket.v4.SocketFactory;
import org.xbib.net.socket.v4.datagram.DatagramPacket;
import org.xbib.net.socket.v4.datagram.DatagramSocket;
import org.xbib.net.socket.v4.ip.Packet;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
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

    private final List<PingResponseListener> listeners;

    private volatile boolean closed;

    private Thread thread;

    private PingMetric metric;

    public Ping(int id)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this(SocketFactory.createDatagramSocket(DatagramSocket.IPPROTO_ICMP, id));
    }

    public Ping(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
        this.throwable = new AtomicReference<>(null);
        this.listeners = new ArrayList<>();
        this.closed = false;
    }

    public Metric getMetric() {
        return metric;
    }

    public DatagramSocket getPingSocket() {
        return datagramSocket;
    }

    public boolean isFinished() {
        return closed;
    }

    public void start() {
        thread = new Thread(this, "PingThread:PingListener");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() throws InterruptedException {
        closed = true;
        if (thread != null) {
            thread.interrupt();
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

    public void execute(int id, Inet4Address addr)
            throws InterruptedException, NetworkUnreachableException {
        Thread t = new Thread(this);
        t.start();
        execute(id, addr, 1, 10, 1000);
    }

    public void execute(int id,
                        Inet4Address addr,
                        int sequenceNumber,
                        int count,
                        long interval)
            throws InterruptedException, NetworkUnreachableException {
        metric = new PingMetric(count, interval);
        addPingReplyListener(metric);
        final DatagramSocket socket = getPingSocket();
        for(int i = sequenceNumber; i < sequenceNumber + count; i++) {
            final PingRequest request = new PingRequest(id, i);
            request.send(socket, addr);
            Thread.sleep(interval);
        }
    }

    @Override
    public void run() {
        try {
            final DatagramPacket datagram = new DatagramPacket(65535);
            while (!isFinished()) {
                getPingSocket().receive(datagram);
                final long received = System.nanoTime();
                final org.xbib.net.socket.v4.icmp.Packet packet = new org.xbib.net.socket.v4.icmp.Packet(getPayload(datagram));
                final PingResponse echoReply = packet.getType() == org.xbib.net.socket.v4.icmp.Packet.Type.EchoReply ? new PingResponse(packet, received) : null;
                if (echoReply != null && echoReply.isValid()) {
                    logger.log(Level.INFO, String.format("%d bytes from %s: tid=%d icmp_seq=%d time=%.3f ms%n",
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
        return new Packet(datagram.getContent()).getPayload();
    }
}
