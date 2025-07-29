package edu.virginia.cs.Synthesizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import edu.virginia.cs.Framework.Types.AbstractLoad;
import edu.virginia.cs.Framework.Types.AbstractQuery;
import edu.virginia.cs.Framework.Types.AbstractQuery.Action;
import edu.virginia.cs.Framework.Types.ObjectOfDM;
import edu.virginia.cs.Framework.Types.ObjectSet;

public class LoadSynthesizer {
	private Boolean isDebugOn = AppConfig.getDebug();

	public HashMap<String, String> globalNegation = new HashMap<String, String>();
	public ArrayList<String> ids = new ArrayList<String>();
	public HashMap<String, HashMap<String, ArrayList<CodeNamePair>>> allInstances = new HashMap<String, HashMap<String, ArrayList<CodeNamePair>>>();
	boolean isFinished = false;
	public int solutionNo = 1;
	
	
	public void randomInstanceCreator(){
		
	}

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
		String negation = "";
		String factName = "";
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
				getNegation(object);
				PrintStream ps = new PrintStream(new FileOutputStream(new File(
						model), true));
				factName = "fact_" + factNum;
				factNum++;
				negation = System.getProperty("line.separator") + "fact "
						+ factName + " {"
						+ System.getProperty("line.separator");
				for (Entry<String, String> s_negation : this.globalNegation
						.entrySet()) {
					negation += s_negation.getKey()
							+ System.getProperty("line.separator");
				}
				negation += "}";
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
	 * 
	 * @param model
	 * @param solutions
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

	public ArrayList<AbstractLoad> genAbsLoads(ObjectSet objSet) {
		ArrayList<AbstractLoad> loads = new ArrayList<AbstractLoad>();
		loads.add(genInsertLoad(objSet));
		loads.add(genSelectLoad(objSet));
		return loads;
	}

	private AbstractLoad genInsertLoad(ObjectSet objSet) {
		AbstractLoad insertLoads = new AbstractLoad();
		// iterate objects
		for (ObjectOfDM object : objSet.getObjSet()) {
			AbstractQuery aq = new AbstractQuery();
			aq.setAction(Action.INSERT);
			aq.setOodm(object);
			insertLoads.getQuerySet().add(aq);
		}
		return insertLoads;
	}

	private AbstractLoad genSelectLoad(ObjectSet objSet) {
		AbstractLoad selectLoads = new AbstractLoad();
		// iterate objects
		for (ObjectOfDM object : objSet.getObjSet()) {
			AbstractQuery aq = new AbstractQuery();
			aq.setAction(Action.SELECT);
			aq.setOodm(object);
			selectLoads.getQuerySet().add(aq);
		}
		return selectLoads;
	}

	public String getNegation(String xmlFile) {
		String negation = "";
		String forGlobalNegation = "";
		for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : this.allInstances
				.entrySet()) {
			String element = entry.getKey();
			for (Map.Entry<String, ArrayList<CodeNamePair>> instance : entry
					.getValue().entrySet()) {
				forGlobalNegation = "";
				forGlobalNegation = "no o:" + element + " | ";
				negation += "no o:" + element + " | ";
				ArrayList<CodeNamePair> allFields = instance.getValue();
				for (CodeNamePair fields : allFields) {
					String field = fields.getFirst();
					// check if field is ID or not
					if (isID(field.split("_")[1])) {
						String value = fields.getSecond();
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

}
