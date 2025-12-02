package edu.sdle.comms;

import org.zeromq.*;

public class Worker implements Runnable {
    private final ZContext context;
    private final int workerIndex;

    private final RequestHandler handler;

    public Worker(ZContext context, int workerIndex, RequestHandler handler) {
        this.context = context;
        this.workerIndex = workerIndex;
        this.handler = handler;
    }

    @Override
    public void run() {

    }
}
/*
    public void run() {
        Thread.currentThread().setName("Worker-" + workerIndex);

        ZMQ.Socket worker = context.createSocket(SocketType.DEALER);
        worker.connect("inproc://backend");
        worker.send("READY"); // signal READY

        System.out.println("Worker " + workerIndex + " is listening");

        try {
            while (!Thread.currentThread().isInterrupted()) {
                ZMsg msg = ZMsg.recvMsg(worker);
                byte[] clientId = msg.pop().getData();
                msg.pop();

                var reply = handler.handleRequest(msg);

                // Prepend identity and reply
                reply.push("");
                reply.push(clientId);
                reply.send(worker);
            }
        } catch (Exception e) {
            System.err.println("[" + Thread.currentThread().getName() + "] Worker thread interrupted\n" + e);
        }
    }

}
*/