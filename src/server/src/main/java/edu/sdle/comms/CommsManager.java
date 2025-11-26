package edu.sdle.comms;

import edu.sdle.Worker;
import org.zeromq.*;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommsManager {
    private final ZContext context;
    private final ZMQ.Socket frontend;
    private final ZMQ.Socket backend;
    private final Queue<byte[]> freeWorkers;
    private final ExecutorService workerPool;
    private final ExecutorService backupPool;

    private final int noWorkers;
    private boolean running = false;

    public CommsManager(int noWorkers, int noBackupWorkers) {
        context = new ZContext();
        this.noWorkers = noWorkers;
        freeWorkers = new LinkedList<>();

        // Create sockets
        frontend = context.createSocket(SocketType.ROUTER);
        backend = context.createSocket(SocketType.ROUTER);

        // Create thread pools
        var vfactory = Thread.ofVirtual().factory();
        workerPool = Executors.newFixedThreadPool(noWorkers, vfactory);
        backupPool = Executors.newFixedThreadPool(noBackupWorkers, vfactory);
    }

    public void init(String bindAddress) {
        running = true;
        frontend.bind("tcp://" + bindAddress);
        backend.bind("inproc://backend");

        // Start workers
        for (int i = 0; i < noWorkers; i++)
            workerPool.submit(new Worker(context, backupPool, i));

        poll();
    }

    public void shutdown() {
        running = false;
        backend.close();
        frontend.close();
        backupPool.shutdown();
        workerPool.shutdown();
        context.close();
    }

    private void poll() {
        ZMQ.Poller poller = context.createPoller(2);
        poller.register(backend, ZMQ.Poller.POLLIN);
        poller.register(frontend, ZMQ.Poller.POLLIN);

        while (running) {
            poller.poll(1000);

            if (poller.pollin(1) && !freeWorkers.isEmpty()) {
                ZMsg msg = ZMsg.recvMsg(frontend);

                // [identity][empty][type]
                if (msg.size() >= 3) {
                    msg.push(freeWorkers.poll());
                    msg.send(backend);
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
}
