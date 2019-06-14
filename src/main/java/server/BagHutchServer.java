package server;

import client.Client;
import client.RemoteClient;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import partitions.Coordinator;
import partitions.Node;
import store.Store;

import java.util.List;

public class BagHutchServer {
    private Server server;
    private int port;

    String nodeId;
    Coordinator coordinator;
    Client localClient;
    List<RemoteClient> remoteClients;
    Store store;
    List<Node> nodes;

    public BagHutchServer(String nodeId, int port, Store store, Coordinator coordinator, Client client,
                          List<Node> nodes, List<RemoteClient> remoteClients) {
        this.nodeId = nodeId;
        this.store = store;
        this.port = port;
        this.coordinator = coordinator;
        this.localClient = client;
        this.remoteClients = remoteClients;
        this.nodes = nodes;
    }

    public void run() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[] { connector });

        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        BagHutchServlet servlet = new BagHutchServlet(coordinator);
        servletHandler.addServletWithMapping(new ServletHolder(servlet), "/api");
        BagHutchInternalServlet internalServlet = new BagHutchInternalServlet(localClient);
        servletHandler.addServletWithMapping(new ServletHolder(internalServlet), "/internal");

        server.start();
        server.join();
    }

    public void stop() throws Exception {
        server.stop();
    }

}
