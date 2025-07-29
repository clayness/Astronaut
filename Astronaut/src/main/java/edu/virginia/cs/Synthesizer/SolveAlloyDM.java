package edu.virginia.cs.Synthesizer;

/**
 * Created by IntelliJ IDEA.
 * User: ct4ew
 * Date: 7/23/13
 * Time: 8:58 PM
 * To change this template use File | Settings | File Templates.
 */

import edu.mit.csail.sdg.alloy4.A4Reporter;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class SolveAlloyDM {

	private Boolean isDebugOn = AppConfig.getDebug();
	private int intStmts = 0;
	private int intSolutions = 0;
	public HashMap<String, HashMap<String, ArrayList<CodeNamePair>>> allInstances = new HashMap<String, HashMap<String, ArrayList<CodeNamePair>>>();
	/**
	 * Use this method to output the data in inner data structure into sql file
	 */
	HashMap<String, HashMap<String, ArrayList<CodeNamePair>>> schemas = new HashMap<String, HashMap<String, ArrayList<CodeNamePair>>>();
	HashMap<String, ArrayList<CodeNamePair>> parents = new HashMap<String, ArrayList<CodeNamePair>>();
	ArrayList<String> printed = new ArrayList<String>();
	// public HashMap<String, HashMap<String, HashMap<String, String>>>
	// allInsertStmts = new HashMap<String, HashMap<String, HashMap<String,
	// String>>>();
	public HashMap<String, HashMap<String, HashMap<Integer, String>>> allInsertStmts = new HashMap<String, HashMap<String, HashMap<Integer, String>>>();
	public HashMap<String, HashMap<String, HashMap<String, String>>> allUpdateStmts = new HashMap<String, HashMap<String, HashMap<String, String>>>();
	// public HashMap<String, HashMap<String, HashMap<String, String>>>
	// allSelectStmts = new HashMap<String, HashMap<String, HashMap<String,
	// String>>>();
	public HashMap<String, HashMap<String, ArrayList<String>>> allSelectStmts = new HashMap<String, HashMap<String, ArrayList<String>>>();
	// store the FilePrinter of data schemas
	HashMap<String, PrintWriter> insertPrintWriters = new HashMap<String, PrintWriter>();
	// HashMap<String, PrintWriter> insertInstantPrintWriters = new
	// HashMap<String, PrintWriter>();
	HashMap<String, PrintWriter> updatePrintWriters = new HashMap<String, PrintWriter>();
	// HashMap<String, PrintWriter> updateInstantPrintWriters = new
	// HashMap<String, PrintWriter>();
	HashMap<String, PrintWriter> selectPrintWriters = new HashMap<String, PrintWriter>();
	HashMap<String, ArrayList<CodeNamePair>> reverseTAss = new HashMap<String, ArrayList<CodeNamePair>>();
	HashMap<String, ArrayList<CodeNamePair>> foreignKeys = new HashMap<String, ArrayList<CodeNamePair>>();
	HashMap<String, HashMap<String, CodeNamePair>> associations = new HashMap<String, HashMap<String, CodeNamePair>>();
	HashMap<String, ArrayList<CodeNamePair>> primaryKeys = new HashMap<String, ArrayList<CodeNamePair>>();
	// HashMap<String, ArrayList<CodeNamePair>> fields = new
	// HashMap<String, ArrayList<CodeNamePair>>();
	private int intScope = 6;
	HashMap<String, ArrayList<String>> printOrder = new HashMap<String, ArrayList<String>>();
	HashMap<String, HashMap<String, HashSet<String>>> existingID = new HashMap<String, HashMap<String, HashSet<String>>>();
	HashMap<String, ArrayList<String>> allFields = new HashMap<String, ArrayList<String>>();
	HashMap<String, ArrayList<CodeNamePair>> fieldsTable = new HashMap<String, ArrayList<CodeNamePair>>();
	HashMap<String, ArrayList<CodeNamePair>> tableFields = new HashMap<String, ArrayList<CodeNamePair>>();
	HashMap<String, ArrayList<CodeNamePair>> fieldType = new HashMap<String, ArrayList<CodeNamePair>>();
	ArrayList<String> ids = new ArrayList<String>();
	ArrayList<String> assList = new ArrayList<String>();
	HashMap<String, String> typeList = new HashMap<String, String>();
	ArrayList<Sig> sigs = new ArrayList<Sig>();
	HashMap<String, String> globalNegation = new HashMap<String, String>();

	public String insertFile = null;
	public String selectFile = null;

	public SolveAlloyDM(HashMap schemas, HashMap parents, HashMap reverseTAss,
			HashMap foreignKeys, HashMap associations, HashMap primaryKeys,
			HashMap tableFields, HashMap allFields, HashMap fieldsTable,
			HashMap fieldType, ArrayList ids, ArrayList assList, int intScope,
			HashMap<String, String> typeList, ArrayList<Sig> sigs) {
		this.schemas = schemas;
		this.parents = parents;
		this.reverseTAss = reverseTAss;
		this.foreignKeys = foreignKeys;
		this.associations = associations;
		this.primaryKeys = primaryKeys;
		this.tableFields = tableFields;
		this.allFields = allFields;
		this.fieldsTable = fieldsTable;
		this.fieldType = fieldType;
		this.ids = ids;
		this.intScope = intScope;
		this.assList = assList;
		this.typeList = typeList;
		this.sigs = sigs;
	}

	public boolean isAssociation(String element) {
		// for (String s : assList) {
		// if (s.equalsIgnoreCase(element)) {
		// return true;
		// }
		// }
		for (Sig sig : this.sigs) {
			if (sig.category == 1 && sig.sigName.equalsIgnoreCase(element)) {
				return true;
			}
		}
		return false;
	}

	public int numOfInsert() {
		int sum = 0;
		for (Map.Entry<String, HashMap<String, HashMap<Integer, String>>> scheme : this.allInsertStmts
				.entrySet()) {
			for (Map.Entry<String, HashMap<Integer, String>> entry : scheme
					.getValue().entrySet()) {
				sum += entry.getValue().size();
			}
		}
		return sum;
	}

	public String getNegation4(String xmlFile) {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] paths = xmlFile.split(pattern);
		String fileName = paths[paths.length - 1];
		String factName = fileName.substring(0, fileName.length() - 4);
		String negation = "";
		negation += System.getProperty("line.separator") + "fact " + factName
				+ " {" + System.getProperty("line.separator");

		int instanceNum = 1;
		String value_part = "";
		// negation += "no ";
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : this.allInstances
				.entrySet()) {
			String element = entry.getKey();
			if (!isAssociation(element)) {
				continue;
			}

			for (Map.Entry<String, ArrayList<CodeNamePair>> instance : entry
					.getValue().entrySet()) {
				value_part = "";
				negation += "no o" + instanceNum + ":" + element + "|";
				// negation += "o" + instanceNum + ":" + element + ",";
				// String instanceName = instance.getKey();
				ArrayList<CodeNamePair> allFields = instance.getValue();
				for (CodeNamePair fields : allFields) {
					String field = fields.getFirst();
					if (isID(field.split("_")[1])) {
						String value = fields.getSecond();
						value_part += "o" + instanceNum + "." + field + "="
								+ value + " && ";
					}
				}
				value_part = value_part.substring(0, value_part.length() - 4);
				value_part += System.getProperty("line.separator");
				instanceNum++;
				negation += value_part;
			}
		}
		// negation = negation.substring(0, negation.length() - 1);
		// value_part = value_part.substring(0,
		// value_part.length()-System.getProperty("line.separator").length());
		// negation += "|" + System.getProperty("line.separator") + value_part +
		// System.getProperty("line.separator");
		negation += "}";

		return negation;
	}

	public String getNegation3(String xmlFile) {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] paths = xmlFile.split(pattern);
		String fileName = paths[paths.length - 1];
		String factName = fileName.substring(0, fileName.length() - 4);
		String negation = "";
		negation += System.getProperty("line.separator") + "fact " + factName
				+ " {" + System.getProperty("line.separator");

		int instanceNum = 1;
		String value_part = "";
		negation += "no ";
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : this.allInstances
				.entrySet()) {
			String element = entry.getKey();
			if (!isAssociation(element)) {
				continue;
			}
			for (Map.Entry<String, ArrayList<CodeNamePair>> instance : entry
					.getValue().entrySet()) {
				negation += "o" + instanceNum + ":" + element + ",";
				String instanceName = instance.getKey();
				ArrayList<CodeNamePair> allFields = instance.getValue();
				for (CodeNamePair fields : allFields) {
					String field = fields.getFirst();
					// field = field.split("_")[1];
					// check if field is ID or not
					if (isID(field.split("_")[1])) {
						String value = fields.getSecond();
						value_part += "o" + instanceNum + "." + field + "="
								+ value + " && ";
						// int intValue = Integer.valueOf(value).intValue();
						// intValue = intValue + (int) (Math.pow(2, intScope -
						// 1) + 1);
						// negation += "o." + field + "=" + value + " && ";
					}

				}
				value_part += System.getProperty("line.separator");
				// value_part += System.getProperty("line.separator");
				// negation = negation.substring(0, negation.length() - 4) +
				// System.getProperty("line.separator");
				instanceNum++;
			}
			// String goToTable = getTableNameByElement()
		}
		negation = negation.substring(0, negation.length() - 1);
		value_part = value_part.substring(0, value_part.length() - 4
				- System.getProperty("line.separator").length());
		negation += "|" + System.getProperty("line.separator") + value_part
				+ System.getProperty("line.separator");
		negation += "}";

		return negation;
	}

	public String getNegation2(String xmlFile) {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] paths = xmlFile.split(pattern);
		String fileName = paths[paths.length - 1];
		String factName = fileName.substring(0, fileName.length() - 4);
		String negation = "";
		// negation += System.getProperty("line.separator") + "fact " + factName
		// + " {" + System.getProperty("line.separator");

		int instanceNum = 1;
		String value_part = "";
		negation += "no ";
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : this.allInstances
				.entrySet()) {
			String element = entry.getKey();
			for (Map.Entry<String, ArrayList<CodeNamePair>> instance : entry
					.getValue().entrySet()) {
				negation += "o" + instanceNum + ":" + element + ",";
				String instanceName = instance.getKey();
				ArrayList<CodeNamePair> allFields = instance.getValue();
				for (CodeNamePair fields : allFields) {
					String field = fields.getFirst();
					// field = field.split("_")[1];
					// check if field is ID or not
					if (isID(field.split("_")[1])) {
						String value = fields.getSecond();
						value_part += "o" + instanceNum + "." + field + "="
								+ value + " && ";
						// int intValue = Integer.valueOf(value).intValue();
						// intValue = intValue + (int) (Math.pow(2, intScope -
						// 1) + 1);
						// negation += "o." + field + "=" + value + " && ";
					}

				}
				value_part += System.getProperty("line.separator");
				// value_part += System.getProperty("line.separator");
				// negation = negation.substring(0, negation.length() - 4) +
				// System.getProperty("line.separator");
				instanceNum++;
			}
			// String goToTable = getTableNameByElement()
		}
		negation = negation.substring(0, negation.length() - 1);
		value_part = value_part.substring(0, value_part.length() - 4
				- System.getProperty("line.separator").length());
		negation += "|" + System.getProperty("line.separator") + value_part
				+ System.getProperty("line.separator");
		// negation += "}";

		return negation;
	}

	public String getNegation(String xmlFile) {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] paths = xmlFile.split(pattern);
		String fileName = paths[paths.length - 1];
		String factName = fileName.substring(0, fileName.length() - 4);
		String negation = "";
		String forGlobalNegation = "";
		// negation += System.getProperty("line.separator") + "fact " + factName
		// + " {" + System.getProperty("line.separator");
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : this.allInstances
				.entrySet()) {
			String element = entry.getKey();
			for (Map.Entry<String, ArrayList<CodeNamePair>> instance : entry
					.getValue().entrySet()) {
				forGlobalNegation = "";
				forGlobalNegation = "no o:" + element + " | ";
				negation += "no o:" + element + " | ";
				String instanceName = instance.getKey();
				ArrayList<CodeNamePair> allFields = instance.getValue();
				for (CodeNamePair fields : allFields) {
					String field = fields.getFirst();
					// field = field.split("_")[1];
					// check if field is ID or not
					if (isID(field.split("_")[1])) {
						String value = fields.getSecond();
						// int intValue = Integer.valueOf(value).intValue();
						// intValue = intValue + (int) (Math.pow(2, intScope -
						// 1) + 1);
						negation += "o." + field + "=" + value + " && ";
						forGlobalNegation += "o." + field + "=" + value
								+ " && ";
					}
				}
				forGlobalNegation = forGlobalNegation.substring(0,
						forGlobalNegation.length() - 4);// +
														// System.getProperty("line.separator");
				globalNegation.put(forGlobalNegation, "");
				negation = negation.substring(0, negation.length() - 4)
						+ System.getProperty("line.separator");
			}
			// String goToTable = getTableNameByElement()
		}
		// negation += "}";

		return negation;
	}

	public boolean isID(String field) {
		for (String s : this.ids) {
			if (s.equals(field))
				return true;
		}
		return false;
	}

	public void solveModel(String model, String solutions) throws Err,
			IOException {
		File log = new File(solutions + System.getProperty("file.separator")
				+ "log.txt");
		FileOutputStream logFS = new FileOutputStream(log, true);
		PrintWriter logPW = new PrintWriter(logFS);
		int solutionNo = 1;
		String xmlFile = "";
		int maxSol = 1000000;
		// while (maxSol-- > 0) {
		long start = System.currentTimeMillis();
		long forStop = System.currentTimeMillis();
		long forOutput = System.currentTimeMillis();
		long forAddNegation = System.currentTimeMillis();

		int max = 0;
		while (maxSol > solutionNo) {

			String negation = callAlloyEngine(model, solutionNo);
			if (negation == null) {
				break;
			}
			// add negation at the end of model
			PrintStream ps = new PrintStream(new FileOutputStream(new File(
					model), true));
			ps.print(negation);
			String outputLog = "Solution #" + solutionNo++
					+ " has been generated.";
			if (isDebugOn) {
				System.out.println(outputLog);
			}
			logPW.println(outputLog);

			long now = System.currentTimeMillis();
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			long elapsed = now - start;
			Date date_elapsed = new Date(elapsed);
			outputLog = "Within time: " + sdf.format(date_elapsed);
			if (isDebugOn) {
				System.out.println(outputLog);
			}
			logPW.println(outputLog);

			// CHANGE UPDATE TO INSERT
			int updateSize = numOfInsert();
			if (isDebugOn) {
				System.out.println("number of all updates: " + updateSize);
			}
			logPW.println("number of all updates: " + updateSize);
			if (updateSize > max) {
				if (isDebugOn) {
					System.out.println("new updates: " + (updateSize - max));
				}
				logPW.println("new updates: " + (updateSize - max));
				// update time and max
				max = updateSize;
				forStop = System.currentTimeMillis();
			} else {
				long stop = System.currentTimeMillis();
				long noNewStmts = stop - forStop;
				if (noNewStmts > TimeUnit.MINUTES.toMillis(1)) {
					if (isDebugOn) {
						System.out
								.println("No new insert statements created in 5 minutes...");
					}
					logPW.println("No new insert statements created in 5 minutes...");
					logPW.flush();
					break;
				}
			}

			long outputTime = System.currentTimeMillis();
			long hour = outputTime - forOutput;
			if (hour >= TimeUnit.MINUTES.toMillis(15)) {
				// call function to output the queries
				outputEachHour(model);
				// this.allUpdateStmts.clear();
				// this.allInsertStmts.clear();
				// max = 0;
				// updateSize = 0;
				forOutput = System.currentTimeMillis();
			}
			logPW.println();
			logPW.flush();
			if (isDebugOn) {
				System.out.println("");
			}
		}

		outputEachHour(model);

		logPW.close();
	}

	public String getCurrentTime() {
		// DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		return "--------" + dateFormat.format(date);
	}

	/**
	 * Output all statements in memory to disk
	 * 
	 * @param i
	 *            : the caller 0: from synthesizer 1: from random generator
	 */
	public void printAllStatements(int i) {
		if (isDebugOn) {
			System.out.println("Enter printAllStatements()" + getCurrentTime());
		}
		// create insert printwriters for data schemas
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
				.entrySet()) {
			String dbSchemaFile = entry.getKey(); // this file include .sql
													// extension
			String OMName = dbSchemaFile.substring(
					dbSchemaFile.lastIndexOf(File.separator) + 1,
					dbSchemaFile.lastIndexOf("."));
			String bmFolder = null;
			bmFolder = dbSchemaFile.substring(0,
					dbSchemaFile.lastIndexOf(File.separator))
					+ File.separator + "Benchmark";
			if (!new File(bmFolder).exists()) {
				new File(bmFolder).mkdir();
			}

			if (i == 0) {
				insertFile = bmFolder + File.separator + OMName + "_insert.sql";
			} else {
				insertFile = bmFolder + File.separator + OMName
						+ "_insert_random.sql";
			}

			try {
				if (!new File(insertFile).exists()) {
					new File(insertFile).createNewFile();
				}
				FileWriter fw = new FileWriter(insertFile, true);
				PrintWriter pw = new PrintWriter(fw);
				String dbName = dbSchemaFile.substring(0,
						dbSchemaFile.length() - 4);
				dbName = dbName
						.substring(dbName.lastIndexOf(File.separator) + 1,
								dbName.length());
				// Chong: isMySQL
				pw.println("USE " + dbName + ";");
				this.insertPrintWriters.put(dbSchemaFile, pw);
			} catch (IOException e) {
				e.printStackTrace(); // To change body of catch statement use
										// File | Settings | File Templates.
			}
		}

		// // create update print writers for data schemas
		// for (Map.Entry<String, HashMap<String,
		// ArrayList<CodeNamePair>>> entry : schemas.entrySet()) {
		// String dbSchemaFile = entry.getKey(); // this file include .sql
		// extension
		// String updateFile = dbSchemaFile.substring(0, dbSchemaFile.length() -
		// 4) + "_update.sql";
		// try {
		// FileWriter fw = new FileWriter(updateFile, true);
		// PrintWriter pw = new PrintWriter(fw);
		// this.updatePrintWriters.put(dbSchemaFile, pw);
		// } catch (IOException e) {
		// e.printStackTrace(); //To change body of catch statement use File |
		// Settings | File Templates.
		// }
		// }

		// create select print writers for data schemas
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
				.entrySet()) {
			String dbSchemaFile = entry.getKey(); // this file include .sql
													// extension

			String OMName = dbSchemaFile.substring(
					dbSchemaFile.lastIndexOf(File.separator) + 1,
					dbSchemaFile.lastIndexOf("."));

			String bmFolder = null;
			bmFolder = dbSchemaFile.substring(0,
					dbSchemaFile.lastIndexOf(File.separator))
					+ File.separator + "Benchmark";
			if (!new File(bmFolder).exists()) {
				new File(bmFolder).mkdir();
			}

			if (i == 0) {
				selectFile = bmFolder + File.separator + OMName + "_select.sql";
			} else {
				selectFile = bmFolder + File.separator + OMName
						+ "_select_random.sql";
			}

			try {
				if (!new File(selectFile).exists()) {
					new File(selectFile).createNewFile();
				}
				FileWriter fw = new FileWriter(selectFile, true);
				PrintWriter pw = new PrintWriter(fw);
				String dbName = dbSchemaFile.substring(0,
						dbSchemaFile.length() - 4);
				dbName = dbName
						.substring(dbName.lastIndexOf(File.separator) + 1,
								dbName.length());
				// Chong: isMySQL
				pw.println("USE " + dbName + ";");
				this.selectPrintWriters.put(dbSchemaFile, pw);
			} catch (IOException e) {
				e.printStackTrace(); // To change body of catch statement use
										// File | Settings | File Templates.
			}
		}

		// print all insert statements
		for (Map.Entry<String, PrintWriter> entry : this.insertPrintWriters
				.entrySet()) {
			String dbScheme = entry.getKey();
			// String insertFile = null;
			// String OMName =
			// dbScheme.substring(dbScheme.lastIndexOf(File.separator)+1,
			// dbScheme.indexOf("_"));
			// if(i == 0){
			// insertFile = dbScheme.substring(0,
			// dbScheme.lastIndexOf(File.separator)) + File.separator +
			// "Benchmark" + File.separator + OMName + "_insert.sql";
			// } else {
			// insertFile = dbScheme.substring(0,
			// dbScheme.lastIndexOf(File.separator)) + File.separator +
			// "Benchmark" + File.separator + OMName + "_insert_random.sql";
			// }

			HashMap<String, HashMap<Integer, String>> stmts = this.allInsertStmts
					.get(dbScheme);
			if (stmts == null) {
				continue;
			}

			PrintWriter pw = entry.getValue();
			// dbScheme is the path (includes filename)
			// /home/tang/customerOrder.als
			ArrayList<String> orders = this.printOrder.get(dbScheme);
			// loop in order
			for (String s : orders) {
				if (stmts.containsKey(s)) {
					HashMap<Integer, String> stmt = stmts.get(s);
					for (Map.Entry<Integer, String> single_stmt : stmt
							.entrySet()) {
						pw.println(single_stmt.getValue());
					}
					pw.flush();
				}
			}
		}

		// // print all update statements
		// for (Map.Entry<String, PrintWriter> entry :
		// this.updatePrintWriters.entrySet()) {
		// String dbScheme = entry.getKey();
		// String insertFile = dbScheme.substring(0, dbScheme.length() - 4) +
		// "_update.sql";
		// HashMap<String, HashMap<String, String>> stmts =
		// this.allUpdateStmts.get(dbScheme);
		// if (stmts == null) {
		// continue;
		// }
		// PrintWriter pw = entry.getValue();
		// ArrayList<String> orders = this.printOrder.get(dbScheme);
		// // loop in order
		// for (String s : orders) {
		// if (stmts.containsKey(s)) {
		// HashMap<String, String> stmt = stmts.get(s);
		// for (Map.Entry<String, String> single_stmt : stmt.entrySet()) {
		// pw.println(single_stmt.getKey());
		// }
		// pw.flush();
		// }
		// }
		// }
		// print all select statements
		for (Map.Entry<String, PrintWriter> entry : this.selectPrintWriters
				.entrySet()) {
			String dbScheme = entry.getKey();
			// String selectFile = null;
			// String OMName =
			// dbScheme.substring(dbScheme.lastIndexOf(File.separator)+1,
			// dbScheme.indexOf("_"));
			// if(i == 0){
			// selectFile = dbScheme.substring(0,
			// dbScheme.lastIndexOf(File.separator)) + File.separator +
			// "Benchmark" + File.separator + OMName + "_select.sql";
			// } else {
			// selectFile = dbScheme.substring(0,
			// dbScheme.lastIndexOf(File.separator)) + File.separator +
			// "Benchmark" + File.separator + OMName + "_select_random.sql";
			// }

			HashMap<String, ArrayList<String>> stmts = this.allSelectStmts
					.get(dbScheme);
			if (stmts == null) {
				continue;
			}
			PrintWriter pw = entry.getValue();
			ArrayList<String> orders = this.printOrder.get(dbScheme);
			// loop in order
			for (String s : orders) {
				if (stmts.containsKey(s)) {
					ArrayList<String> stmt = stmts.get(s);
					for (String single_stmt : stmt) {
						pw.println(single_stmt);
					}
					pw.flush();
				}
			}
		}
		// close all PrintWriter
		for (Map.Entry<String, PrintWriter> entry : this.insertPrintWriters
				.entrySet()) {
			PrintWriter pw = entry.getValue();
			pw.close();
		}
		// // close all PrintWriter
		// for (Map.Entry<String, PrintWriter> entry :
		// this.updatePrintWriters.entrySet()) {
		// PrintWriter pw = entry.getValue();
		// pw.close();
		// }
		// close all PrintWriter
		for (Map.Entry<String, PrintWriter> entry : this.selectPrintWriters
				.entrySet()) {
			PrintWriter pw = entry.getValue();
			pw.close();
		}

		this.allInstances.clear();
		this.allInsertStmts.clear();
		this.allSelectStmts.clear();
		if (isDebugOn) {
			System.out.println("Leave printAllStatements()" + getCurrentTime());
		}
	}

	public void outputEachHour(String model) {
		getOutPutOrders(model);
		// create insert printwriters for data schemas
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
				.entrySet()) {
			String dbSchemaFile = entry.getKey(); // this file include .sql
													// extension
			String OMName = dbSchemaFile.substring(
					dbSchemaFile.lastIndexOf(File.separator) + 1,
					dbSchemaFile.lastIndexOf("."));

			String bmFolder = null;
			bmFolder = dbSchemaFile.substring(0,
					dbSchemaFile.lastIndexOf(File.separator))
					+ File.separator + "Benchmark";
			if (!new File(bmFolder).exists()) {
				new File(bmFolder).mkdir();
			}

			// if(i == 0){
			insertFile = bmFolder + File.separator + OMName + "_insert.sql";
			// } else {
			// insertFile = dbSchemaFile.substring(0,
			// dbSchemaFile.lastIndexOf(File.separator)) + File.separator +
			// "Benchmark" + File.separator + OMName + "_select_random.sql";
			// }
			// String insertFile = dbSchemaFile.substring(0,
			// dbSchemaFile.length() - 4) + "_insert.sql";
			// String instantFile = dbSchemaFile.substring(0,
			// dbSchemaFile.length() - 4) + "_insert_noOrder.sql";
			try {
				FileWriter fw = new FileWriter(insertFile);
				PrintWriter pw = new PrintWriter(fw);
				String dbName = dbSchemaFile.substring(0,
						dbSchemaFile.length() - 4);
				dbName = dbName
						.substring(dbName.lastIndexOf(File.separator) + 1,
								dbName.length());
				// Chong: isMySQL
				pw.println("USE " + dbName + ";");
				this.insertPrintWriters.put(dbSchemaFile, pw);
				// this.insertInstantPrintWriters.put(dbSchemaFile, pw1);
			} catch (IOException e) {
				e.printStackTrace(); // To change body of catch statement use
										// File | Settings | File Templates.
			}
		}

		// create update print writers for data schemas
		// for (Map.Entry<String, HashMap<String,
		// ArrayList<CodeNamePair>>> entry : schemas.entrySet()) {
		// String dbSchemaFile = entry.getKey(); // this file include .sql
		// extension
		// String updateFile = dbSchemaFile.substring(0, dbSchemaFile.length() -
		// 4) + "_update.sql";
		// // String updateInstantFile = dbSchemaFile.substring(0,
		// dbSchemaFile.length() - 4) + "_update_noOrder.sql";
		// try {
		// FileWriter fw = new FileWriter(updateFile);
		// PrintWriter pw = new PrintWriter(fw);
		// this.updatePrintWriters.put(dbSchemaFile, pw);
		// // this.updateInstantPrintWriters.put(dbSchemaFile, pw1);
		// } catch (IOException e) {
		// e.printStackTrace(); //To change body of catch statement use File |
		// Settings | File Templates.
		// }
		// }

		// create select print writers for data schemas
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
				.entrySet()) {
			String dbSchemaFile = entry.getKey(); // this file include .sql
													// extension
													// String selectFile =
													// dbSchemaFile.substring(0,
			// dbSchemaFile.length() - 4) + "_select.sql";

			String OMName = dbSchemaFile.substring(
					dbSchemaFile.lastIndexOf(File.separator) + 1,
					dbSchemaFile.lastIndexOf("."));

			String bmFolder = null;
			bmFolder = dbSchemaFile.substring(0,
					dbSchemaFile.lastIndexOf(File.separator))
					+ File.separator + "Benchmark";
			if (!new File(bmFolder).exists()) {
				new File(bmFolder).mkdir();
			}

			// if(i == 0){
			selectFile = bmFolder + File.separator + OMName + "_select.sql";

			// String updateInstantFile = dbSchemaFile.substring(0,
			// dbSchemaFile.length() - 4) + "_update_noOrder.sql";
			try {
				FileWriter fw = new FileWriter(selectFile);
				PrintWriter pw = new PrintWriter(fw);
				String dbName = dbSchemaFile.substring(0,
						dbSchemaFile.length() - 4);
				dbName = dbName
						.substring(dbName.lastIndexOf(File.separator) + 1,
								dbName.length());
				// Chong: isMySQL
				pw.println("USE " + dbName + ";");
				this.selectPrintWriters.put(dbSchemaFile, pw);
				// this.updateInstantPrintWriters.put(dbSchemaFile, pw1);
			} catch (IOException e) {
				e.printStackTrace(); // To change body of catch statement use
										// File | Settings | File Templates.
			}
		}

		// print all statements
		for (Map.Entry<String, PrintWriter> entry : this.insertPrintWriters
				.entrySet()) {
			String dbScheme = entry.getKey();
			HashMap<String, HashMap<Integer, String>> stmts = this.allInsertStmts
					.get(dbScheme);
			if (stmts == null) {
				continue;
			}
			// if file has content, delete all contents first

			PrintWriter pw = entry.getValue();
			ArrayList<String> orders = this.printOrder.get(dbScheme);
			// loop in order
			// Sarun
			for (String s : orders) {
				if (stmts.containsKey(s)) {
					HashMap<Integer, String> stmt = stmts.get(s);
					for (Map.Entry<Integer, String> single_stmt : stmt
							.entrySet()) {
						pw.println(single_stmt.getValue());
					}
					pw.flush();
				}
			}
		}

		// print all update statements
		// for (Map.Entry<String, PrintWriter> entry :
		// this.updatePrintWriters.entrySet()) {
		// String dbScheme = entry.getKey();
		// String insertFile = dbScheme.substring(0, dbScheme.length() - 4) +
		// "_update.sql";
		// HashMap<String, HashMap<String, String>> stmts =
		// this.allUpdateStmts.get(dbScheme);
		// if (stmts == null) {
		// continue;
		// }
		// PrintWriter pw = entry.getValue();
		// ArrayList<String> orders = this.printOrder.get(dbScheme);
		// // loop in order
		// for (String s : orders) {
		// if (stmts.containsKey(s)) {
		// HashMap<String, String> stmt = stmts.get(s);
		// for (Map.Entry<String, String> single_stmt : stmt.entrySet()) {
		// pw.println(single_stmt.getKey());
		// }
		// pw.flush();
		// }
		// }
		// }

		// print all select statements
		for (Map.Entry<String, PrintWriter> entry : this.selectPrintWriters
				.entrySet()) {
			String dbScheme = entry.getKey();
			HashMap<String, ArrayList<String>> stmts = this.allSelectStmts
					.get(dbScheme);
			if (stmts == null) {
				continue;
			}
			PrintWriter pw = entry.getValue();
			ArrayList<String> orders = this.printOrder.get(dbScheme);
			// loop in order
			// Sarun
			for (String s : orders) {
				if (stmts.containsKey(s)) {
					ArrayList<String> stmt = stmts.get(s);
					for (String single_stmt : stmt) {
						pw.println(single_stmt);
					}
					pw.flush();
				}
			}
		}

		// close all PrintWriter
		for (Map.Entry<String, PrintWriter> entry : this.insertPrintWriters
				.entrySet()) {
			PrintWriter pw = entry.getValue();
			pw.close();
		}
		// close all PrintWriter
		// for (Map.Entry<String, PrintWriter> entry :
		// this.updatePrintWriters.entrySet()) {
		// PrintWriter pw = entry.getValue();
		// pw.close();
		// }
		// close all PrintWriter
		for (Map.Entry<String, PrintWriter> entry : this.selectPrintWriters
				.entrySet()) {
			PrintWriter pw = entry.getValue();
			pw.close();
		}
	}

	public String callAlloyEngine(String model, int solutionNo) throws Err,
			FileNotFoundException {
		String xmlFile = "";
		String negation = "";
		String trimmedFilename = model.substring(0, model.length() - 4);
		boolean isFinished = false;
		Module root = null; // (14:45:08)
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
		root = CompUtil.parseEverything_fromFile(rep, null, model);

		// Choose some default options for how you want to execute the commands
		A4Options options = new A4Options();
		options.solver = A4Options.SatSolver.SAT4J; // .KK;//.MiniSatJNI;
													// //.MiniSatProverJNI;//.SAT4J;
		options.symmetry = AppConfig.getA4ReportSymmetry();
		options.skolemDepth = AppConfig.getA4ReportSkolemDepth();

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

			if (solution.satisfiable()) {
				// System.out.println("Within time: " + format2);
				xmlFile = trimmedFilename + "_Sol_" + solutionNo + ".xml";
				solution.writeXML(xmlFile); // This writes out "answer_0.xml",
											// "answer_1.xml"...
				// after get each solution, we parse it and output the instances
				// into sql files for every data schema
				parseDocument(xmlFile);
				negation = getNegation(xmlFile);
				// negation = getNegation(xmlFile);
				getOutPutOrders(xmlFile);
				generateInsert();
				generateUpdate();
				// parseSelect(xmlFile);
				// delete xml files
				try {
					File file = new File(xmlFile);
					if (file.delete()) {
						// System.out.println(file.getName() + " is deleted!");
					} else {
						// System.out.println("Delete operation is failed.");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				if (isDebugOn) {
					System.out.println("No more Satisfying solutions");
					System.out
							.println("\n-----------------------------------------");
				}
				return null;
			}
			solution = solution.next();
		}

		return negation;
	}

	boolean isFinished = false;

	public void solveWithContinueUpdate(String model, String solutions)
			throws Err, FileNotFoundException {
		String negation = "";
		String factName = "";
		int factNum = 1;
		while (true) {
			solve(model, solutions);
			if (isFinished) {
				break;
			}
			// add negation at the end of model
			PrintStream ps = new PrintStream(new FileOutputStream(new File(
					model), true));
			factName = "fact_" + factNum;
			factNum++;
			// construct negation
			negation = System.getProperty("line.separator") + "fact "
					+ factName + " {" + System.getProperty("line.separator");
			for (Map.Entry<String, String> s_negation : this.globalNegation
					.entrySet()) {
				negation += s_negation.getKey()
						+ System.getProperty("line.separator");
			}
			negation += "}";
			ps.print(negation);
			ps.flush();
			ps.close();
			this.globalNegation.clear();
		}

	}

	int solutionNo = 1;

	public void solve(String model, String solutions) throws Err,
			FileNotFoundException {
		File log = new File(solutions + System.getProperty("file.separator")
				+ "log.txt");
		FileOutputStream logFS = new FileOutputStream(log, true);
		PrintWriter logPW = new PrintWriter(logFS);
		String trimmedFilename = model.substring(0, model.length() - 4);
		String xmlFileName = "";
		Module root = null; // (14:45:08)
		int maxSol = 1000000000;
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
		root = CompUtil.parseEverything_fromFile(rep, null, model);

		// Choose some default options for how you want to execute the commands
		A4Options options = new A4Options();
		options.solver = A4Options.SatSolver.SAT4J; // .KK;//.MiniSatJNI;
													// //.MiniSatProverJNI;//.SAT4J;
		options.symmetry = AppConfig.getA4ReportSymmetry();
		options.skolemDepth = AppConfig.getA4ReportSkolemDepth();
		long start = System.currentTimeMillis();
		long forOutput = System.currentTimeMillis();
		long forStop = System.currentTimeMillis();
		long hundredMinutes = TimeUnit.MINUTES.toMillis(1);

		try {
			for (Command command : root.getAllCommands()) {
				// Execute the command
				A4Solution solution = TranslateAlloyToKodkod.execute_command(
						rep, root.getAllReachableSigs(), command, options);
				for (ExprVar a : solution.getAllAtoms()) {
					root.addGlobal(a.label, a);
				}
				for (ExprVar a : solution.getAllSkolems()) {
					root.addGlobal(a.label, a);
				}

				// solutionNo = 1;
				int max = 0;
				while (!isFinished) {
					if (solutionNo > maxSol) {
						break;
					}
					if (solution.satisfiable()) {
						long now = System.currentTimeMillis();
						String outputLog = "Solution #" + solutionNo
								+ " has been generated.";
						if (isDebugOn) {
							System.out.println(outputLog);
						}
						logPW.println(outputLog);
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
						sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
						long elapsed = now - start;
						Date date_elapsed = new Date(elapsed);
						outputLog = "Within time: " + sdf.format(date_elapsed);
						if (isDebugOn) {
							System.out.println(outputLog);
						}
						logPW.println(outputLog);
						logPW.flush();

						// System.out.println("Within time: " + format2);
						xmlFileName = trimmedFilename + "_Sol_" + solutionNo
								+ ".xml";
						solution.writeXML(xmlFileName); // This writes out
														// "answer_0.xml",
														// "answer_1.xml"...
						// after get each solution, we parse it and output the
						// instances into sql files for every data schema
						parseDocument(xmlFileName);
						// System.out.println("Finish parseDocument");
						getNegation(xmlFileName);
						// System.out.println("Finish getNegation");
						generateInsert();
						// System.out.println("Finish generateInsert");
						// generateUpdate();
						// System.out.println("Finish generateUpdate");
						generateSelect1();
						// System.out.println("Finish generateSelect1");
						// delete the xml files
						try {
							File file = new File(xmlFileName);
							if (file.delete()) {
								// System.out.println(file.getName() +
								// " is deleted!");
							} else {
								// System.out.println("Delete operation is failed.");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						if (isDebugOn) {
							System.out.println("No more Satisfying solutions");
							System.out
									.println("\n-----------------------------------------");
						}
						isFinished = true;
						break;
					}
					// System.out.println("Finish if");
					// print out each hour
					long outputTime = System.currentTimeMillis();
					long hour = outputTime - forOutput;
					if (hour >= TimeUnit.MINUTES.toMillis(15)) {
						// call function to output the queries
						outputEachHour(model);
						forOutput = System.currentTimeMillis();
					}

					// System.out.println("try to get # of inserts");
					int insertSize = numOfInsert();
					if (isDebugOn) {
						System.out.println("# of all inserts: " + insertSize);
					}
					logPW.println("# of all inserts: " + insertSize);
					if (insertSize > max) {
						if (isDebugOn) {
							System.out.println("new insert stmts: "
									+ (insertSize - max));
						}
						logPW.println("new insert stmts: " + (insertSize - max));
						// update time and max
						max = insertSize;
						forStop = System.currentTimeMillis();
					} else {
						long stop = System.currentTimeMillis();
						long noNewStmts = stop - forStop;
						if (noNewStmts > hundredMinutes) {
							if (isDebugOn) {
								System.out
										.println("No new insert statements created in 5 minutes...");
							}
							logPW.println("No new insert statements created in 5 minutes...");
							logPW.flush();
							break;
						}
					}
					logPW.println("");
					System.out.println("");
					solution = solution.next();
					solutionNo++;
				}
			}
		} finally {
			outputEachHour(model);
		}
	}

	public void parseSelect(String alloyDMSolFile) {
		String dbSchemaFile = "";
		// create printwriters for data schemas
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
				.entrySet()) {
			dbSchemaFile = entry.getKey(); // this file include .sql extension
			String selectFile = dbSchemaFile.substring(0,
					dbSchemaFile.length() - 4) + "_select.sql";
			try {
				FileWriter fw = new FileWriter(selectFile, true);
				PrintWriter pw = new PrintWriter(fw);
				this.selectPrintWriters.put(selectFile, pw);
			} catch (IOException e) {
				e.printStackTrace(); // To change body of catch statement use
										// File | Settings | File Templates.
			}
		}

		ArrayList<String> fields = null;
		ArrayList<String> coveredFields = null;
		ArrayList<String> fieldsPlusIDs = null;

		// ArrayList<ArrayList<String>> queryFileds = new
		// ArrayList<ArrayList<String>>();
		for (int i = 0; i < 1; i++) {
			fields = new ArrayList<String>();
			int size = allFields.get(dbSchemaFile).size();
			// for (int j = 0; j < 3; j++) {
			Random rand1 = new Random(System.currentTimeMillis());
			int value1 = rand1.nextInt(size);
			String tempField = allFields.get(dbSchemaFile).get(value1);
			// force to have different fields
			allFields.get(dbSchemaFile).set(value1,
					allFields.get(dbSchemaFile).get(size - 1));
			allFields.get(dbSchemaFile).set(size - 1, tempField);
			size--;
			if (!fields.contains(tempField)) {
				fields.add(tempField);
			}

			// }
			// fieldsPlusIDs = new ArrayList<String>(fields);

			for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
					.entrySet()) {
				dbSchemaFile = entry.getKey(); // this file include .sql
												// extension
				String insertFile = dbSchemaFile.substring(0,
						dbSchemaFile.length() - 4)
						+ "_select.sql";
				PrintWriter pw = this.selectPrintWriters.get(insertFile); // findWriterByPath(insertFile);

				coveredFields = new ArrayList<String>();
				fieldsPlusIDs = new ArrayList<String>(fields);
				// / Here we add the id related to each field to the tableFields
				// list for the reason of joining.
				for (String f : fields) {
					String fieldID = getIDByField(dbSchemaFile, f);
					if ((!fieldID.equalsIgnoreCase(f))
							&& !fieldsPlusIDs.contains(fieldID))
						fieldsPlusIDs.add(fieldID);
				}

				// ArrayList<String> tables = new ArrayList<String>();
				ArrayList<String> coveredTables = new ArrayList<String>();
				ArrayList<CodeNamePair> unCoveredFields = new ArrayList<CodeNamePair>(
						fieldsTable.get(dbSchemaFile));
				// ArrayList<String> unCoveredFields = new
				// ArrayList<String>(tableFields);
				// table_name -> # of tableFields covered by it
				// HashMap<String, ArrayList<String>> tableFieldsCovered = new
				// HashMap<String, ArrayList<String>>();

				// for (CodeNamePair field_table : unCoveredFields) {
				int iterator = unCoveredFields.size() - 1;
				for (; iterator >= 0; iterator--) {
					CodeNamePair field_table = unCoveredFields
							.get(iterator);
					// if (!tableFields.contains(field_table.getFirst()))
					if (!fieldsPlusIDs.contains(field_table.getFirst()))
						unCoveredFields.remove(field_table);
				}

				ArrayList<String> tables = new ArrayList<String>();
				for (CodeNamePair field_table : unCoveredFields) {
					String tableName = field_table.getSecond();
					if (!tables.contains(tableName))
						tables.add(tableName);
				}

				do {
					HashMap<String, ArrayList<String>> tableFieldsCovered = new HashMap<String, ArrayList<String>>();
					// initialize the tableFieldsCovered
					for (String tableName : tables)
						// tableFieldsCovered.put(tableName, 0);
						tableFieldsCovered.put(tableName,
								new ArrayList<String>());

					// fill the tableFieldsCovered
					for (String tableName : tables) {
						// for (CodeNamePair field_table :
						// fieldsTable.get(dbSchemaFile)) {
						for (CodeNamePair field_table : unCoveredFields) {
							String curTable = field_table.getSecond();
							if (curTable.equalsIgnoreCase(tableName))// &&
																		// tableFields.contains(curTable))
								// tableFieldsCovered.put(tableName,
								// tableFieldsCovered.get(tableName) + 1);
								tableFieldsCovered.get(tableName).add(
										field_table.getFirst());
						}
					}

					// find the most covered table
					String maxTable = null;
					int maxValue = 0;
					for (Map.Entry<String, ArrayList<String>> table : tableFieldsCovered
							.entrySet()) {
						String tableName = table.getKey();
						Integer value = table.getValue().size();
						if (value > maxValue) {
							maxValue = value;
							maxTable = tableName;
						}
					}
					coveredTables.add(maxTable);
					tables.remove(maxTable);

					// remove tableFields covered by the most covered table
					// for (CodeNamePair field_table :
					// fieldsTable.get(dbSchemaFile)) {
					// for (CodeNamePair field_table : unCoveredFields)
					// {
					// if (field_table.getSecond().equalsIgnoreCase(maxTable))
					// unCoveredFields.remove(field_table.getSecond());
					// }

					ArrayList<String> currentCoveredFields = tableFieldsCovered
							.get(maxTable);
					iterator = unCoveredFields.size() - 1;
					for (; iterator >= 0; iterator--) {
						CodeNamePair field_table = unCoveredFields
								.get(iterator);
						String fieldName = field_table.getFirst();
						if (currentCoveredFields.contains(fieldName)
								&& fields.contains(fieldName))
							unCoveredFields.remove(field_table);
					}

					for (String fieldName : fields) {
						if (currentCoveredFields.contains(fieldName)
								&& !coveredFields.contains(fieldName))
							coveredFields.add(fieldName);
					}

					// } while (unCoveredFields.size() > 0);
				} while (!coveredFields.containsAll(fields));

				String fieldsStatement = fields.get(0);
				int j = 1;
				while (j < fields.size()) {
					fieldsStatement += ", `" + fields.get(j) + "`";
					j++;
				}
				String fromStatement = " FROM `" + coveredTables.get(0) + "`";
				j = 1;
				while (j < coveredTables.size()) {
					fromStatement += ", `" + coveredTables.get(j) + "`";
					j++;
				}
				// String s = "SELECT " + fieldsStatement + fromStatement + ";";
				String s = "SELECT " + fieldsStatement + fromStatement; //

				// where field_1 operator a number [or a field_2]
				String whereStatement = " WHERE ";
				Random rand = new Random(System.currentTimeMillis());
				int value = rand.nextInt(fields.size());
				String fieldInWhere = fields.get(value);
				whereStatement += "`" + fieldInWhere + "`";

				String[] operands = { "<=", "<", "=", ">", ">=" };
				value = rand.nextInt(operands.length);
				String operand = operands[value];
				// whereStatement += " " + operand;

				String comparedTo = "";
				String type = getFieldType(dbSchemaFile, fieldInWhere);
				if (type.equalsIgnoreCase("Bool")) {
					value = rand.nextInt(2);
					if (value > 1)
						comparedTo = "true";
					else
						comparedTo = "false";
					whereStatement += " = " + comparedTo;
				} else if (type.equalsIgnoreCase("string")) {
					String randString = "Dtype";
					whereStatement += " = `" + randString + "`";
				} else if (type.equalsIgnoreCase("Integer")) {
					int randValue = rand.nextInt(2 ^ intScope - 1);
					whereStatement += operand + randValue;
				} else if (type.equalsIgnoreCase("Real")) {
					int randValue = rand.nextInt(2 ^ intScope - 1);
					whereStatement += operand + randValue;
				} else if (type.equalsIgnoreCase("DType")) {
					int randValue = rand.nextInt(coveredTables.size());
					whereStatement += " = " + coveredTables.get(randValue);
				}

				s += whereStatement;
				s += ";";
				pw.println(s);
			}
		}
	}

	public String getFieldType(String scheme, String field) {
		ArrayList<CodeNamePair> types = this.fieldType.get(scheme);
		for (CodeNamePair field_type : types) {
			if (field_type.getFirst().equalsIgnoreCase(field)) {
				return field_type.getSecond();
			}
		}
		return "";
	}

	public String getIDByField(String scheme, String field) {
		String output = field;
		String tableName = null;
		// check if field is id or not
		for (CodeNamePair pair : this.primaryKeys.get(scheme)) {
			if (pair.getSecond().equalsIgnoreCase(field)) {
				return output;
			}
		}

		String fieldTable = new String();
		// it means that it's not the id, then it is not foreign key
		// then we need to find out the primary key of the table that this field
		// belongs to
		for (CodeNamePair pair : this.tableFields.get(scheme)) {
			if (pair.getSecond().equalsIgnoreCase(field))
				fieldTable = pair.getFirst();
		}

		for (CodeNamePair pair : this.primaryKeys.get(scheme)) {
			if (pair.getFirst().equalsIgnoreCase(fieldTable)) {
				return pair.getSecond();
			}
		}

		return output;
	}

	public boolean isPrimaryKeys(String dbScheme, String table, String field) {
		ArrayList<CodeNamePair> keys = this.primaryKeys.get(dbScheme);
		for (CodeNamePair s : keys) {
			if (s.getFirst().equalsIgnoreCase(table)
					&& s.getSecond().equalsIgnoreCase(field)) {
				return true;
			}
		}
		return false;
	}

	public String getParent(String element) {
		String parent = null;
		for (Sig sig : this.sigs) {
			if (sig.category == 0) {
				if (sig.hasParent) {
					return sig.parent;
				}
			}
		}
		return parent;
	}

	public void generateSelect1() {
		// System.out.println("Enter generateSelect" + getCurrentTime());
		// this.allSelectStmts.clear();
		String selectPart = "";
		String fromPart = "";
		String wherePart = "";
		ArrayList<CodeNamePair> allAboutOMClass;
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> instances : this.allInstances
				.entrySet()) {
			String element = instances.getKey();
			boolean isAss = isAssociation(element);
			// ignore association????
			if (isAss) {
				continue;
			}
			for (Map.Entry<String, ArrayList<CodeNamePair>> instance : instances
					.getValue().entrySet()) {
				for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> omClass : this.schemas
						.entrySet()) {
					selectPart = "SELECT ";
					fromPart = " FROM ";
					wherePart = " WHERE ";
					String dbScheme = omClass.getKey();
					// ArrayList<String> offSprings = getOffSprings(dbScheme,
					// element);
					String goToTable = getTableNameByElement(dbScheme, element);
					String parent = getParent(element);
					if (parent == null) { // element is a root class
						allAboutOMClass = omClass.getValue().get(element);
						fromPart += "`" + element + "`";
						for (CodeNamePair pair : allAboutOMClass) {
							if (pair.getFirst().equalsIgnoreCase("fields")) {
								String field = pair.getSecond();
								selectPart += "`" + element + "`.`" + field
										+ "`,";
								if (isPrimaryKeys(dbScheme, element, field)) {
									String value = getFieldValue(
											instance.getValue(), field);
									wherePart += "`" + element + "`.`" + field
											+ "`=" + value + " AND ";
								}
							}
						}
					} else if (!goToTable.equalsIgnoreCase(element)) { // class
																		// C is
																		// mapped
																		// to
																		// the
																		// same
																		// table
																		// as
																		// its
																		// super
																		// class
						// ArrayList<CodeNamePair> allAboutOMClass =
						// omClass.getValue().get(goToTable);
						// fromPart += goToTable;
						// for (CodeNamePair pair : allAboutOMClass) {
						// if (pair.getFirst().equalsIgnoreCase("fields")) {
						// String field = pair.getSecond();
						// selectPart += "`" + goToTable + "`.`" + field + "`,";
						// if (isPrimaryKeys(dbScheme, goToTable, field)) {
						// String value = getFieldValue(instance.getValue(),
						// field);
						// wherePart += "`" + goToTable + "`.`" + field + "`=" +
						// value + " AND ";
						// }
						// }
						// }
					} else if (goToTable.equalsIgnoreCase(element)) { // class C
																		// is
																		// mapped
																		// to
																		// its
																		// own
																		// table
						fromPart += "`" + goToTable + "`";
						allAboutOMClass = omClass.getValue().get(element);
						for (CodeNamePair pair : allAboutOMClass) {
							if (pair.getFirst().equalsIgnoreCase("fields")) {
								String field = pair.getSecond();
								selectPart += "`" + element + "`.`" + field
										+ "`,";
								if (isPrimaryKeys(dbScheme, element, field)) {
									String value = getFieldValue(
											instance.getValue(), field);
									wherePart += "`" + element + "`.`" + field
											+ "`=" + value + " AND ";
								}
							}
						}
					}
					selectPart = selectPart.substring(0,
							selectPart.length() - 1);
					wherePart = wherePart.substring(0, wherePart.length() - 5);
					// fromPart = fromPart.substring(0, fromPart.length() - 1);
					String stmt = selectPart + fromPart + wherePart + ";";
					if (stmt.substring(0, 11).equalsIgnoreCase("select from")) {
						continue;
					}
					stmt += "RESET QUERY CACHE;";
					// System.out.println("Check selects");
					// if (!dataSchemaHasSelectStatement(dbScheme, goToTable,
					// stmt)) {
					// System.out.println("Finish check selects");
					addSelectStmtIntoDataSchema(dbScheme, goToTable, stmt);
					// }
				}
			}
		}
		// System.out.println("Leave generateSelect" + getCurrentTime());
	}

	public void generateSelect() {
		// System.out.println("Enter generateSelect()" + getCurrentTime());

		String selectPart = "";
		String fromPart = "";
		String wherePart = "";
		HashSet<String> coveredFileds = new HashSet<String>();
		HashSet<String> coveredTableInFrom = new HashSet<String>();
		// start with the instances
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> instances : this.allInstances
				.entrySet()) {
			String element = instances.getKey();
			boolean isAss = isAssociation(element);
			// iterate all the instances in instance for element
			for (Map.Entry<String, ArrayList<CodeNamePair>> instance : instances
					.getValue().entrySet()) {
				for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> omClass : this.schemas
						.entrySet()) {
					String dbScheme = omClass.getKey();
					ArrayList<String> offSprings = getOffSprings(dbScheme,
							element);
					// String insertFile = dbScheme.substring(0,
					// dbScheme.length() - 4) + "_insert.sql";
					// PrintWriter pw = findWriterByPath(insertFile);
					// PrintWriter pw = this.insertPrintWriters.get(dbScheme);
					String goToTable = getTableNameByElement(dbScheme, element);
					if (goToTable.length() == 0) {
						// there is no t_association information for this
						// element
						// which indicates it's a association table
						continue;
					}
					coveredFileds.clear();
					coveredTableInFrom.clear();
					// get the fields list of goToTable
					ArrayList<CodeNamePair> allAboutOMClass = omClass
							.getValue().get(goToTable);
					// String id = getPrimaryKeyByTableName(omClass.getValue(),
					// goToTable);
					// String id_value = getFieldValue(instance.getValue(), id);
					selectPart = "SELECT ";
					fromPart = " FROM `" + goToTable + "`,";
					coveredTableInFrom.add(goToTable);
					wherePart = " WHERE ";
					// wherePart += "`"+goToTable+"`.`"+id + "`=" + id_value +
					// " AND ";
					// coveredTableInFrom.add(id);
					for (CodeNamePair pair : allAboutOMClass) {
						if (pair.getFirst().equalsIgnoreCase("fields")) {
							String field = pair.getSecond();
							// if (!coveredFileds.contains(field)) {
							selectPart += "`" + goToTable + "`.`" + field
									+ "`,";
							coveredFileds.add(field);
							if (isPrimaryKeys(dbScheme, goToTable, field)) {
								String value = getFieldValue(
										instance.getValue(), field);
								wherePart += "`" + goToTable + "`.`" + field
										+ "`=" + value + " AND ";
							}
							// }
						}
					}
					if (isAss) {
						// get src
						String srcElement = getSrcElement(element);
						String dstElement = getDstElement(element);
						// fromPart += "`" + srcElement + "`,`" + dstElement +
						// "`,";
						// allAboutOMClass.clear();
						allAboutOMClass = omClass.getValue().get(srcElement);
						if (allAboutOMClass != null) {
							for (CodeNamePair pair : allAboutOMClass) {
								if (pair.getFirst().equalsIgnoreCase("fields")) {
									String field = pair.getSecond();
									if (!coveredFileds.contains(field)) {
										if (!coveredTableInFrom
												.contains(srcElement)) {
											fromPart += "`" + srcElement + "`,";
											coveredTableInFrom.add(srcElement);
										}
										selectPart += "`" + srcElement + "`.`"
												+ field + "`,";
										coveredFileds.add(field);
									}
									if (isPrimaryKeys(dbScheme, srcElement,
											field)) {
										// String value =
										// getFieldValue(instance.getValue(),
										// field);
										if (coveredTableInFrom
												.contains(srcElement)) {
											wherePart += "`" + srcElement
													+ "`.`" + field + "`=`"
													+ goToTable + "`.`" + field
													+ "` AND ";
										}
									}
								}
							}
						}
						// allAboutOMClass.clear();
						allAboutOMClass = omClass.getValue().get(dstElement);
						if (allAboutOMClass != null) {
							for (CodeNamePair pair : allAboutOMClass) {
								if (pair.getFirst().equalsIgnoreCase("fields")) {
									String field = pair.getSecond();
									if (!coveredFileds.contains(field)) {
										if (!coveredTableInFrom
												.contains(dstElement)) {
											fromPart += "`" + dstElement + "`,";
											coveredTableInFrom.add(dstElement);
										}
										selectPart += "`" + dstElement + "`.`"
												+ field + "`,";
										coveredFileds.add(field);
									}
									if (isPrimaryKeys(dbScheme, dstElement,
											field)) {
										// String value =
										// getFieldValue(instance.getValue(),
										// field);
										if (coveredTableInFrom
												.contains(dstElement)) {
											wherePart += "`" + dstElement
													+ "`.`" + field + "`=`"
													+ goToTable + "`.`" + field
													+ "` AND ";
											// "`" + dstElement + "`.`" + field
											// + "`=" + value + " AND ";
										}
									}
								}
							}
						}
					}
					for (String offSpring : offSprings) {
						// allAboutOMClass.clear();
						allAboutOMClass = omClass.getValue().get(offSpring);
						if (allAboutOMClass == null) {
							continue;
						}
						for (CodeNamePair pair : allAboutOMClass) {
							if (pair.getFirst().equalsIgnoreCase("fields")) {
								String field = pair.getSecond();
								if (!coveredFileds.contains(field)) {
									selectPart += "`" + offSpring + "`.`"
											+ field + "`,";
									coveredFileds.add(field);
								}
								if (isPrimaryKeys(dbScheme, offSpring, field)) {
									// String value =
									// getFieldValue(instance.getValue(),
									// field);
									if (coveredTableInFrom.contains(offSpring)) {
										wherePart += "`" + offSpring + "`.`"
												+ field + "`=`" + goToTable
												+ "`.`" + field + "` AND ";
									}
								}
							}
						}
					}

					selectPart = selectPart.substring(0,
							selectPart.length() - 1);
					wherePart = wherePart.substring(0, wherePart.length() - 5);
					fromPart = fromPart.substring(0, fromPart.length() - 1);
					String stmt = selectPart + fromPart + wherePart + ";";
					if (stmt.substring(0, 10).equalsIgnoreCase("select from")) {
						continue;
					} else if (!dataSchemaHasSelectStatement(dbScheme,
							goToTable, stmt)) {
						addSelectStmtIntoDataSchema(dbScheme, goToTable, stmt);
					}
				}
			}
		}
		// System.out.println("Leave generateSelect()" + getCurrentTime());
	}

	public ArrayList<String> getOffSprings(String dbScheme, String element) {
		ArrayList<String> offSprings = new ArrayList<String>();
		ArrayList<CodeNamePair> parents = this.parents.get(dbScheme);
		for (CodeNamePair pair : parents) {
			if (pair.getSecond().equalsIgnoreCase(element)) {
				offSprings.add(pair.getFirst());
			}
		}
		return offSprings;
	}

	public String getSrcElement(String ass) {
		for (Sig sig : this.sigs) {
			if (sig.category == 1 && sig.sigName.equalsIgnoreCase(ass)) {
				return sig.src;
			}
		}
		return null;
	}

	public String getDstElement(String ass) {
		for (Sig sig : this.sigs) {
			if (sig.category == 1 && sig.sigName.equalsIgnoreCase(ass)) {
				return sig.dst;
			}
		}
		return null;
	}

	public void generateInsert() {
		// this.allInsertStmts.clear();
		String field_part = "";
		String value_part = "";
		String element;
		String dbScheme;
		String goToTable;
		ArrayList<CodeNamePair> allAboutOMClass;
		ArrayList<String> fTables;
		ArrayList<HashMap<String, CodeNamePair>> associations = new ArrayList<HashMap<String, CodeNamePair>>();
		HashMap<String, CodeNamePair> ass;
		// start with the instances
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> instances : this.allInstances
				.entrySet()) {
			element = instances.getKey();
			// iterate all the instances in instance for element
			for (Map.Entry<String, ArrayList<CodeNamePair>> instance : instances
					.getValue().entrySet()) {
				for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> omClass : this.schemas
						.entrySet()) {
					dbScheme = omClass.getKey();
					// String insertFile = dbScheme.substring(0,
					// dbScheme.length() - 4) + "_insert.sql";
					// PrintWriter pw = findWriterByPath(insertFile);
					// PrintWriter pw = this.insertPrintWriters.get(dbScheme);
					goToTable = getTableNameByElement(dbScheme, element);
					if (goToTable.length() == 0) {
						// there is no t_association information for this
						// element
						// which indicates it's a association table
						continue;
					}
					// get the fields list of goToTable
					allAboutOMClass = omClass.getValue().get(goToTable);
					String id = getPrimaryKeyByTableName(omClass.getValue(),
							goToTable);
					String id_value = getFieldValue(instance.getValue(), id);
					field_part = "";
					value_part = "";
					for (CodeNamePair pair : allAboutOMClass) {
						if (pair.getFirst().equalsIgnoreCase("fields")) {
							String field = pair.getSecond();
							// then get value of this field from instance, which
							// we needs element name and field name
							String value = getFieldValue(instance.getValue(),
									field);
							// if returns null, indicates no such field in
							// element
							if (value != null) {
								field_part += "`" + field + "`,";
								value_part += value + ",";
							} else {
								if (field.equalsIgnoreCase("DType")) {
									value = "'" + element + "'";
									field_part += "`" + field + "`,";
									value_part += value + ",";
								} else {
									// if the field is foreign key
									boolean isForeignKey = isForeignKey(
											omClass.getValue(), goToTable,
											field);
									if (isForeignKey) {
										// find association
										// find the foreign tables by primary
										// key
										// this function may returns a list of
										// tables, for example, customer ID is
										// the
										// primary key of Customer table and
										// also PreferredCustomer table
										fTables = getTablesByPrimaryKey(
												dbScheme, field);
										// the ArrayList should only has one
										// element?????
										associations.clear();
										// for each of the table, try to find
										// the list of associations
										for (String fTable : fTables) {
											ass = getAssByKey(dbScheme,
													element, fTable);
											if (ass != null) {
												associations.add(ass);
											}
										}
										// now we have the associations, still
										// need to find out which association to
										// search
										// first get the primary key of this
										// table
										// then get the

										for (HashMap<String, CodeNamePair> tmp_ass : associations) {
											for (Map.Entry<String, CodeNamePair> ass_entry : tmp_ass
													.entrySet()) {
												// need the value of id, srcdst,
												// srcdst1
												String para = null;
												if (ass_entry.getValue()
														.getFirst()
														.equalsIgnoreCase(id)) {
													para = ass_entry.getValue()
															.getSecond();
												} else {
													para = ass_entry.getValue()
															.getFirst();
												}
												String pKeyOfPara = getPrimaryKeyByTableName(
														omClass.getValue(),
														para);
												String fValue = getForeignKeyValue(
														this.allInstances.get(ass_entry
																.getKey()),
														id_value, id,
														pKeyOfPara);
												field_part += "`" + field
														+ "`,";
												value_part += fValue + ",";
											}
										}
									}
								}
							}
						}
					}
					// insert the id into existing id
					// this is used to avoid duplicate objects, for ex, the
					// customer and preferredcustomer with same id
					// if (!this.existingID.containsKey(dbScheme)) {
					// this.existingID.put(dbScheme, new HashMap<String,
					// HashSet<String>>());
					// }
					// if
					// (!this.existingID.get(dbScheme).containsKey(goToTable)) {
					// this.existingID.get(dbScheme).put(goToTable, new
					// HashSet<String>());
					// }
					// // check if the id is existing in ArrayList
					// boolean hasID =
					// this.existingID.get(dbScheme).get(goToTable).add(id_value);

					// if(hasID){
					field_part = field_part.substring(0,
							field_part.length() - 1);
					value_part = value_part.substring(0,
							value_part.length() - 1);
					String stmt = "INSERT INTO `" + goToTable + "` ("
							+ field_part + ") VALUES (" + value_part + ");";
					stmt += "FLUSH TABLES;";
					// System.out.println("Check inserts");
					if (!dataSchemaHasInsertStatement(dbScheme, goToTable,
							Integer.valueOf(id_value))) {
						// System.out.println("Finish check inserts");
						addInsertStmtIntoDataSchema(dbScheme, goToTable, stmt,
								Integer.valueOf(id_value));
					}
					// }
				}
			}
		}
		// System.out.println("Leave generateInsert()" + getCurrentTime());
	}

	public int getTATISum(String dbScheme) {
		int tati = 0;
		for (Sig sig : this.sigs) {
			if (sig.category == 0) {
				tati += getTATI(dbScheme, sig.sigName);
			}
		}
		return tati;
	}

	public int getTATI(String dbScheme, String className) {
		HashSet<String> table = new HashSet<String>();
		this.childrenInOM.clear();
		table.add(getTableNameByElement(dbScheme, className));
		this.getChildrenOM(className);
		for (String child : childrenInOM) {
			String elementTable = getTableNameByElement(dbScheme, child);
			table.add(elementTable);
		}
		return table.size();
	}

	HashSet<String> childrenInOM = new HashSet<String>();

	public void getChildrenOM(String parent) {
		// ArrayList<String> children = new ArrayList<String>();
		for (Sig sig : this.sigs) {
			if (sig.hasParent && sig.parent.equalsIgnoreCase(parent)) {
				childrenInOM.add(sig.sigName);
				getChildrenOM(sig.sigName);
			}
		}
		// Collection<String> c = children;
		// return c;
	}

	public int getNCTSum(String dbScheme) {
		int nct = 0;
		for (Sig sig : this.sigs) {
			nct += getNCT(dbScheme, sig.sigName);
		}
		return nct;
	}

	public int getNCT(String dbScheme, String className) {
		for (Sig sig : this.sigs) {
			if (sig.category == 0 && sig.sigName.equalsIgnoreCase(className)) {
				if (!sig.hasParent) {
					return 1;
				} else {
					String parentTable = getTableNameByElement(dbScheme,
							sig.parent);
					String elementTable = getTableNameByElement(dbScheme,
							sig.sigName);
					if (parentTable.equalsIgnoreCase(elementTable)) {
						return getNCT(dbScheme, sig.parent);
					} else { // class is mapped to its own table
						return getNCT(dbScheme, sig.parent) + 1;
					}
				}
			}
		}
		return 0;
	}

	// Sarun
	public List<String> getOutputOrder(String modelFileName){

		return null;
	}

	public void getOutPutOrders(String instFile) {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String instFiles[] = instFile.split(pattern);
		String fileName = instFiles[instFiles.length - 1];

		if (fileName.contains("customer")) {
			// init print order
			for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
					.entrySet()) {
				String dbSchemaFile = entry.getKey(); // this file include .sql
														// extension
				this.printOrder.put(dbSchemaFile, new ArrayList<String>());
				this.printOrder.get(dbSchemaFile).add("Customer");
				this.printOrder.get(dbSchemaFile).add("PreferredCustomer");
				this.printOrder.get(dbSchemaFile).add("Order");
				this.printOrder.get(dbSchemaFile).add(
						"CustomerOrderAssociation");
			}
		} else if (fileName.contains("CSOS")) {
			for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
					.entrySet()) {
				String dbSchemaFile = entry.getKey(); // this file include .sql
														// extension
				// String insertFile = dbSchemaFile.substring(0,
				// dbSchemaFile.length() - 4) + "_insert.sql";
				this.printOrder.put(dbSchemaFile, new ArrayList<String>());
				this.printOrder.get(dbSchemaFile).add("Channel");
				this.printOrder.get(dbSchemaFile).add("Principal");
				this.printOrder.get(dbSchemaFile).add("Role");
				this.printOrder.get(dbSchemaFile).add("ProcessStateMachine");
				this.printOrder.get(dbSchemaFile).add(
						"ProcessStateMachineState");
				this.printOrder.get(dbSchemaFile).add(
						"ProcessStateMachineAction");
				this.printOrder.get(dbSchemaFile).add(
						"ProcessStateMachineEvent");
				this.printOrder.get(dbSchemaFile).add(
						"ProcessStateMachineTransition");
				this.printOrder.get(dbSchemaFile).add(
						"ProcessStateMachineExecution");
				this.printOrder.get(dbSchemaFile).add("EmailChannel");
				this.printOrder.get(dbSchemaFile).add("SMSChannel");
				this.printOrder.get(dbSchemaFile).add("ProcessQueryResponse");
				this.printOrder.get(dbSchemaFile).add(
						"ProcessQueryResponseAction");
				this.printOrder.get(dbSchemaFile).add(
						"ProcessQueryResponseExecution");
				this.printOrder.get(dbSchemaFile).add("PrincipalProxy");
				this.printOrder.get(dbSchemaFile).add("PrincipalRole");
				this.printOrder.get(dbSchemaFile).add("MachineStates");
				this.printOrder.get(dbSchemaFile).add("TerminalStates");
				this.printOrder.get(dbSchemaFile).add("StateMachineEvents");
				this.printOrder.get(dbSchemaFile)
						.add("StateMachineTransitions");
			}
		} else if (fileName.contains("ecommerce")) {
			for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
					.entrySet()) {
				String dbSchemaFile = entry.getKey(); // this file include .sql
														// extension
				// String insertFile = dbSchemaFile.substring(0,
				// dbSchemaFile.length() - 4) + "_insert.sql";
				this.printOrder.put(dbSchemaFile, new ArrayList<String>());
				this.printOrder.get(dbSchemaFile).add("Customer");
				this.printOrder.get(dbSchemaFile).add("Asset");
				this.printOrder.get(dbSchemaFile).add("Order");
				this.printOrder.get(dbSchemaFile).add("ShippingCart");
				this.printOrder.get(dbSchemaFile).add("Item");
				this.printOrder.get(dbSchemaFile).add("Category");
				this.printOrder.get(dbSchemaFile).add("Catalog");
				this.printOrder.get(dbSchemaFile).add("Product");
				this.printOrder.get(dbSchemaFile).add("CartItem");
				this.printOrder.get(dbSchemaFile).add("OrderItem");
				this.printOrder.get(dbSchemaFile).add("PhysicalProduct");
				this.printOrder.get(dbSchemaFile).add("ElectronicProduct");
				this.printOrder.get(dbSchemaFile).add("Service");
				this.printOrder.get(dbSchemaFile).add("Media");
				this.printOrder.get(dbSchemaFile).add("Documents");
				this.printOrder.get(dbSchemaFile).add(
						"CustomerOrderAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"CustomerShippingCartAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"ShippingCartItemAssociation");
				this.printOrder.get(dbSchemaFile).add("OrderItemAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"ProductCategoryAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"ProductCatalogAssociation");
				this.printOrder.get(dbSchemaFile).add("ProductItemAssociation");
				this.printOrder.get(dbSchemaFile)
						.add("ProductAssetAssociation");
			}
		} else if (fileName.contains("decider")) {
			for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
					.entrySet()) {
				String dbSchemaFile = entry.getKey(); // this file include .sql
														// extension
				// String insertFile = dbSchemaFile.substring(0,
				// dbSchemaFile.length() - 4) + "_insert.sql";
				this.printOrder.put(dbSchemaFile, new ArrayList<String>());
				this.printOrder.get(dbSchemaFile).add("User");
				this.printOrder.get(dbSchemaFile).add("NameSpace");
				this.printOrder.get(dbSchemaFile).add("Variable");
				this.printOrder.get(dbSchemaFile).add("Relationship");
				this.printOrder.get(dbSchemaFile).add("Role");
				this.printOrder.get(dbSchemaFile).add("Cluster");
				this.printOrder.get(dbSchemaFile).add("DecisionSpace");
				this.printOrder.get(dbSchemaFile).add("roleBindings");
				this.printOrder.get(dbSchemaFile).add("Participants");
				this.printOrder.get(dbSchemaFile).add("DSN");
				this.printOrder.get(dbSchemaFile).add(
						"NameSpaceOwnerAssociation");
				this.printOrder.get(dbSchemaFile).add("varInAssociation");
				this.printOrder.get(dbSchemaFile).add("varOutAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"clusterVariableAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"userDecisionSpaceAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"descisionSpaceRoleBindingsAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"descisionSpaceParticipantsAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"descisionSpaceVariablesAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"descisionSpaceRoleAssociation");
				// this.printOrder.get(dbSchemaFile).add("descisionSpaceUserAssociation");
				this.printOrder.get(dbSchemaFile).add("DSNUserAssociation");
				this.printOrder.get(dbSchemaFile)
						.add("DSNNamespaceAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"DSNDecisionSpaceAssociation");
			}
		} else if (fileName.contains("person")) {
			for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
					.entrySet()) {
				String dbSchemaFile = entry.getKey(); // this file include .sql
														// extension
				// String insertFile = dbSchemaFile.substring(0,
				// dbSchemaFile.length() - 4) + "_insert.sql";
				this.printOrder.put(dbSchemaFile, new ArrayList<String>());
				this.printOrder.get(dbSchemaFile).add("Person");
				this.printOrder.get(dbSchemaFile).add("Student");
				this.printOrder.get(dbSchemaFile).add("Employee");
				this.printOrder.get(dbSchemaFile).add("Clerk");
				this.printOrder.get(dbSchemaFile).add("Manager");
			}
		} else if (fileName.contains("wordpress")) {
			for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
					.entrySet()) {
				String dbSchemaFile = entry.getKey(); // this file include .sql
														// extension
				// String insertFile = dbSchemaFile.substring(0,
				// dbSchemaFile.length() - 4) + "_insert.sql";
				this.printOrder.put(dbSchemaFile, new ArrayList<String>());
				this.printOrder.get(dbSchemaFile).add("CommentMeta");
				this.printOrder.get(dbSchemaFile).add("Comments");
				this.printOrder.get(dbSchemaFile).add("Links");
				this.printOrder.get(dbSchemaFile).add("PostMeta");
				this.printOrder.get(dbSchemaFile).add("Posts");
				this.printOrder.get(dbSchemaFile).add("Pages");
				this.printOrder.get(dbSchemaFile).add("UserMeta");
				this.printOrder.get(dbSchemaFile).add("Users");
				this.printOrder.get(dbSchemaFile).add("Terms");
				this.printOrder.get(dbSchemaFile).add("Tags");
				this.printOrder.get(dbSchemaFile).add("Category");
				this.printOrder.get(dbSchemaFile).add("PostCategory");
				this.printOrder.get(dbSchemaFile).add("LinkCategory");
				this.printOrder.get(dbSchemaFile).add("CommentPostAssociation");
				this.printOrder.get(dbSchemaFile).add("CommentUserAssociation");
				this.printOrder.get(dbSchemaFile).add("PostUserAssociation");
				this.printOrder.get(dbSchemaFile).add("TermPostsAssociation");
				this.printOrder.get(dbSchemaFile).add("TermLinksAssociation");
			}
		} else if (fileName.contains("moodle")) {
			for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
					.entrySet()) {
				String dbSchemaFile = entry.getKey(); // this file include .sql
														// extension
				// String insertFile = dbSchemaFile.substring(0,
				// dbSchemaFile.length() - 4) + "_insert.sql";
				this.printOrder.put(dbSchemaFile, new ArrayList<String>());
				this.printOrder.get(dbSchemaFile).add("Course");
				this.printOrder.get(dbSchemaFile).add("GradeItem");
				this.printOrder.get(dbSchemaFile).add("Grades");
				this.printOrder.get(dbSchemaFile).add("ScaleGrades");
				this.printOrder.get(dbSchemaFile).add("PointGrades");
				this.printOrder.get(dbSchemaFile).add("GradeSettings");
				this.printOrder.get(dbSchemaFile).add("ImportNewItem");
				this.printOrder.get(dbSchemaFile).add("ImportValues");
				this.printOrder.get(dbSchemaFile).add(
						"CourseGradeItemAssociation");
				this.printOrder.get(dbSchemaFile).add(
						"CourseGradeSettingsAssociation");
				// this.printOrder.get(dbSchemaFile).add("CourseOutcomeAssociation");
				// this.printOrder.get(dbSchemaFile).add("CourseGradeCategoriesAssociation");
				// this.printOrder.get(dbSchemaFile).add("GradeCategoryGradeItemAssociation");
				// this.printOrder.get(dbSchemaFile).add("GradeItemGradesAssociation");
				// this.printOrder.get(dbSchemaFile).add("CourseGradeSettingsAssociation");
				// this.printOrder.get(dbSchemaFile).add("ImportNewitemImportValuesAssociation");
			}
		} else if (fileName.contains("ke")) {
			for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
					.entrySet()) {
				String dbSchemaFile = entry.getKey(); // this file include .sql
														// extension
				this.printOrder.put(dbSchemaFile, new ArrayList<String>());
				this.printOrder.get(dbSchemaFile).add("Response");
			}
		} else { // this is the default case, for any other object model, the
					// print order is empty string list
			for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
					.entrySet()) {
				String dbSchemaFile = entry.getKey(); // this file include .sql
														// extension
				this.printOrder.put(dbSchemaFile, new ArrayList<String>());
			}
		}
	}

	public void generateUpdate() {
		// System.out.println("Enter generateUpdate()" + getCurrentTime());
		// create update print writers for data schemas
		// for (Map.Entry<String, HashMap<String,
		// ArrayList<CodeNamePair>>> entry : schemas.entrySet()) {
		// String dbSchemaFile = entry.getKey(); // this file include .sql
		// extension
		// String updateInstantFile = dbSchemaFile.substring(0,
		// dbSchemaFile.length() - 4) + "_update_noOrder.sql";
		// try {
		// FileWriter fw1 = new FileWriter(updateInstantFile, true);
		// PrintWriter pw1 = new PrintWriter(fw1);
		// this.updateInstantPrintWriters.put(dbSchemaFile, pw1);
		// } catch (IOException e) {
		// e.printStackTrace(); //To change body of catch statement use File |
		// Settings | File Templates.
		// }
		// }

		String updateStmt = "";
		// start with the instances
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> instances : this.allInstances
				.entrySet()) {
			String element = instances.getKey();
			// iterate all the instances in instance for element
			for (Map.Entry<String, ArrayList<CodeNamePair>> instance : instances
					.getValue().entrySet()) {
				for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> omClass : this.schemas
						.entrySet()) {
					String dbScheme = omClass.getKey();
					// String insertFile = dbScheme.substring(0,
					// dbScheme.length() - 4) + "_insert.sql";
					// PrintWriter pw = findWriterByPath(insertFile);
					// PrintWriter pw = this.insertPrintWriters.get(dbScheme);
					String goToTable = getTableNameByElement(dbScheme, element);
					if (goToTable.length() == 0) {
						// there is no t_association information for this
						// element
						// which indicates it's a association table
						continue;
					}
					updateStmt = "";
					updateStmt += "UPDATE `" + goToTable + "` SET ";

					// get the fields list of goToTable
					ArrayList<CodeNamePair> allAboutOMClass = omClass
							.getValue().get(goToTable);
					String id = getPrimaryKeyByTableName(omClass.getValue(),
							goToTable);
					String id_value = getFieldValue(instance.getValue(), id);
					for (CodeNamePair pair : allAboutOMClass) {
						if (pair.getFirst().equalsIgnoreCase("fields")) {
							String field = pair.getSecond();
							// then get value of this field from instance, which
							// we needs element name and field name
							String value = getFieldValue(instance.getValue(),
									field);
							// if returns null, indicates no such field in
							// element
							if (value != null) {
								updateStmt += field + "=" + value + ",";
							} else {
								if (field.equalsIgnoreCase("DType")) {
									value = "'" + element + "'";
									updateStmt += field + "=" + value + ",";
								} else {
									// if the field is foreign key
									boolean isForeignKey = isForeignKey(
											omClass.getValue(), goToTable,
											field);
									if (isForeignKey) {
										// find association
										// find the foreign tables by primary
										// key
										// this function may returns a list of
										// tables, for example, customer ID is
										// the
										// primary key of Customer table and
										// also PreferredCustomer table
										ArrayList<String> fTables = getTablesByPrimaryKey(
												dbScheme, field);
										// the ArrayList should only has one
										// element?????
										ArrayList<HashMap<String, CodeNamePair>> associations = new ArrayList<HashMap<String, CodeNamePair>>();
										// for each of the table, try to find
										// the list of associations
										for (String fTable : fTables) {
											HashMap<String, CodeNamePair> ass = getAssByKey(
													dbScheme, element, fTable);
											if (ass != null) {
												associations.add(ass);
											}
										}
										// now we have the associations, still
										// need to find out which association to
										// search
										// first get the primary key of this
										// table
										// then get the

										for (HashMap<String, CodeNamePair> ass : associations) {
											for (Map.Entry<String, CodeNamePair> ass_entry : ass
													.entrySet()) {
												// need the value of id, srcdst,
												// srcdst1
												String para = null;
												if (ass_entry.getValue()
														.getFirst()
														.equalsIgnoreCase(id)) {
													para = ass_entry.getValue()
															.getSecond();
												} else {
													para = ass_entry.getValue()
															.getFirst();
												}
												String pKeyOfPara = getPrimaryKeyByTableName(
														omClass.getValue(),
														para);
												String fValue = getForeignKeyValue(
														this.allInstances.get(ass_entry
																.getKey()),
														id_value, id,
														pKeyOfPara);
												updateStmt += field + "="
														+ fValue + ",";
											}
										}
									}
								}
							}
						}
					}
					updateStmt = updateStmt.substring(0,
							updateStmt.length() - 1);
					updateStmt += " WHERE " + id + "=" + id_value + ";";
					updateStmt += "RESET QUERY CACHE;";
					if (!dataSchemaHasUpdateStatement(dbScheme, goToTable,
							updateStmt)) {
						addUpdateStmtIntoDataSchema(dbScheme, goToTable,
								updateStmt);
						// this.updateInstantPrintWriters.get(dbScheme).print(updateStmt
						// + System.getProperty("line.separator"));
					}
				}
			}
		}
		// for (Map.Entry<String, PrintWriter> pw :
		// this.updateInstantPrintWriters.entrySet()) {
		// pw.getValue().close();
		// }
		// System.out.println("Leave generateUpdate()" + getCurrentTime());
	}

	public void addUpdateStmtIntoDataSchema(String dataSchema,
			String tableName, String stmt) {
		boolean contains = this.allUpdateStmts.containsKey(dataSchema);
		if (!contains) {
			this.allUpdateStmts.put(dataSchema,
					new HashMap<String, HashMap<String, String>>());
			this.allUpdateStmts.get(dataSchema).put(tableName,
					new HashMap<String, String>());
		}
		if (!this.allUpdateStmts.get(dataSchema).containsKey(tableName)) {
			this.allUpdateStmts.get(dataSchema).put(tableName,
					new HashMap<String, String>());
		}
		this.allUpdateStmts.get(dataSchema).get(tableName).put(stmt, "");
	}

	public boolean dataSchemaHasUpdateStatement(String dataSchema,
			String tableName, String stmt) {
		if (this.allUpdateStmts.containsKey(dataSchema)) {
			if (this.allUpdateStmts.get(dataSchema).containsKey(tableName)) {
				if (this.allUpdateStmts.get(dataSchema).get(tableName)
						.containsKey(stmt)) {
					return true;
				}
			}
		}
		return false;
	}

	public void addSelectStmtIntoDataSchema(String dataSchema,
			String tableName, String stmt) {
		boolean contains = this.allSelectStmts.containsKey(dataSchema);
		if (!contains) {
			this.allSelectStmts.put(dataSchema,
					new HashMap<String, ArrayList<String>>());
			this.allSelectStmts.get(dataSchema).put(tableName,
					new ArrayList<String>());
		}
		if (!this.allSelectStmts.get(dataSchema).containsKey(tableName)) {
			this.allSelectStmts.get(dataSchema).put(tableName,
					new ArrayList<String>());
		}
		this.allSelectStmts.get(dataSchema).get(tableName).add(stmt);
	}

	public boolean dataSchemaHasSelectStatement(String dataSchema,
			String tableName, String stmt) {
		if (this.allSelectStmts.containsKey(dataSchema)) {
			if (this.allSelectStmts.get(dataSchema).containsKey(tableName)) {
				if (this.allSelectStmts.get(dataSchema).get(tableName)
						.contains(stmt)) {
					return true;
				}
			}
		}
		return false;
	}

	public ArrayList<String> getTablesByPrimaryKey(String scheme,
			String primaryKey) {
		ArrayList<String> tables = new ArrayList<String>();
		ArrayList<CodeNamePair> pairs = this.primaryKeys.get(scheme);
		for (CodeNamePair pair : pairs) {
			if (pair.getSecond().equalsIgnoreCase(primaryKey)) {
				tables.add(pair.getFirst());
			}
		}
		return tables;
	}

	public boolean isForeignKey(
			HashMap<String, ArrayList<CodeNamePair>> scheme,
			String table, String field) {
		for (CodeNamePair pair : scheme.get(table)) {
			if (pair.getFirst().equalsIgnoreCase("foreignKey")) {
				if (pair.getSecond().equalsIgnoreCase(field)) {
					return true;
				}
			}
		}
		return false;
	}

	public String getFieldValue(ArrayList<CodeNamePair> instance,
			String field) {
		String value = null;
		for (CodeNamePair pair : instance) {
			if (pair.getFirst().split("_")[1].equalsIgnoreCase(field)) {
				String tmp = pair.getSecond();
				if (isNumeric(tmp)) {
					int intValue = Integer.valueOf(tmp).intValue();
					intValue = intValue + (int) (Math.pow(2, (intScope - 1)))
							+ 1;
					value = String.valueOf(intValue);
					return value;
				}
			}
		}
		return null;
	}

	public String getFTableByForeignKey(String scheme, String primaryKey) {
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : this.schemas
				.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(scheme)) {
				for (Map.Entry<String, ArrayList<CodeNamePair>> table : entry
						.getValue().entrySet()) {
					for (CodeNamePair pair : table.getValue()) {
						if (pair.getFirst().equalsIgnoreCase("primaryKey")) {
							if (pair.getSecond().equalsIgnoreCase(primaryKey)) {
								return table.getKey();
							}
						}
					}
				}
			}
		}
		return null;
	}

	public String getForeignKeyValue(
			HashMap<String, ArrayList<CodeNamePair>> in_instance,
			String key_value, String srcDst, String srcDst1) {
		String value = null;
		for (Map.Entry<String, ArrayList<CodeNamePair>> instance : in_instance
				.entrySet()) {
			for (CodeNamePair pair : instance.getValue()) {
				if (pair.getFirst().split("_")[1].equalsIgnoreCase(srcDst1)) {
					int intValue = Integer.valueOf(pair.getSecond()).intValue();
					intValue = intValue + (int) (Math.pow(2, (intScope - 1)))
							+ 1;
					return String.valueOf(intValue);
				}
			}
		}
		return null;
	}

	// return null if not found
	public String getForeignKeyValue(
			HashMap<String, ArrayList<CodeNamePair>> in_instance,
			String asso, String key_value, String srcDst, String srcDst1) {
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> instance : this.allInstances
				.entrySet()) {
			if (instance.getValue().equals(in_instance)) {
				for (Map.Entry<String, ArrayList<CodeNamePair>> s_inst : instance
						.getValue().entrySet()) {
					// the key is instance name, such as
					// CustomerOrderAssociation$0
					// and the value is the list of field and value
					if (asso.equalsIgnoreCase(instance.getKey())) {
						// if the list has srcDst and key value, then search the
						// srcDst1 and return its value
						boolean hasValueAndSrcDst = false;

						for (CodeNamePair pair : s_inst.getValue()) {
							if (pair.getFirst().split("_")[1]
									.equalsIgnoreCase(srcDst)
									&& pair.getSecond().equalsIgnoreCase(
											key_value)) {
								hasValueAndSrcDst = true;
								break;
							}
						}
						if (hasValueAndSrcDst) {
							for (CodeNamePair pair : s_inst.getValue()) {
								if (pair.getFirst().split("_")[1]
										.equalsIgnoreCase(srcDst1)) {
									return pair.getSecond();
								}
							}
						} else {
							return null;
						}
					}
				}
			}
		}
		return null;
	}

	public boolean isNumeric(String str) {
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition(0);
		formatter.parse(str, pos);
		return str.length() == pos.getIndex();
	}

	public String getPrimaryKeyByTableName(
			HashMap<String, ArrayList<CodeNamePair>> scheme,
			String tableName) {
		ArrayList<CodeNamePair> table = scheme.get(tableName);
		for (CodeNamePair pair : table) {
			if (pair.getFirst().equalsIgnoreCase("primaryKey")) {
				return pair.getSecond();
			}
		}
		return "";
	}

	public HashMap getAssByKey(String scheme, String pTable, String fTable) {
		HashMap<String, CodeNamePair> ass_map = new HashMap<String, CodeNamePair>();
		String src = "";
		String dst = "";
		String ass = "";
		HashMap<String, ArrayList<CodeNamePair>> single_scheme = this.schemas
				.get(scheme);
		for (Map.Entry<String, ArrayList<CodeNamePair>> entry : single_scheme
				.entrySet()) {
			for (CodeNamePair pair : entry.getValue()) {
				if (pair.getFirst().equalsIgnoreCase("src")) {
					if (pair.getSecond().equalsIgnoreCase(pTable)) {
						src = pTable;
					}
					if (pair.getSecond().equalsIgnoreCase(fTable)) {
						src = fTable;
					}
				}
				if (pair.getFirst().equalsIgnoreCase("dst")) {
					if (pair.getSecond().equalsIgnoreCase(pTable)) {
						dst = pTable;
					}
					if (pair.getSecond().equalsIgnoreCase(fTable)) {
						dst = fTable;
					}
				}
			}
			if (src.length() > 0 && dst.length() > 0) {
				ass = entry.getKey();
				CodeNamePair pair = new CodeNamePair(src, dst);
				ass_map.put(ass, pair);
				return ass_map;
			}
		}
		return null;
	}

	public ArrayList<String> getForeignKey(String schema, String element) {
		ArrayList<String> list = new ArrayList<String>();
		for (Map.Entry<String, ArrayList<CodeNamePair>> entry : this.foreignKeys
				.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(schema)) {
				for (CodeNamePair table_key_pair : entry.getValue()) {
					if (table_key_pair.getFirst().equalsIgnoreCase(element)) {
						list.add(table_key_pair.getSecond());
					}
				}
			}
		}
		return list;
	}

	// looks up reverse t_associate data structure to find a target table for
	// each object element, e.g. a class instance or an association
	public String getTableNameByElement(String schema, String element) {
		ArrayList<CodeNamePair> entry = this.reverseTAss.get(schema);
		if (entry != null) {
			for (CodeNamePair pair : entry) {
				if (pair.getFirst().equalsIgnoreCase(element)) {
					return pair.getSecond();
				}
			}
		}
		return "";
	}

	public void addInsertStmtIntoDataSchema(String dataSchema,
			String tableName, String stmt, int idValue) {
		boolean contains = this.allInsertStmts.containsKey(dataSchema);
		if (!contains) {
			this.allInsertStmts.put(dataSchema,
					new HashMap<String, HashMap<Integer, String>>());
			this.allInsertStmts.get(dataSchema).put(tableName,
					new HashMap<Integer, String>());
		}
		if (!this.allInsertStmts.get(dataSchema).containsKey(tableName)) {
			this.allInsertStmts.get(dataSchema).put(tableName,
					new HashMap<Integer, String>());
		}
		this.allInsertStmts.get(dataSchema).get(tableName).put(idValue, stmt);
	}

	public boolean dataSchemaHasInsertStatement(String dataSchema,
			String tableName, int idValue) {
		if (this.allInsertStmts.containsKey(dataSchema)) {
			if (this.allInsertStmts.get(dataSchema).containsKey(tableName)) {
				if (this.allInsertStmts.get(dataSchema).get(tableName)
						.containsKey(idValue)) {
					return true;
				}
			}
		}
		return false;
	}

	public HashMap getInstancesByTable(String tableName) {
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : this.allInstances
				.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(tableName)) {
				return entry.getValue();
			}
		}
		return null;
	}

	public boolean childIsTable(String dataSchema, String child) {
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : this.schemas
				.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(dataSchema)) {
				HashMap<String, ArrayList<CodeNamePair>> value = entry
						.getValue();
				for (Map.Entry<String, ArrayList<CodeNamePair>> single_table : value
						.entrySet()) {
					if (single_table.getKey().equalsIgnoreCase(child)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean printed(String tableName) {
		for (String s : this.printed) {
			if (s.equalsIgnoreCase(tableName)) {
				return true;
			}
		}
		return false;
	}

	// public boolean hasInstanceForTable(String tableName) {
	// Collection<HashMap<String, ArrayList<CodeNamePair>>> instTables =
	// this.tables.values();
	// for (HashMap<String, ArrayList<CodeNamePair>> instTable :
	// instTables) {
	// for (Map.Entry<String, ArrayList<CodeNamePair>> inst :
	// instTable.entrySet()) {
	// String key = inst.getKey();
	// if (key.equalsIgnoreCase(tableName)) {
	// return true;
	// }
	// }
	// }
	// return false;
	// }

	public ArrayList<String> getChildren(String schemaName, String tableName) {
		ArrayList<String> children = new ArrayList<String>();
		for (Map.Entry<String, ArrayList<CodeNamePair>> entry : this.parents
				.entrySet()) {
			String key = entry.getKey();
			if (key.equalsIgnoreCase(schemaName)) {
				ArrayList<CodeNamePair> value = entry.getValue();
				for (CodeNamePair pair : value) {
					String parent = pair.getSecond();
					if (parent.equalsIgnoreCase(tableName)) {
						children.add(pair.getFirst());
					}
				}
			}
		}
		return children;
	}

	public String getIDBySigName(String sigName) {
		String pk = "";
		for (Sig s : sigs) {
			if (s.sigName.equalsIgnoreCase(sigName)) {
				return s.id;
			}
		}
		return pk;
	}

	public void randomInstanceGenerator(int i, int range) {
		if (isDebugOn) {
			System.out.println("Enter randomInstanceGenerator()"
					+ getCurrentTime());
		}
		// int range = 1000000;
		this.allInstances.clear();
		for (; i < range; i++) {
			for (Sig sig : this.sigs) {
				String sigName = sig.sigName;
				String instanceName = sigName + i;
				if (!this.allInstances.containsKey(sigName)) {
					this.allInstances
							.put(sigName,
									new HashMap<String, ArrayList<CodeNamePair>>());
				}
				if (!this.allInstances.get(sigName).containsKey(instanceName)) {
					this.allInstances.get(sigName).put(instanceName,
							new ArrayList<CodeNamePair>());
				}
				if (sig.category == 0) { // 0 is class
					String id = sig.id;
					String fieldValue = String.valueOf(i);
					this.allInstances
							.get(sigName)
							.get(instanceName)
							.add(new CodeNamePair(sigName + "_" + id,
									fieldValue));
					for (String fieldName : sig.attrSet) {
						if (fieldName.equalsIgnoreCase(id)) {
							continue;
						}
						String fieldType = typeList.get(fieldName);
						if (fieldType.equalsIgnoreCase("Integer")) {
							// assign random value
							Random rand = new Random(System.currentTimeMillis());
							fieldValue = String.valueOf(rand.nextInt(1000));
						} else if (fieldType.equalsIgnoreCase("string")) {
							fieldValue = fieldName
									+ UUID.randomUUID().toString()
											.substring(0, 6);
						} else if (fieldType.equalsIgnoreCase("Bool")) {
							// assign it as true
							fieldValue = "true";
						}
						this.allInstances
								.get(sigName)
								.get(instanceName)
								.add(new CodeNamePair(sigName + "_"
										+ fieldName, fieldValue));
					}
				} else if (sig.category == 1) { // 0 is association
					Random rand = new Random(System.currentTimeMillis());
					// int numOfAss = rand.nextInt(3);
					// for (int j = 0; j <= numOfAss; j++) {
					String srcIDName = getIDBySigName(sig.src);
					int srcIDValue = i + rand.nextInt(range - i);
					String dstIDName = getIDBySigName(sig.dst);
					// int dstIDValue = rand.nextInt(range);
					int dstIDValue = i + rand.nextInt(range - i);
					this.allInstances
							.get(sigName)
							.get(instanceName)
							.add(new CodeNamePair(sigName + "_"
									+ srcIDName, String.valueOf(srcIDValue)));
					this.allInstances
							.get(sigName)
							.get(instanceName)
							.add(new CodeNamePair(sigName + "_"
									+ dstIDName, String.valueOf(dstIDValue)));
					// }
				}
			}
		}
		if (isDebugOn) {
			System.out.println("Leave randomInstanceGenerator()"
					+ getCurrentTime());
		}
	}

	public void randomSelectGenarator(int i, int range) {
		if (isDebugOn) {
			System.out.println("Enter randomSelectGenarator()"
					+ getCurrentTime());
		}
		String dbSchemaFile = "";
		// create printwriters for data schemas
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
				.entrySet()) {
			dbSchemaFile = entry.getKey(); // this file include .sql extension
			String selectFile = dbSchemaFile.substring(0,
					dbSchemaFile.length() - 4) + "_select.sql";
			try {
				FileWriter fw = new FileWriter(selectFile, true);
				PrintWriter pw = new PrintWriter(fw);
				this.selectPrintWriters.put(selectFile, pw);
			} catch (IOException e) {
				e.printStackTrace(); // To change body of catch statement use
										// File | Settings | File Templates.
			}
		}

		ArrayList<String> fields = null;
		ArrayList<String> coveredFields = null;
		ArrayList<String> fieldsPlusIDs = null;

		// ArrayList<ArrayList<String>> queryFileds = new
		// ArrayList<ArrayList<String>>();
		for (; i < range; i++) {
			fields = new ArrayList<String>();
			int size = allFields.get(dbSchemaFile).size();
			for (int j = 0; j < 3; j++) {
				Random rand1 = new Random(System.currentTimeMillis());
				int value1 = rand1.nextInt(size);
				String tempfield = allFields.get(dbSchemaFile).get(value1);
				// force to have different fields
				allFields.get(dbSchemaFile).set(value1,
						allFields.get(dbSchemaFile).get(size - 1));
				allFields.get(dbSchemaFile).set(size - 1, tempfield);
				size--;
				if (!fields.contains(tempfield)) {
					fields.add(tempfield);
				}

			}
			// fieldsPlusIDs = new ArrayList<String>(fields);

			for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
					.entrySet()) {
				dbSchemaFile = entry.getKey(); // this file include .sql
												// extension

				String insertFile = dbSchemaFile.substring(0,
						dbSchemaFile.length() - 4)
						+ "_select.sql";
				PrintWriter pw = this.selectPrintWriters.get(insertFile); // findWriterByPath(insertFile);

				coveredFields = new ArrayList<String>();
				fieldsPlusIDs = new ArrayList<String>(fields);
				// / Here we add the id related to each field to the tableFields
				// list for the reason of joining.
				for (String f : fields) {
					String fieldID = getIDByField(dbSchemaFile, f);
					if ((!fieldID.equalsIgnoreCase(f))
							&& !fieldsPlusIDs.contains(fieldID))
						fieldsPlusIDs.add(fieldID);
				}

				// ArrayList<String> tables = new ArrayList<String>();
				ArrayList<String> coveredTables = new ArrayList<String>();
				ArrayList<CodeNamePair> unCoveredFields = new ArrayList<CodeNamePair>(
						fieldsTable.get(dbSchemaFile));
				// ArrayList<String> unCoveredFields = new
				// ArrayList<String>(tableFields);
				// table_name -> # of tableFields covered by it
				// HashMap<String, ArrayList<String>> tableFieldsCovered = new
				// HashMap<String, ArrayList<String>>();

				// for (CodeNamePair field_table : unCoveredFields) {
				int iterator = unCoveredFields.size() - 1;
				for (; iterator >= 0; iterator--) {
					CodeNamePair field_table = unCoveredFields
							.get(iterator);
					// if (!tableFields.contains(field_table.getFirst()))
					if (!fieldsPlusIDs.contains(field_table.getFirst()))
						unCoveredFields.remove(field_table);
				}

				ArrayList<String> tables = new ArrayList<String>();
				for (CodeNamePair field_table : unCoveredFields) {
					String tableName = field_table.getSecond();
					if (!tables.contains(tableName))
						tables.add(tableName);
				}

				// ArrayList<CodeNamePair> fieldsCoveredByTable = new
				// ArrayList<CodeNamePair>();
				HashMap<String, String> fieldsCoveredByTable = new HashMap<String, String>();
				do {
					HashMap<String, ArrayList<String>> tableFieldsCovered = new HashMap<String, ArrayList<String>>();
					// initialize the tableFieldsCovered
					for (String tableName : tables)
						// tableFieldsCovered.put(tableName, 0);
						tableFieldsCovered.put(tableName,
								new ArrayList<String>());

					// fill the tableFieldsCovered
					for (String tableName : tables) {
						// for (CodeNamePair field_table :
						// fieldsTable.get(dbSchemaFile)) {
						for (CodeNamePair field_table : unCoveredFields) {
							String curTable = field_table.getSecond();
							if (curTable.equalsIgnoreCase(tableName))// &&
																		// tableFields.contains(curTable))
								// tableFieldsCovered.put(tableName,
								// tableFieldsCovered.get(tableName) + 1);
								tableFieldsCovered.get(tableName).add(
										field_table.getFirst());
						}
					}

					// find the most covered table
					String maxTable = null;
					int maxValue = 0;
					for (Map.Entry<String, ArrayList<String>> table : tableFieldsCovered
							.entrySet()) {
						String tableName = table.getKey();
						Integer value = table.getValue().size();
						if (value > maxValue) {
							maxValue = value;
							maxTable = tableName;
						}
					}
					coveredTables.add(maxTable);
					tables.remove(maxTable);
					for (String field : tableFieldsCovered.get(maxTable)) {
						fieldsCoveredByTable.put(field, maxTable);
					}

					// remove tableFields covered by the most covered table
					// for (CodeNamePair field_table :
					// fieldsTable.get(dbSchemaFile)) {
					// for (CodeNamePair field_table : unCoveredFields)
					// {
					// if (field_table.getSecond().equalsIgnoreCase(maxTable))
					// unCoveredFields.remove(field_table.getSecond());
					// }

					ArrayList<String> currentCoveredFields = tableFieldsCovered
							.get(maxTable);
					iterator = unCoveredFields.size() - 1;
					for (; iterator >= 0; iterator--) {
						CodeNamePair field_table = unCoveredFields
								.get(iterator);
						String fieldName = field_table.getFirst();
						if (currentCoveredFields.contains(fieldName)
								&& fields.contains(fieldName))
							unCoveredFields.remove(field_table);
					}

					for (String fieldName : fields) {
						if (currentCoveredFields.contains(fieldName)
								&& !coveredFields.contains(fieldName))
							coveredFields.add(fieldName);
					}

					// } while (unCoveredFields.size() > 0);
				} while (!coveredFields.containsAll(fields));

				String fieldsStatement = "`"
						+ fieldsCoveredByTable.get(fields.get(0)) + "`" + ".`"
						+ fields.get(0) + "`";
				int j = 1;
				while (j < fields.size()) {
					fieldsStatement += ", `"
							+ fieldsCoveredByTable.get(fields.get(j)) + "`"
							+ ".`" + fields.get(j) + "`";
					j++;
				}
				// fieldsStatement += ", COUNT(*)";
				String fromStatement = " FROM `" + coveredTables.get(0) + "`";
				j = 1;
				while (j < coveredTables.size()) {
					fromStatement += ", `" + coveredTables.get(j) + "`";
					j++;
				}
				// String s = "SELECT " + fieldsStatement + fromStatement + ";";
				String s = "SELECT " + fieldsStatement + fromStatement; //

				// where field_1 operator a number [or a field_2]
				String whereStatement = " WHERE ";
				Random rand = new Random(System.currentTimeMillis());

				// Select a field randomly for where clause
				// int value = rand.nextInt(fields.size());
				// String fieldInWhere = fields.get(value);
				// whereStatement += "`" + fieldInWhere + "`";

				// Select a field from the id fields for where clause
				int value = rand.nextInt(coveredTables.size());
				String tableInWhere = coveredTables.get(value);
				String fieldInWhere = getPrimaryKeyByTableName(
						this.schemas.get(dbSchemaFile), tableInWhere);
				whereStatement += "`" + tableInWhere + "`" + ".`"
						+ fieldInWhere + "`";

				// String[] operands = {"<=", "<", "=", ">", ">="};
				// value = rand.nextInt(operands.length);
				// String operand = operands[value];
				String operand = "=";
				// whereStatement += " " + operand;

				boolean isID = isID(fieldInWhere);
				String comparedTo = "";
				String type = getFieldType(dbSchemaFile, fieldInWhere);
				if (type.equalsIgnoreCase("Bool")) {
					value = rand.nextInt(2);
					if (value > 1)
						comparedTo = "true";
					else
						comparedTo = "false";
					whereStatement += " = " + comparedTo;
				} else if (type.equalsIgnoreCase("string")) {
					String randString = "\"Dtype\"";
					whereStatement += " = " + randString;
				} else if (type.equalsIgnoreCase("Integer")) {
					int randValue = rand.nextInt(1000);
					if (isID) {
						rand = new Random(System.currentTimeMillis());
						randValue = rand.nextInt(range);
					}
					whereStatement += operand + randValue;
				} else if (type.equalsIgnoreCase("Real")) {
					int randValue = rand.nextInt(1000);
					whereStatement += operand + randValue;
				} else if (type.equalsIgnoreCase("DType")) {
					int randValue = rand.nextInt(coveredTables.size());
					whereStatement += " = " + coveredTables.get(randValue);
				}

				s += whereStatement + " LIMIT 1";
				s += ";";
				pw.println(s);
			}
		}
		for (Map.Entry<String, PrintWriter> pw : this.selectPrintWriters
				.entrySet()) {
			pw.getValue().close();
		}
		if (isDebugOn) {
			System.out.println("Leave randomSelectGenarator()"
					+ getCurrentTime());
		}
	}

	/**
	 * This method will be used to parse all data in XML to internal data
	 * structure
	 */
	public void parseDocument(String instFile) {
		// if (isRandom == true) {
		// randomInstanceGenerator();
		// return;
		// }
		this.allInstances.clear();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(instFile);
			// get the root element
			Element docEle = dom.getDocumentElement();
			// get instance
			Node instance = docEle.getFirstChild();
			// get a nodelist of elements
			NodeList signodes = docEle.getElementsByTagName("sig");

			// handle sig nodes
			// there maybe multiple instances for one table, for each instance,
			// we create a Arraylist<CodeNamePair> for it
			for (int i = 0; i < signodes.getLength(); i++) {
				Node node = signodes.item(i);
				Element tmpElement = (Element) node;
				if (node.hasAttributes()) {
					// Element element = (Element) node;
					String table_name = tmpElement.getAttribute("label");
					if (table_name.equalsIgnoreCase("univ")
							|| table_name.equalsIgnoreCase("int")
							|| table_name.equalsIgnoreCase("string")
							|| table_name.contains("/")) {
						continue;
					} else {
						ArrayList<CodeNamePair> single_table;
						HashMap<String, ArrayList<CodeNamePair>> instances = new HashMap<String, ArrayList<CodeNamePair>>();
						NodeList multiple_instances = tmpElement
								.getElementsByTagName("atom");
						int instances_num = multiple_instances.getLength();
						for (int j = 0; j < instances_num; j++) {
							Element single_instance = (Element) multiple_instances
									.item(j);
							String s_instance_name = single_instance
									.getAttribute("label");
							single_table = new ArrayList<CodeNamePair>();
							instances.put(s_instance_name, single_table);
						}
						this.allInstances.put(table_name, instances);
					}
				}
			}

			// handle field nodes
			NodeList fieldnodes = docEle.getElementsByTagName("field");

			for (int i = 0; i < fieldnodes.getLength(); i++) {
				Node node = fieldnodes.item(i);
				if (node.hasAttributes()) {
					Element element = (Element) node;
					String field = element.getAttribute("label");
					// if (node.hasAttributes()) {
					// Element element = (Element) node;
					NodeList children = element.getElementsByTagName("tuple");
					for (int j = 0; j < children.getLength(); j++) {
						Node child = children.item(j);
						if (child.hasChildNodes()) { // for every <tuple>
														// tag
							Element singleTuple = (Element) child;
							// get the first part
							String instanceName = getSingleAtom(singleTuple, 0);
							// get the second part
							String value = getSingleAtom(singleTuple, 1); 
							// get table name
							String table_name = instanceName.split("\\$")[0];
							this.allInstances.get(table_name).get(instanceName)
									.add(new CodeNamePair(field, value));
						}
					}
					// }
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public String getSingleAtom(Element singleTuple, int i) {
		NodeList atoms = singleTuple.getElementsByTagName("atom");
		Element tableCode = (Element) atoms.item(i);
		String code = tableCode.getAttribute("label");
		String[] tmp = code.split("/");
		code = tmp[tmp.length - 1];
		return code;
	}
}
