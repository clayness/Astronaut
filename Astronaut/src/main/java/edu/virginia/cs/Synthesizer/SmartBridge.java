package edu.virginia.cs.Synthesizer;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import edu.virginia.cs.AppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

public class SmartBridge {

    protected final boolean isFinished = false;
    PrintStream file = null;
    FileOutputStream Output = null;
    String trimmedFilename;

    // The list of quality equivalence classes on the Pareto optimal frontier.
    final List<MetricValue> solutionsMV = new ArrayList<>();
    // The list of Solutions on the Pareto optimal frontier.
    final List<MetricValue> paretoOptimalSolutions = new ArrayList<>();
    private Integer overallNIC = 0;
    static final boolean storeAllSolutions = AppConfig.getStoreAllSolutions();

    public SmartBridge(String solutionDirectory, String AlloyFile, int maxSol)
            throws Err {

        try {
            String filePath = solutionDirectory + File.separator + "metricsValue.txt";
            System.out.println(filePath);
            File myFile = new File(filePath);
            if (!myFile.exists()) {
                System.out.println("Create new file");
                //noinspection ResultOfMethodCallIgnored
                myFile.createNewFile();
            }
            Output = new FileOutputStream(solutionDirectory
                    + File.separator + "metricsValue.txt");
            file = new PrintStream(Output);
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }

        Module root = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, AlloyFile);
        assert file != null;
        file.println("Computing Satisfying Solutions ...");
        file.println("Current Time: " + now());

        // Choose some default options for how you want to execute the commands
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J;
        options.symmetry = 20;
        options.skolemDepth = 1;

        trimmedFilename = AlloyFile.replace(".als", "");
        StringTokenizer st = new StringTokenizer(trimmedFilename, "\\/");
        String tmp = null;
        while (st.hasMoreTokens()) {
            tmp = st.nextToken();
        }
        String appFileName = tmp;

        assert appFileName != null;
        trimmedFilename = solutionDirectory + "/"
                + appFileName.substring(0, appFileName.length() - 12);

        for (Command command : root.getAllCommands()) {
            A4Solution solution = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, root.getAllReachableSigs(), command, options);
            for (ExprVar a : solution.getAllAtoms()) {
                root.addGlobal(a.label, a);
            }
            for (ExprVar a : solution.getAllSkolems()) {
                root.addGlobal(a.label, a);
            }

            int solutionNo = 1;

            //noinspection LoopConditionNotUpdatedInsideLoop
            while (!isFinished) {
                boolean isDebugOn = AppConfig.getDebug();
                if (solutionNo > maxSol) {
                    file.println("\n-----------------------------------------");
                    file.println("# Eq.Classes: " + solutionsMV.size() + " / "
                            + solutionNo);
                    file.println("Current Time: " + now());
                    break;
                }
                if (solution.satisfiable()) {
                    file.println("-----------------------------------------");
                    file.println("Solution #" + solutionNo
                            + " has been generated.");
                    if (solutionNo % 1000 == 0) {
                        file.println("\n-----------------------------------------");
                        file.println("# Eq.Classes: " + solutionsMV.size()
                                + " / " + solutionNo);
                        file.println("Current Time: " + now());
                    }

                    if (storeAllSolutions) {
                        solution.writeXML(trimmedFilename + "_Sol_" + solutionNo + ".xml");
                    }
                    measureMetric(root, solution, solutionNo);

                } else {
                    if (isDebugOn) {
                        System.out.println("No more Satisfying solutions");
                        System.out
                                .println("\n======================================");
                    }
                    file.println("\n-----------------------------------------");
                    file.println("# Eq.Classes: " + solutionsMV.size() + " / "
                            + solutionNo);
                    file.println("Current Time: " + now());
                    file.println("No more Satisfying solutions");
                    break;
                }
                solution = solution.next();
                solutionNo = solutionNo + 1;
            }
        }
    }

    private void measureMetric(Module root, A4Solution solution,
                               int solutionNo) throws Err {
        MetricValue solutionMV = new MetricValue();

        int overallTATI = 0;
        int overallNCT = 0;
        int overallNCRF = 0;
        int overallANV = 0;
        // Integer overallNIC = 0;

        Evaluator e = new Evaluator(root, solution);

        List<String> classes = e.query("Class");
        String className;

        List<String> classNames = new ArrayList<>();
        for (String currentResult : classes) {
            StringTokenizer innerST = new StringTokenizer(currentResult, "/");
            String innerTmp = "";
            while (innerST.hasMoreTokens()) {
                innerTmp = innerST.nextToken();
            }
            className = innerTmp.replace("$", "");
            // System.out.println("className: " + className);
            classNames.add(className);
        }

        // Measuring TATI - Table Access for Type Identification
        for (String value : classNames) {
            className = value;
            String queryTATI = "#" + className + ".*(~parent).~tAssociate";
            List<String> queryResults = e.query(queryTATI);
            int valueTATI = Integer
                    .parseInt(queryResults.get(0));
            // if (value <0) value = 16+value
            valueTATI = valueTATI < 0 ? 16 + valueTATI : valueTATI;
            overallTATI += valueTATI;
            // System.out.println("TATI(" + className+")= " +valueTATI);
            solutionMV.setTATI_detail(solutionMV.getTATI_detail() + "\nTATI("
                    + className + ")= " + valueTATI);
        }
        // System.out.println("Overall_TATI(solution:" + solutionNo+")= "
        // +overallTATI);

        // Measuring NCT - Number of Corresponding Tables
        for (String string : classNames) {
            className = string;
            String queryNCT = "#" + className + ".~tAssociate.foreignKey +1";
            List<String> queryResults = e.query(queryNCT);
            int valueNCT = Integer.parseInt(queryResults.get(0));
            // if (value <0) value = 16+value
            valueNCT = valueNCT < 0 ? 16 + valueNCT : valueNCT;
            overallNCT += valueNCT;
            // System.out.println("NCT(" + className+")= " +valueNCT);
            solutionMV.setNCT_detail(solutionMV.getNCT_detail() + "\nNCT("
                    + className + ")= " + valueNCT);
        }
        // System.out.println("Overall_NCT(solution:" + solutionNo+")= "
        // +overallNCT);

        // Measuring NCRF - Number of Corresponding Relational Fields
        for (String s : classNames) {
            className = s;
            String queryNCRF = "#(" + className + ".attrSet-" + className
                    + ".id).~fAssociate.~fields";
            List<String> queryResults = e.query(queryNCRF);
            int valueNCRF = Integer
                    .parseInt(queryResults.get(0));
            // if (value <0) value = 16+value
            valueNCRF = valueNCRF < 0 ? 16 + valueNCRF : valueNCRF;
            overallNCRF += valueNCRF;
            // System.out.println("NCRF(" + className+")= " +valueNCRF);
            solutionMV.setNCRF_detail(solutionMV.getNCRF_detail() + "\nNCRF("
                    + className + ")= " + valueNCRF);
        }
        // System.out.println("Overall_NCRF(solution:" + solutionNo+")= "
        // +overallNCRF);

        // Measuring ANV - Additional Null Values
        // ANV(Student) = #Student.attrSet X
        // #(Student.~tAssociate.tAssociate-Student.*(parent))
        for (String name : classNames) {
            className = name;
            String queryANV1 = "#" + className + ".attrSet";
            String queryANV2 = "#(" + className + ".~tAssociate.tAssociate-"
                    + className + ".*(parent))";
            List<String> queryResults1 = e.query(queryANV1);
            List<String> queryResults2 = e.query(queryANV2);
            int value1 = Integer.parseInt(queryResults1.get(0));
            int value2 = Integer.parseInt(queryResults2.get(0));
            // if (value <0) value = 16+value
            value1 = value1 < 0 ? 16 + value1 : value1;
            value2 = value2 < 0 ? 16 + value2 : value2;
            int valueANV = value1 * value2;
            overallANV += valueANV;
            // System.out.println("ANV(" + className+")= " +valueANV);
            solutionMV.setANV_detail(solutionMV.getANV_detail() + "\nANV("
                    + className + ")= " + valueANV);
        }

        // Measuring NFK - Number of Foreign Keys
        String queryNFK = "#foreignKey";
        List<String> queryResults = e.query(queryNFK);
        int valueNFK = Integer.parseInt(queryResults.get(0));
        valueNFK = valueNFK < 0 ? 16 + valueNFK : valueNFK;
        solutionMV.setNFK(valueNFK);

        solutionMV.setTATI(overallTATI);
        solutionMV.setNCT(overallNCT);
        solutionMV.setNCRF(overallNCRF);
        solutionMV.setANV(overallANV);

        file.println("Eq.Class #" + eqClass(solutionMV));

        if (!contains(solutionMV)) {
            measureNIC(e, classNames);
            solutionMV.setNIC(overallNIC);

            boolean isParetoOptimal = true;
            for (MetricValue instance : solutionsMV) {
                if (solutionMV.getTATI() >= instance.getTATI()
                        && solutionMV.getNCT() >= instance.getNCT()
                        && solutionMV.getNCRF() >= instance.getNCRF()
                        && solutionMV.getANV() >= instance.getANV()
                        && solutionMV.getNFK() >= instance.getNFK()
                        && solutionMV.getNIC() >= instance.getNIC()) {
                    isParetoOptimal = false;
                    break;
                }
            }
            if (isParetoOptimal) {
                paretoOptimalSolutions.add(solutionMV);
                solutionsMV.add(solutionMV);

                // Chong: changed by Chong
                // if storeAllSolution is on, then all solutions will be write
                // there is no need to write again
                if (!storeAllSolutions) {
                    // changed to write all solutions before calling this method
                    solution.writeXML(trimmedFilename + "_Sol_" + solutionNo + ".xml");
                }
                file.println(solutionMV.getTATI_detail());
                file.println("Overall_TATI(solution:" + solutionNo + ")= "
                        + overallTATI);
                file.println(solutionMV.getNCT_detail());
                file.println("Overall_NCT(solution:" + solutionNo + ")= "
                        + overallNCT);
                file.println(solutionMV.getNCRF_detail());
                file.println("Overall_NCRF(solution:" + solutionNo + ")= "
                        + overallNCRF);
                file.println(solutionMV.getANV_detail());
                file.println("Overall_ANV(solution:" + solutionNo + ")= "
                        + overallANV);
                // file.println(solutionMV.getNIC_detail());
                file.println("Overall_NIC(solution:" + solutionNo + ")= "
                        + overallNIC);
                file.println("Overall_NFK(solution:" + solutionNo + ")= "
                        + valueNFK);
                file.println("Eq.Classes: " + solutionsMV.size() + " / "
                        + solutionNo);
                file.println("-----------------------------------------");

                // System.out.println(solutionMV.getTATI_detail());
                // System.out.println("Overall_TATI(solution:" +
                // solutionNo+")= "
                // +overallTATI);
                // System.out.println(solutionMV.getNCT_detail());
                // System.out.println("Overall_NCT(solution:" + solutionNo+")= "
                // +overallNCT);
                // System.out.println(solutionMV.getNCRF_detail());
                // System.out.println("Overall_NCRF(solution:" +
                // solutionNo+")= "
                // +overallNCRF);
                // System.out.println(solutionMV.getANV_detail());
                // System.out.println("Overall_ANV(solution:" + solutionNo+")= "
                // +overallANV);
                // // System.out.println(solutionMV.getNIC_detail());
                // // System.out.println("Overall_NIC(solution:" +
                // solutionNo+")= "
                // +overallNIC);
                // System.out.println("Overall_NFK(solution:" + solutionNo+")= "
                // +valueNFK);
                // System.out.println("overall_NIC(solution:" + solutionNo+")= "
                // +overallNIC);
                // System.out.println("Eq.Classes: " +solutionsMV.size() +" / "
                // +
                // solutionNo);
                // // System.out.println("# ParetoOptimalSolutions: "
                // +paretoOptimalSolutions.size());
            }
        }
    }

    private void measureNIC(Evaluator e, List<String> classNames) {
        String className;// Measuring NIC - Number of Involved Classes
        List<String> visitedTables = new ArrayList<>();
        overallNIC = 0;

        for (String name : classNames) {
            className = name;
            String queryNIC1 = className + ".~tAssociate";
            List<String> queryResults1 = e.query(queryNIC1);
            if (!queryResults1.isEmpty()
                    && !visitedTables.contains(queryResults1.get(0))) {
                visitedTables.add(queryResults1.get(0));
                String queryNIC2 = "#(" + className
                        + ".~tAssociate.fields.fAssociate.~attrSet)";
                List<String> queryResults2 = e.query(queryNIC2);
                int value1 = Integer.parseInt(queryResults2.get(0));
                value1 = value1 < 0 ? 16 + value1 : value1;
                overallNIC += value1;
            }
        }
    }

    private boolean contains(MetricValue solutionMV) {
        boolean contains = false;
        for (MetricValue c : solutionsMV) {
            if (c.equals(solutionMV)) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    private int eqClass(MetricValue solutionMV) {
        int eqClassNo = 0;
        boolean found = false;
        for (MetricValue c : solutionsMV) {
            eqClassNo++;
            if (c.equals(solutionMV)) {
                found = true;
                break;
            }
        }
        if (!found)
            eqClassNo++;
        return eqClassNo;
    }

    public static String now() {
        String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss.SSS";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

}
