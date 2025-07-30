package edu.virginia.cs.Synthesizer;

import edu.mit.csail.sdg.alloy4.Err;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA. User: ct4ew Date: 7/23/13 Time: 3:20 PM To change
 * this template use File | Settings | File Templates.
 */
public class Synthesizer implements Serializable {

	static String workspace = ".";// UI.workspace;
	PrintStream file = null;
	FileOutputStream output = null;
	String trimmedFilename = "";

	// The list of quality equivalence classes on the Pareto optimal frontier.
	List<MetricValue> solutionsMV = new ArrayList<MetricValue>();
	// The list of Solutions on the Pareto optimal frontier.
	List<MetricValue> paretoOptimalSolutions = new ArrayList<MetricValue>();
	private Integer overallNIC = 0;
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
		String alloyFile = args[1];
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
		Map<String, Map<String, List<CodeNamePair>>> schemas = new HashMap<>();
		Map<String, List<CodeNamePair>> parents = new HashMap<>();
		Map<String, List<CodeNamePair>> reverseTAss = new HashMap<>();
		Map<String, List<CodeNamePair>> foreignKeys = new HashMap<>();
		Map<String, Map<String, CodeNamePair>> association = new HashMap<>();
		Map<String, List<CodeNamePair>> primaryKeys = new HashMap<>();
		Map<String, List<CodeNamePair>> fields = new HashMap<>();
		Map<String, List<CodeNamePair>> fieldsTable = new HashMap<>();
		Map<String, List<String>> allFields = new HashMap<>();
		Map<String, List<CodeNamePair>> fieldType = new HashMap<>();
		boolean isRandom;

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
		String alloyInstanceModel = "";
		for (int i = 0; i < paths.length - 1; i++) {
			alloyInstanceModel += paths[i] + File.separator;
		}
		alloyInstanceModel += OMName;
		// String alloyInstanceModel = args[2];
		// String alloyInstanceModel =
		// "C:\\Users\\ct4ew\\Desktop\\CSOS\\csos_dm.als";
		int intScope = 6; // default
		if (args.length >= 3) {
			intScope = Integer.valueOf(args[2]);
		}
		AlloyOMToAlloyDM aotad = new AlloyOMToAlloyDM();
		aotad.run(alloyOM, alloyInstanceModel, intScope);
		List<String> ids = aotad.getIDs();
		List<String> associations = aotad.getAss();
		Map<String, String> typeList = aotad.getTypeList();
		List<Sig> sigs = aotad.getSigs();

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
		for (Map.Entry<String, Map<String, List<CodeNamePair>>> entry : schemas
				.entrySet()) {
			String dbScheme = entry.getKey();
			System.out.print("NCT of " + dbScheme + ": ");
			System.out.println(solver.getNCTSum(dbScheme));
			System.out.println("-------------------------------");
		}
		for (Map.Entry<String, Map<String, List<CodeNamePair>>> entry : schemas
				.entrySet()) {
			String dbScheme = entry.getKey();
			System.out.print("TATI of " + dbScheme + ": ");
			System.out.println(solver.getTATISum(dbScheme));
			System.out.println("-------------------------------");
		}

		
			isRandom = Boolean.valueOf(args[3]);
			int range = Integer.valueOf(args[4]);
			/**
			 * Chong: For qualifying exam, I will generate random test cases as
			 * well as synthesized test cases
			 */

			// if (!isRandom) {
//			try {
			/**
			 * Chong: synthesize test cases first, and then generate random test cases
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
			
			/**
			 * generate random test cases starts here
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
