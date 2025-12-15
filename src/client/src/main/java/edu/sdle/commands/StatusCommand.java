package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.comms.CommsManager;
import edu.sdle.storage.Storage;

public class StatusCommand extends Command {
    private final CommsManager comms;

    public StatusCommand(CommsManager comms) {
        super("/status", "Check proxy connectivity status");
        this.comms = comms;
    }

    @Override
    public void execute(String commandStr, Storage storage, User user) {
        var status = comms.testProxyConnectivity();

        System.out.println("Proxy connectivity status:");
        int reachable = 0;

        for (var entry : status.entrySet()) {
            String proxy = entry.getKey();
            boolean ok = entry.getValue();
            String mark = ok ? "[OK]" : "[DOWN]";
            System.out.println("  " + mark + " " + proxy);
            if (ok) reachable++;
        }

        System.out.printf("Reachable proxies: %d/%d\n", reachable, status.size());

        if (reachable == 0) {
            System.out.println("Warning: No proxies reachable. Check if proxies are running.");
        }
    }
}
