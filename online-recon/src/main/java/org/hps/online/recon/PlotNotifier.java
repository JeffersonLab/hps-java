package org.hps.online.recon;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class PlotNotifier extends WebSocketServer {

    static Logger LOG = Logger.getLogger(PlotNotifier.class.getPackage().getName());

    static PlotNotifier INSTANCE = null;

    static private final int DEFAULT_PORT = 8887;

    public static PlotNotifier instance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new PlotNotifier(DEFAULT_PORT);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        return INSTANCE;
    }

    private PlotNotifier(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
        LOG.info("PlotNotifier starting on port: " + port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOG.info("PlotNotifier got new connection from " + conn.getRemoteSocketAddress().getHostString() + " on port "
                + conn.getRemoteSocketAddress().getPort());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
    }

    @Override
    public void onStart() {
    }
}
