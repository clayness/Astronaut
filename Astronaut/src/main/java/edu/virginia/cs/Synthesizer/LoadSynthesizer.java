package edu.virginia.cs.Synthesizer;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import edu.virginia.cs.AppConfig;
import edu.virginia.cs.Framework.Types.ObjectOfDM;
import edu.virginia.cs.Uniq.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LoadSynthesizer {
    public final HashMap<String, String> globalNegation = new HashMap<>();
    private final Boolean isDebugOn = AppConfig.getDebug();
    public ArrayList<String> ids = new ArrayList<>();
    public HashMap<String, HashMap<String, ArrayList<Pair<String>>>> allInstances = new HashMap<>();
    public int solutionNo = 1;
    boolean isFinished = false;

    public void genObjsHelper(String model, String solutions,
                              ArrayList<String> ids) {
        // call solve()
        // parse one solution
        // add negation to the end of DM
        // go to step (1)
        if (isDebugOn) {
            System.out.println("Generate objects starts....");
        }
        this.ids = ids;
        StringBuilder negation;
        String factName;
        int factNum = 1;
        while (true) {
            try {
                String object = genObjects(model, solutions);
                System.gc();
                if (isFinished) {
                    break;
                }
                // parse the document
                ObjectOfDM oodm = new ObjectOfDM(object);
                allInstances = oodm.parseDocument();
                // add negation to data model
                getNegation();
                PrintStream ps = new PrintStream(new FileOutputStream(model, true));
                factName = "fact_" + factNum;
                factNum++;
                negation = new StringBuilder(System.getProperty("line.separator") + "fact "
                        + factName + " {"
                        + System.getProperty("line.separator"));
                for (Entry<String, String> s_negation : this.globalNegation
                        .entrySet()) {
                    negation.append(s_negation.getKey()).append(System.getProperty("line.separator"));
                }
                negation.append("}");
                ps.print(negation);
                ps.flush();
                ps.close();
                this.globalNegation.clear();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (isDebugOn) {
            System.out.println("Generate objects ends....");
        }
    }

    /**
     * Call legacy code
     */
    public String genObjects(String model, String solutions) {
        String logFile = solutions + File.separator + "log.txt";
        if (!new File(logFile).exists()) {
            try {
                new File(logFile).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String trimmedFilename = model.substring(
                model.lastIndexOf(File.separator) + 1, model.length() - 4);
        String xmlFileNameBase = solutions + File.separator + trimmedFilename;
        Module root = null; // (14:45:08)
        int maxSol = AppConfig.getMaxSolForTest();
        A4Reporter rep = new A4Reporter() {
            @Override
            public void warning(ErrorWarning msg) {
                if (isDebugOn) {
                    System.out.print("Relevance Warning:\n"
                            + (msg.toString().trim()) + "\n\n");
                    System.out.flush();
                }
            }
        };
        try {
            root = CompUtil.parseEverything_fromFile(rep, null, model);
        } catch (Err e1) {
            e1.printStackTrace();
        }

        // Choose some default options for how you want to execute the commands
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J; // .KK;//.MiniSatJNI;
        // //.MiniSatProverJNI;//.SAT4J;
        options.symmetry = AppConfig.getA4ReportSymmetry();
        options.skolemDepth = AppConfig.getA4ReportSkolemDepth();

        ConstList<Command> cmds = root.getAllCommands();
        try {
            for (Command command : cmds) {
                // Execute the command
                A4Solution solution = TranslateAlloyToKodkod.execute_command(
                        rep, root.getAllReachableSigs(), command, options);
                for (ExprVar a : solution.getAllAtoms()) {
                    root.addGlobal(a.label, a);
                }
                for (ExprVar a : solution.getAllSkolems()) {
                    root.addGlobal(a.label, a);
                }

                while (!isFinished) {
                    if (solutionNo > maxSol) {
                        break;
                    }
                    if (solution.satisfiable()) {
                        String xmlFileName = xmlFileNameBase + "_Sol_"
                                + solutionNo + ".xml";
                        solution.writeXML(xmlFileName); // This writes out
                        solutionNo++;
                        return xmlFileName;
                    } else {
                        isFinished = true;
                    }
                }
            }
        } catch (Err e) {
            e.printStackTrace();
        }
        return null;
    }

    public void getNegation() {
        StringBuilder negation = new StringBuilder();
        StringBuilder forGlobalNegation;
        for (Map.Entry<String, HashMap<String, ArrayList<Pair<String>>>> entry : this.allInstances
                .entrySet()) {
            String element = entry.getKey();
            for (Map.Entry<String, ArrayList<Pair<String>>> instance : entry
                    .getValue().entrySet()) {
                forGlobalNegation = new StringBuilder("no o:" + element + " | ");
                negation.append("no o:").append(element).append(" | ");
                ArrayList<Pair<String>> allFields = instance.getValue();
                for (Pair<String> fields : allFields) {
                    String field = fields.getFirst();
                    // check if field is ID or not
                    if (isID(field.split("_")[1])) {
                        String value = fields.getSecond();
                        negation.append("o.").append(field).append("=").append(value).append(" && ");
                        forGlobalNegation.append("o.").append(field).append("=").append(value).append(" && ");
                    }
                }
                forGlobalNegation = new StringBuilder(forGlobalNegation.substring(0,
                        forGlobalNegation.length() - 4));// +
                // System.getProperty("line.separator");
                globalNegation.put(forGlobalNegation.toString(), "");
                negation = new StringBuilder(negation.substring(0, negation.length() - 4)
                        + System.getProperty("line.separator"));
            }
            // String goToTable = getTableNameByElement()
        }
        // negation += "}";

    }

    public boolean isID(String field) {
        for (String s : this.ids) {
            if (s.equals(field))
                return true;
        }
        return false;
    }

}
