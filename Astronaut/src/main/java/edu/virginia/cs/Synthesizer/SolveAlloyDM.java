package edu.virginia.cs.Synthesizer;

import edu.virginia.cs.AppConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class SolveAlloyDM {

    private final Boolean isDebugOn = AppConfig.getDebug();
    // HashMap<String, ArrayList<CodeNamePair>> fields = new
    // HashMap<String, ArrayList<CodeNamePair>>();
    private final int intScope;
    public final HashMap<String, HashMap<String, ArrayList<CodeNamePair>>> allInstances = new HashMap<>();
    // public HashMap<String, HashMap<String, HashMap<String, String>>>
    // allInsertStmts = new HashMap<String, HashMap<String, HashMap<String,
    // String>>>();
    public final HashMap<String, HashMap<String, HashMap<Integer, String>>> allInsertStmts = new HashMap<>();
    // public HashMap<String, HashMap<String, HashMap<String, String>>> allUpdateStmts = new HashMap<>();
    // public HashMap<String, HashMap<String, HashMap<String, String>>>
    // allSelectStmts = new HashMap<String, HashMap<String, HashMap<String,
    // String>>>();
    public final HashMap<String, HashMap<String, ArrayList<String>>> allSelectStmts = new HashMap<>();
    public String insertFile = null;
    public String selectFile = null;
    /**
     * Use this method to output the data in inner data structure into sql file
     */
    final HashMap<String, HashMap<String, ArrayList<CodeNamePair>>> schemas;
    final HashMap<String, ArrayList<CodeNamePair>> parents;
    // store the FilePrinter of data schemas
    final HashMap<String, PrintWriter> insertPrintWriters = new HashMap<>();
    // HashMap<String, PrintWriter> insertInstantPrintWriters = new
    // HashMap<String, PrintWriter>();
    // HashMap<String, PrintWriter> updatePrintWriters = new HashMap<>();
    // HashMap<String, PrintWriter> updateInstantPrintWriters = new
    // HashMap<String, PrintWriter>();
    final HashMap<String, PrintWriter> selectPrintWriters = new HashMap<>();
    final HashMap<String, ArrayList<CodeNamePair>> reverseTAss;
    final HashMap<String, ArrayList<CodeNamePair>> foreignKeys;
    final HashMap<String, HashMap<String, CodeNamePair>> associations;
    final HashMap<String, ArrayList<CodeNamePair>> primaryKeys;
    final HashMap<String, ArrayList<String>> printOrder = new HashMap<>();
    // HashMap<String, HashMap<String, HashSet<String>>> existingID = new HashMap<>();
    final HashMap<String, ArrayList<String>> allFields;
    final HashMap<String, ArrayList<CodeNamePair>> fieldsTable;
    final HashMap<String, ArrayList<CodeNamePair>> tableFields;
    final HashMap<String, ArrayList<CodeNamePair>> fieldType;
    final ArrayList<String> ids;
    final ArrayList<String> assList;
    final HashMap<String, String> typeList;
    final ArrayList<Sig> sigs;
    final HashSet<String> childrenInOM = new HashSet<>();

    public SolveAlloyDM(HashMap<String, HashMap<String, ArrayList<CodeNamePair>>> schemas, HashMap<String, ArrayList<CodeNamePair>> parents, HashMap<String, ArrayList<CodeNamePair>> reverseTAss,
                        HashMap<String, ArrayList<CodeNamePair>> foreignKeys, HashMap<String, HashMap<String, CodeNamePair>> associations, HashMap<String, ArrayList<CodeNamePair>> primaryKeys,
                        HashMap<String, ArrayList<CodeNamePair>> tableFields, HashMap<String, ArrayList<String>> allFields, HashMap<String, ArrayList<CodeNamePair>> fieldsTable,
                        HashMap<String, ArrayList<CodeNamePair>> fieldType, ArrayList<String> ids, ArrayList<String> assList, int intScope,
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

    public String getCurrentTime() {
        // DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        return "--------" + dateFormat.format(date);
    }

    /**
     * Output all statements in memory to disk
     *
     * @param i : the caller 0: from synthesizer 1: from random generator
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
            String bmFolder;
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
                dbName = dbName.substring(dbName.lastIndexOf(File.separator) + 1);
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

            String bmFolder = dbSchemaFile.substring(0,
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
                dbName = dbName.substring(dbName.lastIndexOf(File.separator) + 1);
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
        StringBuilder selectPart;
        String fromPart;
        StringBuilder wherePart;
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
                    selectPart = new StringBuilder("SELECT ");
                    fromPart = " FROM ";
                    wherePart = new StringBuilder(" WHERE ");
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
                                selectPart.append("`").append(element).append("`.`").append(field).append("`,");
                                if (isPrimaryKeys(dbScheme, element, field)) {
                                    String value = getFieldValue(
                                            instance.getValue(), field);
                                    wherePart.append("`").append(element).append("`.`").append(field).append("`=").append(value).append(" AND ");
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
                                selectPart.append("`").append(element).append("`.`").append(field).append("`,");
                                if (isPrimaryKeys(dbScheme, element, field)) {
                                    String value = getFieldValue(
                                            instance.getValue(), field);
                                    wherePart.append("`").append(element).append("`.`").append(field).append("`=").append(value).append(" AND ");
                                }
                            }
                        }
                    }
                    selectPart = new StringBuilder(selectPart.substring(0,
                            selectPart.length() - 1));
                    wherePart = new StringBuilder(wherePart.substring(0, wherePart.length() - 5));
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

    public void generateInsert() {
        // this.allInsertStmts.clear();
        StringBuilder field_part;
        StringBuilder value_part;
        String element;
        String dbScheme;
        String goToTable;
        ArrayList<CodeNamePair> allAboutOMClass;
        ArrayList<String> fTables;
        ArrayList<HashMap<String, CodeNamePair>> associations = new ArrayList<>();
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
                    field_part = new StringBuilder();
                    value_part = new StringBuilder();
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
                                field_part.append("`").append(field).append("`,");
                                value_part.append(value).append(",");
                            } else {
                                if (field.equalsIgnoreCase("DType")) {
                                    value = "'" + element + "'";
                                    field_part.append("`").append(field).append("`,");
                                    value_part.append(value).append(",");
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
                                                String para;
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
                                                field_part.append("`").append(field).append("`,");
                                                value_part.append(fValue).append(",");
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
                    field_part = new StringBuilder(field_part.substring(0,
                            field_part.length() - 1));
                    value_part = new StringBuilder(value_part.substring(0,
                            value_part.length() - 1));
                    String stmt = "INSERT INTO `" + goToTable + "` ("
                            + field_part + ") VALUES (" + value_part + ");";
                    stmt += "FLUSH TABLES;";
                    // System.out.println("Check inserts");
                    if (!dataSchemaHasInsertStatement(dbScheme, goToTable,
                            Integer.parseInt(id_value))) {
                        // System.out.println("Finish check inserts");
                        addInsertStmtIntoDataSchema(dbScheme, goToTable, stmt,
                                Integer.parseInt(id_value));
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
        HashSet<String> table = new HashSet<>();
        this.childrenInOM.clear();
        table.add(getTableNameByElement(dbScheme, className));
        this.getChildrenOM(className);
        for (String child : childrenInOM) {
            String elementTable = getTableNameByElement(dbScheme, child);
            table.add(elementTable);
        }
        return table.size();
    }

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

    public void getOutPutOrders(String instFile) {
        String pattern = Pattern.quote(System.getProperty("file.separator"));
        String[] instFiles = instFile.split(pattern);
        String fileName = instFiles[instFiles.length - 1];

        if (fileName.contains("customer")) {
            // init print order
            for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
                    .entrySet()) {
                String dbSchemaFile = entry.getKey(); // this file include .sql
                // extension
                this.printOrder.put(dbSchemaFile, new ArrayList<>());
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
                this.printOrder.put(dbSchemaFile, new ArrayList<>());
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
                this.printOrder.put(dbSchemaFile, new ArrayList<>());
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
                this.printOrder.put(dbSchemaFile, new ArrayList<>());
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
                this.printOrder.put(dbSchemaFile, new ArrayList<>());
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
                this.printOrder.put(dbSchemaFile, new ArrayList<>());
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
                this.printOrder.put(dbSchemaFile, new ArrayList<>());
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
                this.printOrder.put(dbSchemaFile, new ArrayList<>());
                this.printOrder.get(dbSchemaFile).add("Response");
            }
        } else { // this is the default case, for any other object model, the
            // print order is empty string list
            for (Map.Entry<String, HashMap<String, ArrayList<CodeNamePair>>> entry : schemas
                    .entrySet()) {
                String dbSchemaFile = entry.getKey(); // this file include .sql
                // extension
                this.printOrder.put(dbSchemaFile, new ArrayList<>());
            }
        }
    }

    public void addSelectStmtIntoDataSchema(String dataSchema,
                                            String tableName, String stmt) {
        boolean contains = this.allSelectStmts.containsKey(dataSchema);
        if (!contains) {
            this.allSelectStmts.put(dataSchema,
                    new HashMap<>());
            this.allSelectStmts.get(dataSchema).put(tableName,
                    new ArrayList<>());
        }
        if (!this.allSelectStmts.get(dataSchema).containsKey(tableName)) {
            this.allSelectStmts.get(dataSchema).put(tableName,
                    new ArrayList<>());
        }
        this.allSelectStmts.get(dataSchema).get(tableName).add(stmt);
    }

    public ArrayList<String> getTablesByPrimaryKey(String scheme,
                                                   String primaryKey) {
        ArrayList<String> tables = new ArrayList<>();
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
        String value;
        for (CodeNamePair pair : instance) {
            if (pair.getFirst().split("_")[1].equalsIgnoreCase(field)) {
                String tmp = pair.getSecond();
                if (isNumeric(tmp)) {
                    int intValue = Integer.parseInt(tmp);
                    intValue = intValue + (int) (Math.pow(2, (intScope - 1)))
                            + 1;
                    value = String.valueOf(intValue);
                    return value;
                }
            }
        }
        return null;
    }

    public String getForeignKeyValue(
            HashMap<String, ArrayList<CodeNamePair>> in_instance,
            String key_value, String srcDst, String srcDst1) {
        for (Map.Entry<String, ArrayList<CodeNamePair>> instance : in_instance
                .entrySet()) {
            for (CodeNamePair pair : instance.getValue()) {
                if (pair.getFirst().split("_")[1].equalsIgnoreCase(srcDst1)) {
                    int intValue = Integer.parseInt(pair.getSecond());
                    intValue = intValue + (int) (Math.pow(2, (intScope - 1)))
                            + 1;
                    return String.valueOf(intValue);
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

    public HashMap<String, CodeNamePair> getAssByKey(String scheme, String pTable, String fTable) {
        HashMap<String, CodeNamePair> ass_map = new HashMap<>();
        String src = "";
        String dst = "";
        String ass;
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
                    new HashMap<>());
            this.allInsertStmts.get(dataSchema).put(tableName,
                    new HashMap<>());
        }
        if (!this.allInsertStmts.get(dataSchema).containsKey(tableName)) {
            this.allInsertStmts.get(dataSchema).put(tableName,
                    new HashMap<>());
        }
        this.allInsertStmts.get(dataSchema).get(tableName).put(idValue, stmt);
    }

    public boolean dataSchemaHasInsertStatement(String dataSchema,
                                                String tableName, int idValue) {
        if (this.allInsertStmts.containsKey(dataSchema)) {
            if (this.allInsertStmts.get(dataSchema).containsKey(tableName)) {
                return this.allInsertStmts.get(dataSchema).get(tableName)
                        .containsKey(idValue);
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
                                    new HashMap<>());
                }
                if (!this.allInstances.get(sigName).containsKey(instanceName)) {
                    this.allInstances.get(sigName).put(instanceName,
                            new ArrayList<>());
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

}
