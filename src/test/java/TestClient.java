import client.Client;
import client.LocalClient;
import data.Context;
import data.Record;
import data.VectorClock;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import store.HashIndexStore;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableRuleMigrationSupport
public class TestClient {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testLocalClient() throws Exception {
        File dataDir = tempFolder.newFolder();

        HashIndexStore store = new HashIndexStore(dataDir);
        store.loadIndex();

        Client client = new LocalClient("node1", store);

        Context context = client.put("key1", "val1", Context.empty()).join();
        client.put("key1", "val2", context).join();
        client.put("key2", "val3", Context.empty()).join();

        Record record = client.get("key1").join();
        assertEquals(record, new Record("val2", new VectorClock(entry("node1", 2))));
        record = client.get("key2").join();
        assertEquals(record, new Record("val3", new VectorClock(entry("node1", 1))));
    }
}
