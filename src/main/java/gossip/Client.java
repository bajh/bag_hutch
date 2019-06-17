package gossip;

import java.io.IOException;

public interface Client {
    public void sendMessage(GossipAction action) throws IOException;
}
