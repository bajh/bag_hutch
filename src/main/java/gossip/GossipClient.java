package gossip;

import java.io.IOException;
import java.net.*;

public class GossipClient implements Client {
    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    public GossipClient(String host, int port) throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        address = InetAddress.getByName(host);
        this.port = port;
    }

    public void sendMessage(GossipAction action) throws IOException {
        byte[] b = Encoder.encode(action);
        DatagramPacket packet = new DatagramPacket(b, b.length, address, port);
        socket.send(packet);
    }

    public void close() {
        socket.close();
    }
}


