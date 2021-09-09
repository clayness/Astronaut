package edu.virginia.cs.Synthesizer;

import edu.mit.csail.sdg.alloy4.Err;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Synthesizer implements Serializable {

    static String workspace = ".";// UI.workspace;

    static boolean storeAllSolutions = false;

    public static void main(String[] args) {
        // suppose we already get the solutions of AlloyOM
        // then we need to parse the solutions to get the data structure and the
        // tAssociate
        // after get the information of tAssociation, we can parse the AlloyOM
        // to get the Alloy Instance Model
        // the execute the Alloy Instance Model
        // after each solution of AIM comes out, we parse it to different data
        // schema, and then go to the next one

        // Chong: we need to generate the solution first
        workspace = args[0];
        int maxSolNoParam = 1000000; // Integer.parseInt(args[2]);
        if (args.length > 2) {
            maxSolNoParam = Integer.parseInt(args[2]);
        }
        if ((args.length > 3) && (args[3].equalsIgnoreCase("all"))) {
            storeAllSolutions = true;
        }
        // if(args.length > 2)
        // workspace = args[2];
        // int maxSolNoParam = 1;
        String solutionDirectory = workspace; // + "\\Solutions";
        // String mergedFile = workspace + "\\"+ appType + "\\" + appDesc + "_"
        // + archStyle + ".als";

        // delete all files and folders in Solution Folder
        File f = new File(solutionDirectory);
        if (!f.exists()) {
            f.mkdir();
        }
        solutionDirectory = f.getAbsolutePath();

        File f1 = new File(args[1]);
        String om = f1.getAbsolutePath();

//		solutionDirectory = solutionDirectory
//				+ File.separator
//				+ om.substring(om.lastIndexOf(File.separator) + 1,
//						om.lastIndexOf("."));
        File f2 = new File(solutionDirectory);
        delete(f2);
        if (!f2.exists()) {
            f2.mkdir();
        }

        try {
            // get mapping_run file first
            String runFile = FileOperation.getMappingRun(om);
            new SmartBridge(solutionDirectory, runFile, maxSolNoParam);
        } catch (Err err) {
            err.printStackTrace();
        }

        // (1) parse the AlloyOM solutions to get the data structure and the
        // tAssociation
        String folderOfAlloyOMSols = solutionDirectory;
        File file = new File(folderOfAlloyOMSols);
        if (!file.isAbsolute()) {
            folderOfAlloyOMSols = file.getAbsolutePath();
        }
        // String folderOfAlloyOMSols = "C:\\Users\\ct4ew\\Desktop\\CSOS\\";
        HashMap<String, HashMap<String, ArrayList<CodeNamePair>>> schemas = new HashMap<>();
        HashMap<String, ArrayList<CodeNamePair>> parents = new HashMap<>();
        HashMap<String, ArrayList<CodeNamePair>> reverseTAss = new HashMap<>();
        HashMap<String, ArrayList<CodeNamePair>> foreignKeys = new HashMap<>();
        HashMap<String, HashMap<String, CodeNamePair>> association = new HashMap<>();
        HashMap<String, ArrayList<CodeNamePair>> primaryKeys = new HashMap<>();
        HashMap<String, ArrayList<CodeNamePair>> fields = new HashMap<>();
        HashMap<String, ArrayList<CodeNamePair>> fieldsTable = new HashMap<>();
        HashMap<String, ArrayList<String>> allFields = new HashMap<>();
        HashMap<String, ArrayList<CodeNamePair>> fieldType = new HashMap<>();

        String alloyOM = args[1];

        file = new File(alloyOM);
        if (!file.isAbsolute()) {
            alloyOM = file.getAbsolutePath();
        }
        // String alloyInstanceModel =
        // "C:\\Users\\ct4ew\\Desktop\\CSOS\\decider_dm.als";
        // String arg3 = args[2];
        String pattern = Pattern.quote(System.getProperty("file.separator"));
        String[] paths = alloyOM.split(pattern);
        String OMName = paths[paths.length - 1];
        OMName = OMName.substring(0, OMName.length() - 4) + "_dm.als";
        StringBuilder alloyInstanceModel = new StringBuilder();
        for (int i = 0; i < paths.length - 1; i++) {
            alloyInstanceModel.append(paths[i]).append(File.separator);
        }
        alloyInstanceModel.append(OMName);
        // String alloyInstanceModel = args[2];
        // String alloyInstanceModel =
        // "C:\\Users\\ct4ew\\Desktop\\CSOS\\csos_dm.als";
        int intScope = 6; // default
        if (args.length >= 3) {
            intScope = Integer.parseInt(args[2]);
        }
        AlloyOMToAlloyDM aotad = new AlloyOMToAlloyDM();
        aotad.run(alloyOM, alloyInstanceModel.toString(), intScope);
        ArrayList<String> ids = aotad.getIDs();
        ArrayList<String> associations = aotad.getAss();
        HashMap<String, String> typeList = aotad.getTypeList();
        ArrayList<Sig> sigs = aotad.getSigs();

        File dir = new File(folderOfAlloyOMSols);
        for (File singleFile : dir.listFiles()) {
            // Do something with child
            if (singleFile.getName().contains("xml")) {
                String fileName = singleFile.getPath();
                String schemaName = singleFile.getName();
                schemaName = schemaName.substring(0, schemaName.length() - 4);
                String dbSchemaFile = folderOfAlloyOMSols + File.separator
                        + schemaName + ".sql";
                ORMParser parser = new ORMParser(fileName, dbSchemaFile, sigs);
                parser.createSchemas();

                schemas.put(dbSchemaFile, parser.getDataSchemas());
                parents.put(dbSchemaFile, parser.getParents());
                reverseTAss.put(dbSchemaFile, parser.getReverseTAssociate());
                foreignKeys.put(dbSchemaFile, parser.getForeignKey());
                association.put(dbSchemaFile, parser.getAssociation());
                primaryKeys.put(dbSchemaFile, parser.getPrimaryKeys());
                fields.put(dbSchemaFile, parser.getFields());
                allFields.put(dbSchemaFile, parser.getallFields());
                fieldsTable.put(dbSchemaFile, parser.getFieldsTable());
                fieldType.put(dbSchemaFile, parser.getFieldType());
            }
        }

        // (2) parse the AlloyOM to get Alloy Instance Model
        SolveAlloyDM solver = new SolveAlloyDM(schemas, parents, reverseTAss,
                foreignKeys, association, primaryKeys, fields, allFields,
                fieldsTable, fieldType, ids, associations, intScope, typeList,
                sigs);
        for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
                .entrySet()) {
            String dbScheme = entry.getKey();
            System.out.print("NCT of " + dbScheme + ": ");
            System.out.println(solver.getNCTSum(dbScheme));
            System.out.println("-------------------------------");
        }
        for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
                .entrySet()) {
            String dbScheme = entry.getKey();
            System.out.print("TATI of " + dbScheme + ": ");
            System.out.println(solver.getTATISum(dbScheme));
            System.out.println("-------------------------------");
        }

        int range = Integer.parseInt(args[4]);
        /*
          Chong: For qualifying exam, I will generate random test cases as
          well as synthesized test cases
         */

        // if (!isRandom) {
//			try {
        /*
          Chong: synthesize test cases first, and then generate random test cases
         */
//			solver.solveWithContinueUpdate(alloyInstanceModel, folderOfAlloyOMSols);

//		} catch (Err err) {
        // err.printStackTrace(); // To change body of catch statement use File
        // // | Settings | File Templates.
        // } catch (FileNotFoundException e) {
        // e.printStackTrace(); // To change body of catch statement use File |
        // // Settings | File Templates.
        // }
        // solver.solveModel(alloyInstanceModel);
        // } else {

        /*
          generate random test cases starts here
         */
        int newRange = 5000;
        long start = System.currentTimeMillis();
        solver.getOutPutOrders(alloyOM);
        int i = 0; // for decider
        // range = 164812;
        SimpleDateFormat sdf;
        Date date_elapsed;
        while (i < range) {
            int low = i;
            int up = i + newRange;
            if (up > range) {
                up = range;
            }
            solver.randomInstanceGenerator(low + 1, up);
            solver.generateInsert();
            // solver.generateUpdate();
            solver.generateSelect1();
            solver.printAllStatements(1);
            long now = System.currentTimeMillis();
            sdf = new SimpleDateFormat("HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            long elapsed = now - start;
            date_elapsed = new Date(elapsed);
            System.out.println(up + " queries are generated within time: "
                    + sdf.format(date_elapsed));
            i = i + newRange;
        }
        // }

    }

    public static void delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                delete(c);
            }
        }
        if (!f.delete()) {
            try {
                throw new FileNotFoundException("Failed to delete file: " + f);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Synthesizer.class.getName()).log(Level.SEVERE, null,
                        ex);
            }
        }
    }
}
