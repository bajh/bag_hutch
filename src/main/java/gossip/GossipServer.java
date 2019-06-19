package gossip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class GossipServer extends Thread {
    private DatagramSocket socket;
    private FailureDetector failureDetector;
    private boolean running;
    private byte[] buf = new byte[1024]; // TODO: figure out the actual maximum packet size

    public GossipServer(int port, FailureDetector failureDetector) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.failureDetector = failureDetector;
    }

    public void run() {
        running = true;

        failureDetector.start();

        while (running) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            try {
                failureDetector.handleAction(Encoder.decode(packet.getData()));
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        socket.close();
    }

    public void close() {
        running = false;
    }
}
