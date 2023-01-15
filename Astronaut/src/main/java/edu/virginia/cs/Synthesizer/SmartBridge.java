package edu.virginia.cs.Synthesizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Scanner;
import java.util.StringTokenizer;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4compiler.ast.Command;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprVar;
import edu.mit.csail.sdg.alloy4compiler.parser.CompUtil;
import edu.mit.csail.sdg.alloy4compiler.parser.Module;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Options;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Solution;
import edu.mit.csail.sdg.alloy4compiler.translator.TranslateAlloyToKodkod;
//import edu.mit.csail.sdg.alloy4compiler.parser.Module;
//import edu.virginia.cs.AppConfig;

public class SmartBridge {
	private Boolean isDebugOn = AppConfig.getDebug();

	protected boolean isFinished = false;
	// static String applicationName;
	// static String appType = "";
	// static String archStyle = "";
	static String workspace = ".";// UI.workspace;
	PrintStream file = null;
	FileOutputStream Output = null;
	String trimmedFilename = "";

	// The list of quality equivalence classes on the Pareto optimal frontier.
	ArrayList<MetricValue> solutionsMV = new ArrayList<MetricValue>();
	// The list of Solutions on the Pareto optimal frontier.
	ArrayList<MetricValue> paretoOptimalSolutions = new ArrayList<MetricValue>();
	private Integer overallNIC = 0;
	static boolean storeAllSolutions = AppConfig.getStoreAllSolutions();

	/*
	 * args[0]: solution folder args[1]: alloy OM args[2]: max solution number
	 * parameter args[3] (optional): store all solution? default value: false
	 */
	public static void main(String args[]) {

		workspace = args[0];
		String alloyFile = args[1];
		int maxSolNoParam = 1000000; // Integer.parseInt(args[2]);
		if (args.length > 2) {
			maxSolNoParam = Integer.parseInt(args[2]);
		}
		//if ((args.length > 3) && (args[3].equalsIgnoreCase("all"))) {
		//	storeAllSolutions = true;
		//}
		// if(args.length > 2)
		// workspace = args[2];
		// int maxSolNoParam = 1;
		String solutionDirectory = workspace; // + "\\Solutions";
		// String mergedFile = workspace + "\\"+ appType + "\\" + appDesc + "_"
		// + archStyle + ".als";

		try {
			new SmartBridge(solutionDirectory, alloyFile, maxSolNoParam);
		} catch (Err err) {
			err.printStackTrace();
		}
	}

	// public SmartBridge(String solutionDirectory, String filename, String
	// appDescp, String appTypep, String archStylep, int maxSol) throws Err {

	public SmartBridge(String solutionDirectory, String AlloyFile, int maxSol)
			throws Err {

		try {
			String filePath = solutionDirectory + File.separator+"metricsValue.txt";
			System.out.println(filePath);
			File myFile = new File(filePath);
			if(!myFile.exists()){
				System.out.println("Create new file");
				myFile.createNewFile();
			}
			Output = new FileOutputStream(solutionDirectory
					+ File.separator+ "metricsValue.txt");
			file = new PrintStream(Output);
		} catch (IOException e) {
			e.printStackTrace(); // To change body of catch statement use File |
									// Settings | File Templates.
		}

		// Alloy4 sends diagnostic messages and progress reports to the
		// A4Reporter.
		// By default, the A4Reporter ignores all these events (but you can
		// extend the A4Reporter to display the event for the user)
		A4Reporter rep = new A4Reporter() {
			// For example, here we choose to display each "warning" by printing
			// it to System.out
			@Override
			public void warning(ErrorWarning msg) {
				if (isDebugOn) {
					System.out.print("Relevance Warning:\n"
							+ (msg.toString().trim()) + "\n\n");
					System.out.flush();
				}
			}
		};

		Module root = null;
		root = CompUtil.parseEverything_fromFile(rep, null, AlloyFile);

		// Parse+typecheck the model
		// System.out.println("=========== Parsing+Typechecking "+filename+" =============");
		if (isDebugOn) {
			System.out.println("Computing Satisfying Solutions ...");
			System.out.println("Current Time: " + now());
		}
		file.println("Computing Satisfying Solutions ...");
		file.println("Current Time: " + now());

		// Choose some default options for how you want to execute the commands
		A4Options options = new A4Options();
		options.solver = A4Options.SatSolver.SAT4J; // .KK;//.MiniSatJNI;
		// .MiniSatProverJNI;//.SAT4J;
		options.symmetry = 20;
		options.skolemDepth = 1;

		// to replace AA with Aluminum
		// MinA4Options options = new MinA4Options();
		// options.solver = MinA4Options.SatSolver.SAT4J;

		// String trimmedFilename = AlloyFile.replace(".als", "");

		trimmedFilename = AlloyFile.replace(".als", "");
		// System.out.println(sensorName);
		StringTokenizer st = new StringTokenizer(trimmedFilename, "\\/");
		String tmp = null;
		while (st.hasMoreTokens()) {
			tmp = st.nextToken();
		}
		String appFileName = tmp;

		trimmedFilename = solutionDirectory + "/"
				+ appFileName.substring(0, appFileName.length() - 12);

		for (Command command : root.getAllCommands()) {
			// Execute the command
			// System.out.println("============ Command "+command+": ============");
			A4Solution solution = TranslateAlloyToKodkod.execute_command(rep,
					root.getAllReachableSigs(), command, options);
			for (ExprVar a : solution.getAllAtoms()) {
				root.addGlobal(a.label, a);
			}
			for (ExprVar a : solution.getAllSkolems()) {
				root.addGlobal(a.label, a);
			}

			// to replace AA with Aluminum
			// MinA4Solution solution =
			// MinTranslateAlloyToKodkod.execute_command(
			// rep, root.getAllReachableSigs(), command, options);

			int solutionNo = 1;
			// if (solution.satisfiable()) {
			// //
			// System.out.println("-----------------------------------------");
			// System.out.println("Solution #" + solutionNo +
			// " has been generated.");
			//
			// // moved to the measureMetric
			// solution.writeXML(trimmedFilename + "_Sol_" + solutionNo +
			// ".xml");
			//
			//
			// measureMetric(root, solution, solutionNo);
			//
			// } else {
			// System.out.println("No more Satisfying solutions");
			// System.out.println("\n-----------------------------------------");
			// System.out.println("# Eq.Classes / # overall solutions : "
			// +solutionsMV.size() +" / " + --solutionNo);
			// System.out.println("Current Time: "+now());
			// file.println("\n-----------------------------------------");
			// file.println("# Eq.Classes / # overall solutions : "
			// +solutionsMV.size() +" / " + solutionNo);
			// file.println("Current Time: "+now());
			// file.println("No more Satisfying solutions");
			// break;
			// }
			boolean isNewSolution;

			while (!isFinished) {
				if (solutionNo > maxSol) {
					if (isDebugOn) {
//						System.out
//								.println("\n-----------------------------------------");
//						System.out.println("# Eq.Classes: "
//								+ solutionsMV.size() + " / " + --solutionNo);
//						System.out.println("Current Time: " + now());
					}
					file.println("\n-----------------------------------------");
					file.println("# Eq.Classes: " + solutionsMV.size() + " / "
							+ solutionNo);
					file.println("Current Time: " + now());
					break;
				}
				if (solution.satisfiable()) {
					if (isDebugOn) {
//						System.out
//								.println("-----------------------------------------");
//						System.out.println("Solution #" + solutionNo
//								+ " has been generated.");
					}
					file.println("-----------------------------------------");
					file.println("Solution #" + solutionNo
							+ " has been generated.");
					if (solutionNo % 1000 == 0) {
						file.println("\n-----------------------------------------");
						file.println("# Eq.Classes: " + solutionsMV.size()
								+ " / " + solutionNo);
						file.println("Current Time: " + now());
					}

					if(storeAllSolutions){
						solution.writeXML(trimmedFilename + "_Sol_" + solutionNo + ".xml");
					}
					isNewSolution = measureMetric(root, solution, solutionNo);

//					if (isNewSolution && storeAllSolutions) {
//						solution.writeXML(trimmedFilename + "_Sol_"
//								+ solutionNo + ".xml"); // This writes out
//														// "answer_0.xml",
//														// "answer_1.xml"...
//
//					}

				} else {
					if (isDebugOn) {
						System.out.println("No more Satisfying solutions");
						System.out
								.println("\n======================================");
						System.out.println("# Eq.Classes: "
								+ solutionsMV.size() + " / " + --solutionNo);
						System.out.println("Current Time: " + now());
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

	private boolean measureMetric(Module root, A4Solution solution,
			int solutionNo) throws Err {
		// private boolean measureMetric(Module root, MinA4Solution solution,
		// int solutionNo) throws Err {
		boolean isNewSolution = false;
		MetricValue solutionMV = new MetricValue(solutionNo);

		Integer overallTATI = 0;
		Integer overallNCT = 0;
		Integer overallNCRF = 0;
		Integer overallANV = 0;
		Integer overallNIC = 0;

		Evaluator e = new Evaluator(root, solution);

		ArrayList classes = e.query("Class");
		String className = "";

		ArrayList<String> classNames = new ArrayList<String>();
		for (Iterator<String> resultIterator = classes.iterator(); resultIterator
				.hasNext();) {
			String currentResult = resultIterator.next();
			StringTokenizer innerST = new StringTokenizer(currentResult, "/");
			String innerTmp = "";
			while (innerST.hasMoreTokens()) {
				innerTmp = innerST.nextToken();
			}
			className = innerTmp.replace("$", "");
			System.out.println("className: " + className);
			classNames.add(className);
		}

		// Measuring TATI - Table Access for Type Identification
		for (Iterator<String> resultIterator = classNames.iterator(); resultIterator
				.hasNext();) {
			className = resultIterator.next();
			String queryTATI = "#" + className + ".*(~parent).~tAssociate";
			ArrayList queryResults = e.query(queryTATI);
			Integer valueTATI = Integer
					.parseInt(queryResults.get(0).toString());
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
		for (Iterator<String> resultIterator = classNames.iterator(); resultIterator
				.hasNext();) {
			className = resultIterator.next();
			String queryNCT = "#" + className + ".~tAssociate.foreignKey +1";
			ArrayList queryResults = e.query(queryNCT);
			Integer valueNCT = Integer.parseInt(queryResults.get(0).toString());
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
		for (Iterator<String> resultIterator = classNames.iterator(); resultIterator
				.hasNext();) {
			className = resultIterator.next();
			String queryNCRF = "#(" + className + ".attrSet-" + className
					+ ".id).~fAssociate.~fields";
			ArrayList queryResults = e.query(queryNCRF);
			Integer valueNCRF = Integer
					.parseInt(queryResults.get(0).toString());
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
		for (Iterator<String> resultIterator = classNames.iterator(); resultIterator
				.hasNext();) {
			className = resultIterator.next();
			String queryANV1 = "#" + className + ".attrSet";
			String queryANV2 = "#(" + className + ".~tAssociate.tAssociate-"
					+ className + ".*(parent))";
			ArrayList queryResults1 = e.query(queryANV1);
			ArrayList queryResults2 = e.query(queryANV2);
			Integer value1 = Integer.parseInt(queryResults1.get(0).toString());
			Integer value2 = Integer.parseInt(queryResults2.get(0).toString());
			// if (value <0) value = 16+value
			value1 = value1 < 0 ? 16 + value1 : value1;
			value2 = value2 < 0 ? 16 + value2 : value2;
			// Integer valueANV =
			// Integer.parseInt(queryResults1.get(0).toString()) *
			// Integer.parseInt(queryResults2.get(0).toString());
			Integer valueANV = value1 * value2;
			overallANV += valueANV;
			// System.out.println("ANV(" + className+")= " +valueANV);
			solutionMV.setANV_detail(solutionMV.getANV_detail() + "\nANV("
					+ className + ")= " + valueANV);
		}

		ArrayList<String> tables = new ArrayList<String>();
		tables.clear();
		tables.addAll(e.query("Table"));
		String tableName = "";
		overallNIC = 0;

		//Measuring NIC - Number of Involved Classes
		for (Iterator<String> resultIterator = tables.iterator();
		resultIterator.hasNext();) {
		tableName = resultIterator.next();
		String queryNIC = "#("+tableName+").fields.fAssociate.~attrSet";
		ArrayList queryResults = e.query(queryNIC);
		//
		Integer valueNIC = 0;
		if(queryResults.size() == 1)
		valueNIC = Integer.parseInt(queryResults.get(0).toString());
		//
		overallNIC += valueNIC;
		solutionMV.setNIC_detail(solutionMV.getNIC_detail() + "\nNIC(" +
		tableName + ")= " + valueNIC);
		 }
		 file.println("overall_NIC(solution:" + solutionNo+")= "
		+overallNIC);
		System.out.println("overall_NIC(solution:" + solutionNo+")= "
		+overallNIC);

		// Measuring NFK - Number of Foreign Keys
		String queryNFK = "#foreignKey";
		ArrayList queryResults = e.query(queryNFK);
		Integer valueNFK = Integer.parseInt(queryResults.get(0).toString());
		// if (value <0) value = 16+value
		valueNFK = valueNFK < 0 ? 16 + valueNFK : valueNFK;
		solutionMV.setNFK(valueNFK);

		solutionMV.setTATI(overallTATI);
		solutionMV.setNCT(overallNCT);
		solutionMV.setNCRF(overallNCRF);
		solutionMV.setANV(overallANV);
		// solutionMV.setNIC(overallNIC);

		file.println("Eq.Class #" + eqClass(solutionMV));
		if (isDebugOn) {
			System.out.println("Eq.Class #" + eqClass(solutionMV));		}

		if (!contains(solutionMV)) {
			 int eqClassNo = eqClass(solutionMV);
			 if (eqClassNo==0) {
			measureNIC(e, classNames);
			solutionMV.setNIC(overallNIC);

			boolean isParetoOptimal = true;
			Iterator<MetricValue> resultIterator = solutionsMV.iterator();
			while (resultIterator.hasNext()) {
				MetricValue instance = resultIterator.next();
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
				file.println("Solution #" + solutionNo +
				" is a pareto Optimal Solution.");
				System.out.println("Solution #" + solutionNo +
				" is a pareto Optimal Solution.");
				solutionsMV.add(solutionMV);
				isNewSolution = true;
				
				// Chong: changed by Chong
				// if storeAllSolution is on, then all solutions will be write
				// there is no need to write again
				
				//Rashed : changed by Rashed 
				//turning on storeAllSolution
				
				if(storeAllSolutions){
					// changed to write all solutions before calling this method
					solution.writeXML(trimmedFilename + "_Sol_" + solutionNo + ".xml"); 
				}
				System.out.println("-----------------------------------------");
				file.println("Solution #" + solutionNo +
				" has been generated.");
				file.println("Current Time: "+now());
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
				file.println("# ParetoOptimalSolutions: "
				+paretoOptimalSolutions.size());
				file.println("-----------------------------------------\n");
				if (isDebugOn) {
					System.out.println("Current Time: " + now());
				}
				file.println("-----------------------------------------");

				System.out.println(solutionMV.getTATI_detail());
				System.out.println("Overall_TATI(solution:" +
				solutionNo+")= "
				+overallTATI);
				System.out.println(solutionMV.getNCT_detail());
				System.out.println("Overall_NCT(solution:" + solutionNo+")= "
				+overallNCT);
				System.out.println(solutionMV.getNCRF_detail());
				System.out.println("Overall_NCRF(solution:" +
				solutionNo+")= "
				+overallNCRF);
				System.out.println(solutionMV.getANV_detail());
				System.out.println("Overall_ANV(solution:" + solutionNo+")= "
				+overallANV);
				System.out.println(solutionMV.getNIC_detail());
				System.out.println("Overall_NIC(solution:" +
				solutionNo+")= "
				+overallNIC);
				System.out.println("Overall_NFK(solution:" + solutionNo+")= "
				+valueNFK);
				System.out.println("overall_NIC(solution:" + solutionNo+")= "
				+overallNIC);
				System.out.println("Eq.Classes: " +solutionsMV.size() +" / "
				+
				solutionNo);
				System.out.println("# ParetoOptimalSolutions: "
				+paretoOptimalSolutions.size());
				System.out.println("Current Time: "+now());
				System.out.println("-----------------------------------------");
			}
		}
		return isNewSolution;
	}

	private void measureNIC(Evaluator e, ArrayList<String> classNames) {
		String className;// Measuring NIC - Number of Involved Classes
		ArrayList<String> visitedTables = new ArrayList<String>();
		visitedTables.clear();
		overallNIC = 0;

		for (Iterator<String> resultIterator = classNames.iterator(); resultIterator
				.hasNext();) {
			className = resultIterator.next();
			String queryNIC1 = className + ".~tAssociate";
			ArrayList queryResults1 = e.query(queryNIC1);
			if (queryResults1.size() > 0
					&& !visitedTables.contains(queryResults1.get(0).toString())) {
				visitedTables.add(queryResults1.get(0).toString());
				String queryNIC2 = "#(" + className
						+ ".~tAssociate.fields.fAssociate.~attrSet)";
				ArrayList queryResults2 = e.query(queryNIC2);
				Integer value1 = Integer.parseInt(queryResults2.get(0)
						.toString());
				value1 = value1 < 0 ? 16 + value1 : value1;
				overallNIC += value1;
			}
		}
	}

	private static boolean pressKey(Scanner kb, boolean pressed) {
		String entered;
		entered = "";
		entered = kb.next();
		if ((entered.equals("E")) || entered.equals("e")) {
			pressed = true;
		} else {
			if (AppConfig.getDebug()) {
				System.out.printf("Finding next Solution ...\n");
			}
		}
		return pressed;
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
