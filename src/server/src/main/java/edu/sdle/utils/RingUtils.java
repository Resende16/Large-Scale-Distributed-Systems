package edu.sdle.utils;

import edu.sdle.HintStorage;
import edu.sdle.hashring.DistributedHashRing;
import edu.sdle.hashring.Node;
import edu.sdle.storage.Storage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.*;

import static edu.sdle.hashring.NodeStatus.*;

public class RingUtils {
    public static <T> List<T> difference(List<T> A, List<T> B) {
        List<T> result = new ArrayList<>(A);
        result.removeAll(B);
        return result;
    }

    // Get the stored keys a node is responsible for, and,
    // for each, all nodes responsible for them
    public static Map<UUID, List<Node>> getKeyRangeForNode(DistributedHashRing ring, Set<UUID> keys, Node node) {
        Map<UUID, List<Node>> result = new HashMap<>();

        for (UUID key : keys) {
            var responsibleNodes = ring.getNNodes(key);

            if (responsibleNodes.contains(node))
                result.put(key, responsibleNodes);
        }
        return result;
    }

    // Get the set of keys the node used to own,  and, for each, compare its
    // old and new preference list; if there are newly responsible nodes,
    // those are added as keys to transfer along with the nodes to transfer to
    public static Map<UUID, List<Node>> getKeysToTransfer(DistributedHashRing oldRing,
                                                          DistributedHashRing newRing,
                                                          Set<UUID> keys,
                                                          Node node)
    {
        var oldKeys = getKeyRangeForNode(oldRing, keys, node);
        //var newKeys = getKeyRangeForNode(newRing, keys, node);

        Map<UUID, List<Node>> result = new HashMap<>();

        for (var entry : oldKeys.entrySet()) {
            var key = entry.getKey();
            var oldResponsibleNodes = entry.getValue();
            var newResponsibleNodes = newRing.getNNodes(key);

            var diff = difference(newResponsibleNodes, oldResponsibleNodes);
            if (!diff.isEmpty())
                result.put(key, diff);
        }

        result.remove(node.getId()); // remove self just in case
        return result;
    }

    // Given a change in the hash ring, send all keys a node owns to
    // the newly responsible nodes for that key
    public static void transferKeys(DistributedHashRing oldRing,
                                    DistributedHashRing newRing,
                                    Storage storage,
                                    ZMQ.Socket socket,
                                    Node node)
    {
        // Check for membership changes that require data transfers
        var transfers = RingUtils.getKeysToTransfer(oldRing, newRing, storage.getKeys(), node);

        for (var entry : transfers.entrySet()) {
            var key = entry.getKey();

            var value = storage.get(key);
            if (value == null)
                continue;

            var nodes = entry.getValue();

            var preventDeletion = false;

            for (var _node : nodes) {
                var reply = RequestUtils.
                        makeRPutRequest(socket, _node, key, value);
                // If reply is null, then the node might've gone down
                // before it got its keys; therefore we should keep them
                preventDeletion |= (reply == null);
            }

            // Delete if no longer responsible for it and all nodes responded
            //todo: Prevent deletion if response did not succeed??
            if (!preventDeletion &&
                    !newRing.getNNodes(key).contains(node))
                storage.remove(key);
        }
    }

    // utility to execute function above when caller has
    // no immediate access to a socket
    public static void transferKeys(DistributedHashRing oldRing,
                                    DistributedHashRing newRing,
                                    Storage storage,
                                    Node node)
    {
        var context = new ZContext();
        var socket = context.createSocket(SocketType.REQ);

        socket.setReceiveTimeOut(1000);
        socket.setSendTimeOut(300);
        socket.setReqRelaxed(true);
        socket.setLinger(0);

        transferKeys(oldRing, newRing, storage, socket, node);

        socket.close();
        context.destroy();
    }

    // Transfer all stored hints to node that are back "UP"
    public static void transferHints(DistributedHashRing oldRing,
                                     DistributedHashRing newRing,
                                     HintStorage hintStorage,
                                     ZMQ.Socket socket)
    {
        for (var node : newRing.getNodes()) {
            var id = node.getId();

            if (node.getStatus() != UP)
                continue; // still down

            var oldNode = oldRing.getNode(id);
            if (oldNode == null)
                continue; // wasn't in the ring before

            if (oldNode.getStatus() == UP)
                continue; // was already up before

            var hints = hintStorage.getHintsForNode(id);

            var hintsSent = 0;

            // Send hints
            for (var entry : hints.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();

                var reply = RequestUtils.
                        makeRPutRequest(socket, node, key, value);

                // Unless timed out, delete hint from storage
                if (reply != null) {
                    hintStorage.removeHintForNode(id, key);
                    hintsSent++;
                }
            }

            if (hintsSent > 0)
                System.out.println("[Sent " + hintsSent + " hints to " + node.getId() + "]");
        }
    }
}
