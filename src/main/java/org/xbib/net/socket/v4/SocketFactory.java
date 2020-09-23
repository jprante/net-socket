package org.xbib.net.socket.v4;

import com.sun.jna.Platform;
import org.xbib.net.socket.v4.datagram.DatagramSocket;

import java.lang.reflect.InvocationTargetException;

public class SocketFactory {

    public static final int SOCK_DGRAM = Platform.isSolaris() ? 1 : 2;

    public static DatagramSocket createDatagramSocket(int protocol, int port)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<? extends DatagramSocket> implementationClass = Class.forName(getImplementationClassName())
                .asSubclass(DatagramSocket.class);
        return implementationClass
                .getDeclaredConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE)
                .newInstance(SOCK_DGRAM, protocol, port);
    }


    private static String getImplementationClassName() {
        return "org.xbib.net.socket.v4." + getArchName() + ".NativeDatagramSocket";
    }

    private static String getArchName() {
        return Platform.isWindows() ? "win32"
                : Platform.isSolaris() ? "sun"
                : (Platform.isMac() || Platform.isFreeBSD() || Platform.isOpenBSD()) ? "bsd"
                : "unix";
    }
}
