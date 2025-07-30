package edu.virginia.cs.Synthesizer;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Evaluator {

    final Module root;
    final A4Solution ans;
    protected String result;
    protected final List<String> resultsArray = new ArrayList<>();

    public Evaluator(Module rootElement, A4Solution solution) {
        root = rootElement;
        ans = solution;
    }

    protected List<String> query(String inputQuery) {
        resultsArray.clear();
        Expr e;
        // e.g., inputQuery = "Sensor";
        try {
            e = CompUtil.parseOneExpression_fromString(root, inputQuery);
            result = ans.eval(e).toString();
        } catch (Err err) {
            //noinspection CallToPrintStackTrace
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
