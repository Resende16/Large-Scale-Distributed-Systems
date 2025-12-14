package edu.sdle.comms;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static edu.sdle.comms.ReplyCode.FAILURE_BUSY;
import static java.util.concurrent.TimeUnit.SECONDS;

public class CommsManager {
    private final ZContext context;
    private final ZMQ.Socket frontend;
    private final ZMQ.Socket backend;
    private final Queue<byte[]> freeWorkers;

    private final ExecutorService workerPool;
    private final ExecutorService backupPool;
    private Thread pollerThread;

    private final int noWorkers;
    private boolean running = false;
    private boolean frozen = false;

    public CommsManager(int noWorkers, int noBackupWorkers) {
        context = new ZContext();
        this.noWorkers = noWorkers;
        freeWorkers = new LinkedList<>();

        // Create sockets
        frontend = context.createSocket(SocketType.ROUTER);
        frontend.setMaxMsgSize(1024 * 1024); // 1MB

        backend = context.createSocket(SocketType.ROUTER);

        // Create thread pools
        var vfactory = Thread.ofVirtual().factory();
        workerPool = Executors.newFixedThreadPool(noWorkers, vfactory);
        backupPool = Executors.newFixedThreadPool(noBackupWorkers, vfactory);
    }

    public void init(String bindAddress, RequestHandler handler) {
        running = true;

        // Just in case
        bindAddress = bindAddress
                .replace("localhost:", "*:")
                .replace("127.0.0.1:", "*:");

        frontend.bind(bindAddress);
        backend.bind("inproc://backend");

        // Start workers
        for (int i = 0; i < noWorkers; i++)
            workerPool.submit(new Worker(context, i, handler));

        System.out.println("Node is listening on " + bindAddress);

        pollerThread = Thread.ofVirtual().start(this::poll);
    }

    private void poll() {
        ZMQ.Poller poller = context.createPoller(2);
        poller.register(backend, ZMQ.Poller.POLLIN);
        poller.register(frontend, ZMQ.Poller.POLLIN);

        while (running) {
            // For testing
            if (frozen) {
                try {
                    Thread.sleep(15000);
                    frozen = false;
                } catch (InterruptedException _) {}
                continue;
            }

            poller.poll(1000);

            if (poller.pollin(1)) {
                ZMsg msg = ZMsg.recvMsg(frontend);

                // [identity][empty][type]
                if (msg.size() >= 3) {
                    if (!freeWorkers.isEmpty()) {
                        msg.push(freeWorkers.poll());
                        msg.send(backend);
                    }
                    else {
                        // All workers busy, reject
                        byte[] clientId = msg.pop().getData();
                        ReplyUtils.failure(FAILURE_BUSY)
                                .push("")
                                .push(clientId)
                                .send(frontend);
                    }
                }
            }

            // Poll messages from workers
            if (poller.pollin(0)) {
                ZMsg msg = ZMsg.recvMsg(backend);
                var workerId = msg.pop().getData();

                if (msg.size() == 1 && msg.pop().toString().equals("READY")) {
                    // Worker signaled READY
                    freeWorkers.offer(workerId);
                }
                else {
                    // Worker finished task, forward reply
                    msg.send(frontend);
                    freeWorkers.offer(workerId);
                }
            }
        }
    }

    public void shutdown() {
        running = false;
        workerPool.shutdownNow();
        backupPool.shutdownNow();

        // Wait for workers to finish
        try {
            if (!workerPool.awaitTermination(2, SECONDS))
                System.err.println("Worker pool did not terminate in time!");
            if (!backupPool.awaitTermination(2, SECONDS))
                System.err.println("Backup pool did not terminate in time!");

            if (pollerThread != null)
                pollerThread.join(2000);

        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }

        backend.setLinger(0);
        frontend.setLinger(0);
        backend.close();
        frontend.close();
        context.close();
    }

    public ZContext getContext() {
        return context;
    }

    public ExecutorService getBackupPool() {
        return backupPool;
    }

    public void freeze() {
        frozen = true;
    }
}
