package edu.sdle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sdle.comms.RequestType;
import org.zeromq.*;

import java.util.UUID;
import java.util.concurrent.*;

public class SimpleClient {
    private final static ObjectMapper MAPPER = new ObjectMapper();

    // -------------------------------
    // SIMPLE STATE REQUEST
    // -------------------------------
    public static void main3(String[] args) throws JsonProcessingException {
        try (ZContext ctx = new ZContext();
             ZMQ.Socket client = ctx.createSocket(SocketType.REQ)) {

            client.connect("tcp://localhost:5558");

            System.out.println("Sending STATE request to proxy");
            client.send("STATE");

            var reply = client.recvStr();
            System.out.println("Received: " + reply);

            if (reply.equals("0")) {
                var payload = client.recvStr();
                ProxyState state = MAPPER.readValue(payload, ProxyState.class);
                System.out.println("Received: " + payload);
                System.out.println("Proxy state: " + state);
            }
        }
    }

    // -------------------------------
    // SIMPLE SINGLE-REQUEST TEST
    // -------------------------------
    public static void main4(String[] args) {
        try (ZContext ctx = new ZContext();
             ZMQ.Socket client = ctx.createSocket(SocketType.REQ)) {


            client.connect("tcp://localhost:5555");

            System.out.println("Sending single GET request...");
            client.sendMore(RequestType.GET.toString());

            UUID id = UUID.fromString(UUID.randomUUID().toString());
            client.send(UUIDUtils.uuidToBytes(id));

            var reply = client.recvStr();
            System.out.println("Received: " + reply);

            if (reply.equals("0")) {
                var payload = client.recvStr();
                System.out.println("Received: " + payload);
            }
        }
    }

    public static void main(String[] args) {
        try (ZContext ctx = new ZContext();
             ZMQ.Socket client = ctx.createSocket(SocketType.REQ)) {


            client.connect("tcp://localhost:5556");

            System.out.println("Sending single PUT request...");
            client.sendMore(RequestType.PUT.toString());

            client.sendMore(UUIDUtils.uuidToBytes(UUID.randomUUID()));

            client.send("abc");

            var reply = client.recvStr();
            System.out.println("Received: " + reply);

            if (reply.equals("0")) {
                var payload = client.recvStr();
                System.out.println("Received: " + payload);
            }
        }
    }

    // -------------------------------
    // ORIGINAL MULTI-THREADED TEST
    // -------------------------------
    public static void main2(String[] args) throws InterruptedException {
        final int NUM_REQUESTS = 4;

        try (ZContext ctx = new ZContext()) {
            ExecutorService pool = Executors.newFixedThreadPool(NUM_REQUESTS);
            long start = System.nanoTime();

            CountDownLatch latch = new CountDownLatch(NUM_REQUESTS);

            for (int i = 0; i < NUM_REQUESTS; i++) {
                final int id = i;
                pool.submit(() -> {
                    System.out.println("about to try");
                    try (ZMQ.Socket client = ctx.createSocket(SocketType.REQ)) {
                        client.connect("tcp://localhost:5555");
                        client.sendMore("GET");
                        client.send("" + id);
                        String reply = client.recvStr();
                        System.out.println("Thread " + id + " received: " + reply);
                    }
                    latch.countDown();
                });
            }

            latch.await();
            pool.shutdown();

            long end = System.nanoTime();
            double elapsedMs = (end - start) / 1_000_000.0;
            System.out.printf("All requests completed in %.2f ms\n", elapsedMs);
        }
    }
}
