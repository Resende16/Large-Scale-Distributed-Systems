package edu.sdle;

import edu.sdle.crdt.AWMap;
import edu.sdle.crdt.PNCounter;

public class Main {
    static void main() {
        AWMap<String, PNCounter> awMap = new AWMap<>();

        PNCounter pn1 = new PNCounter();
        pn1.inc("a");
        pn1.inc("a"); // 2

        PNCounter pn2 = new PNCounter();
        pn2.dec("a");
        pn2.dec("a");
        pn2.dec("a"); // -3

        System.out.println("PNCounter 1 > " +  pn1);
        System.out.println("PNCounter 2 > " +  pn2);

        awMap.put("key1", pn1, "a");
        awMap.put("key1", pn2, "a");

        System.out.println(awMap.get("key1"));
    }
}
