package edu.sdle.comms;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

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

        try (worker) {
            worker.connect("inproc://backend");
            worker.setReceiveTimeOut(500);
            worker.send("READY"); // signal READY

            while (!Thread.currentThread().isInterrupted()) {
                ZMsg msg = ZMsg.recvMsg(worker);
                if (msg == null)
                    continue;

                byte[] clientId = msg.pop().getData();
                msg.pop();

                var reply = handler.handleRequest(msg);

                // Prepend identity and reply
                reply.push("");
                reply.push(clientId);
                reply.send(worker);
            }
        } catch (org.zeromq.ZMQException e) {
            // Print unless exception due to shutdown
            if (e.getErrorCode() != 156384765 && e.getErrorCode() != 4)
                System.err.println("[" + Thread.currentThread().getName() + "] Worker thread interrupted\n" + e);
        }
    }
}
*/