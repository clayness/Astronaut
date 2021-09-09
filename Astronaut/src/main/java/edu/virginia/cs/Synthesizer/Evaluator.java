package edu.virginia.cs.Synthesizer;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class Evaluator {

    // MinA4Solution ans = null;
    protected String result;
    protected final ArrayList<String> resultsArray = new ArrayList<>();
    Module root = null;
    A4Solution ans = null;

    public Evaluator() {
    }

    public Evaluator(Module rootElement, A4Solution solution) {
        root = rootElement;
        ans = solution;
    }

    // public Evaluator(Module rootElement, MinA4Solution solution) {
    // root = rootElement;
    // ans = solution;
    // }

    /**
     * This object performs expression evaluation.
     *
     * @param args AlloyFile Query
     */

    public static void main(String[] args) {

        // System.out.println("args[0]: " + args[0]);
        // System.out.println("args[1]: " + args[1]);

        Evaluator e = new Evaluator();
        e.solve(args[0]);
        // e.query(args[1]);
        // System.out.println("results: " + e.query(args[1]).toString());
        // System.out.println("results: " + e.query("state.ran").toString());

    }

    protected void solve(String AlloyFile) {
        // Alloy4 sends diagnostic messages and progress reports to the
        // A4Reporter.
        // By default, the A4Reporter ignores all these events (but you can
        // extend the A4Reporter to display the event for the user)
        A4Reporter rep = new A4Reporter() {
            // For example, here we choose to display each "warning" by printing
            // it to System.out
            @Override
            public void warning(ErrorWarning msg) {
                System.out.print("Relevance Warning:\n"
                        + (msg.toString().trim()) + "\n\n");
                System.out.flush();
            }
        };

        try {
            root = CompUtil.parseEverything_fromFile(rep, null, AlloyFile);
        } catch (Err err) {

            err.printStackTrace();
        }
        // to replace AA with Aluminum
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J;// .KK;//.MiniSatJNI;
        // //.MiniSatProverJNI;//.SAT4J;

        options.symmetry = 20;
        options.skolemDepth = 1;

        // MinA4Options options = new MinA4Options();
        // options.solver = MinA4Options.SatSolver.SAT4J;

        for (Command command : root.getAllCommands()) {
            // Execute the command
            // System.out.println("============ Command "+command+": ============");
            System.out.println("Synthesizing Code ...");
            try {
                ans = TranslateAlloyToKodkod.execute_command(rep,
                        root.getAllReachableSigs(), command, options);
                // ans = MinTranslateAlloyToKodkod.execute_command(rep,
                // root.getAllReachableSigs(), command, options);
            } catch (Err err) {
                err.printStackTrace();
            }
            for (ExprVar a : ans.getAllAtoms()) {
                root.addGlobal(a.label, a);
            }
            for (ExprVar a : ans.getAllSkolems()) {
                root.addGlobal(a.label, a);
            }
        }
    }

    protected ArrayList<String> query(String inputQuery) {
        resultsArray.clear();
        Expr e;
        // e.g., inputQuery = "Sensor";
        try {
            e = CompUtil.parseOneExpression_fromString(root, inputQuery);
            result = ans.eval(e).toString();
        } catch (Err err) {
            err.printStackTrace();
        }
        // System.out.println("new result: " + result);
        // e.g., MIDAS_SCC/IntrusionAlarmAnalyzer$0
        StringTokenizer st = new StringTokenizer(result, "{,} ");
        String tmp;
        while (st.hasMoreTokens()) {
            tmp = st.nextToken();
            resultsArray.add(tmp);
        }
        return resultsArray;

    }

}
