import client.GetResponse;
import client.HttpClient;
import data.Record;
import data.VectorClock;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCoordinator {

    private class GetTest {
        byte[] key;
        Record[] records;
        Record expectedRecord;

        int n;
        int r;
        int w;

        public GetTest(byte[] key, Record[] records, Record expectedRecord, int n, int r, int w) {
            this.key = key;
            this.records = records;
            this.expectedRecord = expectedRecord;
            this.n = n;
            this.r = r;
            this.w = w;
        }
    }

    @Test
    public void testGet() throws Exception {
        byte[] testKey = "test".getBytes();

        // test conflicts
        GetTest test1 = new GetTest(
                testKey,
                new Record[]{
                        new Record("data 1", new VectorClock(entry("1", 1), entry("2", 1))),
                        new Record("data 2", new VectorClock(entry("2", 2))),
                        new Record("data 1", new VectorClock(entry("1", 1), entry("2", 1)))
                },
                new Record(new String[]{"data 1", "data 2"}, new VectorClock(entry("1", 1), entry("2", 2))),
            3,
            3,
            3);

        // test nodes that agree
        GetTest test2 = new GetTest(
                testKey,
                new Record[]{
                        new Record("data 1", new VectorClock(entry("1", 1), entry("2", 3), entry("3", 3))),
                        new Record("data 1", new VectorClock(entry("1", 1), entry("2", 3), entry("3", 3))),
                        new Record("data 1", new VectorClock(entry("1", 1), entry("2", 3), entry("3", 3)))
                },
                new Record("data 1", new VectorClock(entry("1", 1), entry("2", 3), entry("3", 3))),
                3,
                3,
                3);

        // test child
        GetTest test3 = new GetTest(
            testKey,
                new Record[]{
                        new Record("data 1", new VectorClock(entry("1", 2))),
                        new Record("data 2", new VectorClock(entry("1", 1))),
                        new Record("data 3", new VectorClock(entry("1", 3)))
                },
                new Record("data 3", new VectorClock(entry("1", 3))),
                3,
                3,
                3);

        // test conflicts within a single node
        GetTest test4 = new GetTest(
                testKey,
                new Record[]{
                        new Record(new String[]{"data 1", "data 2"}, new VectorClock(entry("1", 1), entry("2", 1))),
                        new Record(new String[]{"data 3", "data 4"}, new VectorClock(entry("2", 2))),
                        new Record(new String[]{"data 1", "data 2"}, new VectorClock(entry("1", 1), entry("2", 1)))
                },
                new Record(new String[]{"data 1", "data 2", "data 3", "data 4"}, new VectorClock(entry("1", 1), entry("2", 2))),
                3,
                3,
                3);

        GetTest[] getTests = new GetTest[]{test1, test2, test3, test4};

        for (GetTest test : getTests) {
            List<Node> nodes = new ArrayList<>();
            for (int i = 0; i < test.records.length; i++) {
                nodes.add(new Node(Integer.toString(i), Mockito.mock(HttpClient.class)));
                CompletableFuture future = new CompletableFuture();
                future.complete(test.records[i]);
                Mockito.when(nodes.get(i).getClient().get(testKey)).thenReturn(future);
            }

            Ring ring = new Ring(32, nodes);
            Coordinator coordinator = new Coordinator("1", test.n, test.r, test.w, ring);
            CompletableFuture result = coordinator.get(testKey);
            Record record = (Record) result.join();
            assertEquals(test.expectedRecord, record);
        }

    }
}
