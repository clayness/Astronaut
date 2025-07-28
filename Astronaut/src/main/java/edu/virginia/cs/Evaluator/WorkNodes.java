package edu.virginia.cs.Evaluator;

import edu.virginia.cs.AppConfig;

import java.util.ArrayList;

/**
 * Created by Tang on 11/24/14.
 */
public class WorkNodes {
    private static final ArrayList<Node> nodes = new ArrayList<>();

    public WorkNodes() {
        for (String name : AppConfig.getSparkSlaves()) {
            Node n = new Node();
            n.setAddr(name);
            nodes.add(n);
            System.out.println(name);
        }
    }

    public static Node getAnIdleNode() {
        for (Node n : nodes) {
            if (n.getStatus() == Node.NodeStatus.IDLE) {
                return n;
            }
        }
        return null;
    }

}
