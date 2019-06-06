import client.HttpClient;
import data.Context;
import data.Record;
import data.VectorClock;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TestCoordinator {

    private class GetTest {
        String key;
        Record[] records;
        Record expectedRecord;

        int n;
        int r;
        int w;

        public GetTest(String key, Record[] records, Record expectedRecord, int n, int r, int w) {
            this.key = key;
            this.records = records;
            this.expectedRecord = expectedRecord;
            this.n = n;
            this.r = r;
            this.w = w;
        }
    }

    // TODO: test that exception is thrown if we aren't able to get enough reads

    @Test
    public void testGet() throws Exception {
        String testKey = "test";

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

    private class PutTest {
        String key;
        String value;

        int n;
        int r;
        int w;

        int nSuccess;
        boolean expectException;
        Context expectedContext;

        public PutTest(String key, String value, int n, int r, int w, int nSuccess,
                       boolean expectException, Context expectedContext) {
            this.key = key;
            this.value = key;
            this.n = n;
            this.r = r;
            this.w = w;
            this.nSuccess = nSuccess;
            this.expectException = expectException;
        }
    }

    @Test
    public void testPut() throws Exception {
        PutTest test1 = new PutTest("a", "b", 5, 1, 3, 3,
                false, null);
        PutTest test2 = new PutTest("a", "b", 5, 1, 3, 5,
                false, null);
        PutTest test3 = new PutTest("a", "b", 5, 1, 3, 2,
                true, null);

        PutTest[] putTests = new PutTest[]{test1, test2, test3};

        for (PutTest test : putTests) {
            List<Node> nodes = new ArrayList<>();
            for (int i = 0; i < test.nSuccess; i++) {
                nodes.add(new Node(Integer.toString(i), Mockito.mock(HttpClient.class)));
                CompletableFuture future = new CompletableFuture();
                future.complete("a");
                Mockito.when(nodes.get(i).getClient().put(test.key, test.value, null)).thenReturn(future);
            }
            for (int i = test.nSuccess; i < test.n; i++) {
                nodes.add(new Node(Integer.toString(i), Mockito.mock(HttpClient.class)));
                CompletableFuture future = new CompletableFuture();
                future.completeExceptionally(new Exception("everything is bad"));
                Mockito.when(nodes.get(i).getClient().put(test.key, test.value, null)).thenReturn(future);
            }

            Ring ring = new Ring(32, nodes);
            Coordinator coordinator = new Coordinator("1", test.n, test.r, test.w, ring);
            CompletableFuture result = coordinator.put(test.key, test.value, null);
            if (test.expectException) {
                assertThrows(CompletionException.class, () -> { result.join(); });
            } else {
                Context newContext = (Context) result.join();
                assertEquals(newContext, test.expectedContext);
            }

        }
    }
}
