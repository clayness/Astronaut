package edu.virginia.cs.Synthesizer;

import edu.virginia.cs.AppConfig;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: ct4ew Date: 7/23/13 Time: 3:53 PM To change
 * this template use File | Settings | File Templates.
 */
public class DataProvider implements Serializable {
    // pairs is used to store all code and real name
    // for example, a table pair will be <Table$0, Customer>
    // a field pair will be <field$1, customerID>
    private List<CodeNamePair> pairs;
    private Map<String, List<CodeNamePair>> tableItems;
    private List<CodeNamePair> types;
    private List<CodeNamePair> parents;
    private Map<String, List<String>> allStmts = new HashMap<>();
    private List<String> orders = new ArrayList<>();
    private List<Sig> sigs;

    public DataProvider(List<Sig> sigs) {
        pairs = new ArrayList<>();
        tableItems = new HashMap<>();
        types = new ArrayList<>();
        parents = new ArrayList<>();
        this.sigs = sigs;
    }

    public DataProvider() {
        pairs = new ArrayList<>();
        tableItems = new HashMap<>();
        types = new ArrayList<>();
        parents = new ArrayList<>();
        // assert TableName.size() == TableItem.size();
    }

    public Map<String, List<CodeNamePair>> getTables() {
        return this.tableItems;
    }

    public List<CodeNamePair> getParents() {
        return this.parents;
    }

    public List<String> getAttrByTableName(String table) {
        List<String> attrs = new ArrayList<>();
        var it = this.tableItems.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            String tableName = entry.getKey();
            if (tableName.equalsIgnoreCase(table)) {
                for (CodeNamePair pair : entry.getValue()) {
                    if (pair.getFirst().equalsIgnoreCase("attr")) {
                        attrs.add(pair.getSecond());
                    }
                }
            }
        }

        return attrs;
    }

    public Boolean isClassAssociate(String className) {
        var it = this.tableItems.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            String tableName = entry.getKey();
            if (tableName.equalsIgnoreCase(className)) {
                for (CodeNamePair pair : entry.getValue()) {
                    if (pair.getFirst().equalsIgnoreCase("src")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<CodeNamePair> getTypes() {
        List<CodeNamePair> list = new ArrayList<>();
        for (CodeNamePair pair : this.types) {
            CodeNamePair tmp = new CodeNamePair(pair.getFirst()
                    .toString(), pair.getSecond().toString());
            list.add(tmp);
        }
        return list;
    }

    public List<CodeNamePair> getReverseIds() {
        List<CodeNamePair> list = new ArrayList<>();

        var tableItemIt = this.tableItems.entrySet().iterator();
        while (tableItemIt.hasNext()) {
            var entry = tableItemIt.next();
            String tableName = entry.getKey();
            List<CodeNamePair> pair = entry.getValue();
            for (CodeNamePair p : pair) {
                if (p.getFirst().equalsIgnoreCase("id")) {
                    list.add(new CodeNamePair(p.getSecond(), tableName));
                }
            }
        }
        return list;
    }

    public String addPair(String code, String name) {
        // check if same first existed
        for (int i = 0; i < this.pairs.size(); i++) {
            if (this.pairs.get(i).getFirst().toString().equalsIgnoreCase(code)) {
                if (code.startsWith("Table")) {
                    return this.pairs.get(i).getSecond().toString();
                }
            }
        }
        CodeNamePair myPair = new CodeNamePair(code, name);
        this.pairs.add(myPair);
        return "true";
    }

    public void addItem(String table, String key, String value) {
        if (this.tableItems.containsKey(table)) {
            CodeNamePair tmp = new CodeNamePair(key, value);
            this.tableItems.get(table).add(tmp);
        } else {
            List<CodeNamePair> tmpArray = new ArrayList<>();
            CodeNamePair tmpPair = new CodeNamePair(key, value);
            tmpArray.add(tmpPair);
            this.tableItems.put(table, tmpArray);
        }
    }

    public boolean addType(String filed, String type) {
        CodeNamePair newPair = new CodeNamePair(filed, type);
        if (this.types.contains(newPair)) {
            return false;
        } else {
            this.types.add(newPair);
        }
        return true;
    }

    public boolean addParent(String child, String parent) {
        CodeNamePair newPair = new CodeNamePair(child, parent);
        if (this.parents.contains(newPair)) {
            return false;
        } else {
            this.parents.add(newPair);
        }
        return true;
    }

    public String getSecondByFirst(String first) {
        String second = null;
        for (int i = 0; i < this.pairs.size(); i++) {
            if (this.pairs.get(i).getFirst().equals(first)) {
                second = this.pairs.get(i).getSecond().toString();
            }
        }
        return second;
    }

    /**
     * replace all items with $ symbol and add them to the table, then delete
     * all table items with $ symbol
     */
    public void refineTable(List<Sig> sigs) {
        Map<String, List<CodeNamePair>> tmpTableItems = new HashMap<>();
        var tableItemSet = this.tableItems.entrySet();
        // replace all items with $ symbol
        for (var entry : tableItemSet) {
            String tableName = entry.getKey();
            List<CodeNamePair> tableContents = entry.getValue();

            if (tableName.contains("$")) {
                tableName = getSecondByFirst(tableName);
                if (!tmpTableItems.containsKey(tableName)) {
                    tmpTableItems.put(tableName,
                            new ArrayList<>());
                }

                int arraySize = tableContents.size();
                for (int i = 0; i < arraySize; i++) {
                    String firstField = tableContents.get(i).getFirst()
                            .toString();
                    if (firstField.contains("$")) {
                        firstField = this.getSecondByFirst(firstField);
                    }
                    String secondField = tableContents.get(i).getSecond()
                            .toString();
                    if (secondField.contains("$")) {
                        secondField = this.getSecondByFirst(secondField);
                    }
                    // add new items into tmpTableItems
                    CodeNamePair tmpPair = new CodeNamePair(firstField,
                            secondField);
                    tmpTableItems.get(tableName).add(tmpPair);
                }
            } else {
                if (tmpTableItems.containsKey(tableName)) {
                    // add all items in this.tableItems in to tmpTableItems
                    for (int i = 0; i < tableContents.size(); i++) {
                        tmpTableItems.get(tableName).add(tableContents.get(i));
                    }
                } else {
                    tmpTableItems.put(tableName, tableContents);
                }
            }
        }

        // // handle parent here
        // for (int i = 0; i < this.parents.size(); i++) {
        // String tableName = this.parents.get(i).getFirst().toString();
        // if (tableName.contains("$")) {
        // String tmpTableName = getPairSecond(tableName);
        // this.parents.get(i).setFirst(tmpTableName);
        // }
        // }
        // handle type here
        for (int i = 0; i < this.types.size(); i++) {
            String typeName = this.types.get(i).getFirst().toString();
            if (typeName.contains("$")) {
                this.types.get(i).setFirst(typeName.split("\\$")[0]);
            }
        }

        // remove pairs from parents which the child is an independent table
        for (int i = 0; i < this.pairs.size(); i++) {
            if (this.pairs.get(i).getFirst().toString().startsWith("Table")) {
                int j = 0;
                for (j = 0; j < this.parents.size(); j++) {
                    if (this.pairs
                            .get(i)
                            .getSecond()
                            .toString()
                            .equalsIgnoreCase(
                                    this.parents.get(j).getFirst().toString())) {
                        break;
                    }
                }
                if (j < this.parents.size()) {
                    this.parents.remove(j);
                }
            }
        }

        for (int i = 0; i < this.parents.size(); i++) {
            // change DType$0 in parent to DType
            List<CodeNamePair> tmpList1 = tmpTableItems
                    .get(this.parents.get(i).getSecond().toString());
            if (tmpList1 == null) {
                continue;
            }
            for (int j = 0; j < tmpList1.size(); j++) {
                String tmp = tmpList1.get(j).getSecond().toString();
                if (tmp.contains("$")) {
                    String tmp1 = tmp.split("\\$")[0];
                    tmpTableItems
                            .get(this.parents.get(i).getSecond().toString())
                            .get(j).setSecond(tmp1);
                    // tmpList1.get(i).setSecond(tmp1);
                }
            }

            List<CodeNamePair> tmpList = tmpTableItems
                    .get(this.parents.get(i).getFirst());
            for (int j = 0; j < tmpList.size(); j++) {
                if (!hasItemInArray(
                        tmpTableItems.get(this.parents.get(i).getSecond()),
                        tmpList.get(j))) {

                    tmpTableItems.get(this.parents.get(i).getSecond()).add(
                            tmpList.get(j));
                }
            }
            tmpTableItems.remove(this.parents.get(i).getFirst());
        }

        this.tableItems.clear();
        this.tableItems = tmpTableItems;
    }

    public boolean hasItemInArray(List<CodeNamePair> list,
                                  CodeNamePair pair) {
        boolean has = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getFirst().toString()
                    .equalsIgnoreCase(pair.getFirst().toString())
                    && list.get(i).getSecond().toString()
                    .equalsIgnoreCase(pair.getSecond().toString())) {
                return true;
            }
        }
        return has;
    }

    public CodeNamePair hasSrcDst(String tableName) {
        CodeNamePair srcdst = new CodeNamePair("", "");
        List<CodeNamePair> items = this.tableItems.get(tableName);
        for (CodeNamePair pair : items) {
            String tmp = pair.getFirst().toString();
            if (tmp.equalsIgnoreCase("src")) {
                srcdst.setFirst(pair.getSecond().toString());
            }
            if (tmp.equalsIgnoreCase("dst")) {
                srcdst.setSecond(pair.getSecond().toString());
            }
            if (srcdst.getFirst().length() > 0
                    && srcdst.getSecond().length() > 0) {
                return srcdst;
            }
        }
        return srcdst;
    }

    public CodeNamePair getMultiplicity(String tableName) {
        CodeNamePair srcdst_mul = new CodeNamePair("", "");
        List<CodeNamePair> items = this.tableItems.get(tableName);
        for (CodeNamePair pair : items) {
            String tmp = pair.getFirst().toString();
            if (tmp.equalsIgnoreCase("srcMul")) {
                srcdst_mul.setFirst(pair.getSecond().toString());
            }
            if (tmp.equalsIgnoreCase("dstMul")) {
                srcdst_mul.setSecond(pair.getSecond().toString());
            }
            if (srcdst_mul.getFirst().length() > 0
                    && srcdst_mul.getSecond().length() > 0) {
                return srcdst_mul;
            }
        }
        return srcdst_mul;
    }

    public List<String> getPrimaryKey(String tableName) {
        List<String> keys = new ArrayList<>();
        List<CodeNamePair> items = this.tableItems.get(tableName);
        for (CodeNamePair pair : items) {
            String tmp = pair.getFirst().toString();
            if (tmp.equalsIgnoreCase("primaryKey")) {
                keys.add(pair.getSecond().toString());
            }
        }
        return keys;
    }

    public boolean isForeignKey(String table, String field) {
        boolean isForeignKey = false;
        List<CodeNamePair> table1 = this.tableItems.get(table);

        for (int i = 0; i < table1.size(); i++) {
            if (table1.get(i).getFirst().toString()
                    .equalsIgnoreCase("foreignKey")) {
                if (table1.get(i).getSecond().toString()
                        .equalsIgnoreCase(field)) {
                    return true;
                }
            }
        }
        return isForeignKey;
    }

    public String getParent(String tableName) {
        for (CodeNamePair pair : this.parents) {
            if (pair.getFirst().toString().equalsIgnoreCase(tableName)) {
                return pair.getSecond().toString();
            }
        }
        return null;
    }

    /**
     * Find the table name that ID is its primaryKey and ID is not its
     * foreignKeyStr
     *
     * @param ID : input id
     * @return the table name
     */
    public String tableNameByID(String ID) {

        String tableName = "NULL";
        var tableItemSet = this.tableItems.entrySet();
        for (var entry : tableItemSet) {
            List<CodeNamePair> tmpArray = entry.getValue();
            int arraySize = tmpArray.size();
            for (int i = 0; i < arraySize; i++) {
                // if
                // (tmpArray.get(i).getFirst().toString().equalsIgnoreCase("attr"))
                // {
                if (tmpArray.get(i).getFirst().toString()
                        .equalsIgnoreCase("ID")) {
                    if (tmpArray.get(i).getSecond().toString()
                            .equalsIgnoreCase(ID)) {
                        // tableName = entry.getKey();
                        // System.out.println("Find Table: "+ tableName);
                        // check the ID is also in attr
                        for (int j = 0; j < arraySize; j++) {
                            if (tmpArray.get(j).getFirst()
                                    .equalsIgnoreCase("attr")) {
                                if (tmpArray.get(j).getSecond()
                                        .equalsIgnoreCase(ID)) {
                                    return entry.getKey();
                                }
                            }
                        }
                    }
                }
            }
        }

        return tableName;
    }

    public String hasParent(String child) {
        String parent = null;
        for (int i = 0; i < this.parents.size(); i++) {
            System.out.println(this.pairs.get(i).getFirst().toString() + "   "
                    + child);
            if (this.pairs.get(i).getFirst().toString().equals(child)) {
                parent = this.parents.get(i).getSecond().toString();
            }
        }
        return parent;
    }

    public int hasMultipleItem(String tableName, String item) {
        int key = 0;
        List<CodeNamePair> table1 = this.tableItems.get(tableName);

        for (int i = 0; i < table1.size(); i++) {
            if (table1.get(i).getFirst().toString().equalsIgnoreCase(item)) {
                key++;
            }
        }

        return key;
    }

    public boolean isID(String field, String table) {
        boolean isID = false;
        List<CodeNamePair> table1 = this.tableItems.get(table);

        for (int i = 0; i < table1.size(); i++) {
            // if (table1.get(i).getFirst().toString().equalsIgnoreCase("Id")) {
            if (table1.get(i).getFirst().toString()
                    .equalsIgnoreCase("primaryKey")) {
                if (table1.get(i).getSecond().toString()
                        .equalsIgnoreCase(field)) {
                    isID = true;
                    break;
                }
            }
        }
        return isID;
    }

    public String getTypesByName(String name) {
        String second = null;
        int size = this.types.size();
        for (int i = 0; i < size; i++) {
            if (this.types.get(i).getFirst().toString().equalsIgnoreCase(name)) {
                second = this.types.get(i).getSecond().toString();
            }
        }
        return second;
    }

    public boolean isParent(String code) {
        for (int i = 0; i < this.parents.size(); i++) {
            if (this.parents.get(i).getSecond().toString()
                    .equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPairCode(String code) {
        int i = 0;
        for (i = 0; i < this.pairs.size(); i++) {
            CodeNamePair tmp = this.pairs.get(i);
            if (tmp.getFirst().toString().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    public void removePairByCode(String code) {
        int i = 0;
        for (i = 0; i < this.pairs.size(); i++) {
            CodeNamePair tmp = this.pairs.get(i);
            if (tmp.getFirst().toString().equalsIgnoreCase(code)) {
                break;
            }
        }
        if (i < this.pairs.size()) {
            this.pairs.remove(i);
        }

    }

    public void outputData(String output) {
        System.out.println("====================" + output
                + "=============================");

        var tableItemSet = this.tableItems.entrySet();
        for (var entry : tableItemSet) {
            System.out.println(entry.getKey() + ":");
            List<CodeNamePair> tmpArray = entry.getValue();
            int arraySize = tmpArray.size();
            for (int i = 0; i < arraySize; i++) {
                System.out.printf("    %s", tmpArray.get(i).getFirst());
                System.out.println("    " + tmpArray.get(i).getSecond());
            }
        }
        System.out.println("=================================================");

        int listSize = this.types.size();
        for (int j = 0; j < listSize; j++) {
            System.out.println(this.types.get(j).getFirst() + ": \t"
                    + this.types.get(j).getSecond());
        }
        System.out
                .println("=================================================\n\n");
    }

    public boolean writeIntoFile(String filename) throws IOException {
        String testDB = AppConfig.getTestDB().trim();
        if (testDB.equalsIgnoreCase("mysql")) {
            return writeIntoFileMySQL(filename);
        } else if (testDB.equalsIgnoreCase("postgres")) {
            return writeIntoFilePostgreSQL(filename);
        }
        return false;
    }

    public boolean writeIntoFilePostgreSQL(String filename) throws IOException {
        List<String> errorTable = new ArrayList<>(); // for debug
        File sqlFile;
        sqlFile = new File(filename);
        FileOutputStream oFile;
        PrintStream pPRINT = null;
        if (!sqlFile.exists()) {
            sqlFile.createNewFile();
        }
        oFile = new FileOutputStream(sqlFile, false);
        pPRINT = new PrintStream(oFile);
        pPRINT.println("-- CREATE DATABASE FOR " + filename + "\n");
        String dbName = sqlFile.getName();
        dbName = dbName.substring(0, dbName.length() - 4);

        String tableName = null;

        int PKNum = 0;
        int FKNum = 0;
        List<String> foreignKeyList = new ArrayList<>();
        var entrySet = this.tableItems.entrySet();
        // iterate all tables
        for (var entry : entrySet) {
            tableName = entry.getKey();
            List<String> primaryKeys = getPrimaryKey(tableName);
            if (primaryKeys.size() == 0) {
                continue;
            }
            // get the number of primary keys and foreign keys
            PKNum = hasMultipleItem(tableName, "primaryKey");
            FKNum = hasMultipleItem(tableName, "foreignKey");

            pPRINT.println("--");
            pPRINT.println("-- Table structure for table " + tableName);
            pPRINT.println("--" + "\n");

            pPRINT.println("CREATE TABLE " + tableName + " (");
            List<CodeNamePair> tableItems = entry.getValue();
            int arraySize = tableItems.size();
            boolean firstPK = true;

            // primaryKeyStr will write to file at the end of every create table
            // block
            String primaryKeyStr = new String("PRIMARY KEY (");
            // foreignKeyStr will write to file at the end of file
            String foreignKeyStr = new String("ALTER TABLE " + tableName
                    + "\n");
            int lastFKCounter = 0;
            // iterate all items of every table
            for (int i = 0; i < arraySize; i++) {
                // the itemName is the name of this item, like customerID,
                // orderID
                String itemName = tableItems.get(i).getSecond().toString();
                // if(itemName.contains("$")){
                // itemName = itemName.split("$")[0];
                // }
                // itemType is the type of the item, for example, the type of
                // customerID is Ineteger
                String itemType = getTypesByName(itemName);
                if (itemType == null) {
                    itemType = "NULL";
                    errorTable.add(tableName + itemName + itemType);
                }
                if (itemType.equalsIgnoreCase("Integer")) {
                    itemType = "int";
                } else if (itemType.equalsIgnoreCase("Real")) {
                    itemType = "decimal(20,5)";
                } else if (itemType.equalsIgnoreCase("string")) {
                    itemType = "varchar(64)";
                } else if (itemType.equalsIgnoreCase("class")) {
                    itemType = "longblob";
                } else if (itemType.equalsIgnoreCase("DType")) {
                    itemType = "varchar(64)";
                } else if (itemType.equalsIgnoreCase("Bool")) {
                    itemType = "boolean"; // boolean is tinyint in mysql
                } else if (itemType.equalsIgnoreCase("Longblob")) {
                    itemType = "Longblob";
                } else if (itemType.equalsIgnoreCase("Time")) {
                    itemType = "TIMESTAMP";
                }

                String caseName = tableItems.get(i).getFirst().toString();
                if (caseName.equalsIgnoreCase("fields")) {
                    String postfix = isID(itemName, tableName) ? " NOT NULL, \n"
                            : ",\n";
                    pPRINT.print(itemName + " " + itemType + postfix);
                } else if (caseName.equalsIgnoreCase("primaryKey")) {
                    if (PKNum > 1) {
                        if (firstPK) {
                            primaryKeyStr = primaryKeyStr + itemName;
                            firstPK = false;
                        } else {
                            primaryKeyStr = primaryKeyStr + "," + itemName
                                    + ")";
                        }
                    } else {
                        primaryKeyStr = "PRIMARY KEY (" + itemName + ")";
                    }
                } else if (caseName.equalsIgnoreCase("foreignKey")) {
                    // add constrains for ths table
                    // find the primary tablename
                    String pkTable = tableNameByID(itemName);
                    if (1 == FKNum) {
                        foreignKeyStr = foreignKeyStr + "  ADD CONSTRAINT FK_"
                                + tableName + "_" + itemName + " "
                                + "FOREIGN KEY (" + itemName
                                + ") REFERENCES " + pkTable + " ("
                                + itemName + ") "
                                + "ON DELETE CASCADE ON UPDATE CASCADE;\n";
                    } else if (FKNum > 1) {
                        lastFKCounter++;
                        if (lastFKCounter < FKNum) {
                            foreignKeyStr = foreignKeyStr
                                    + "  ADD CONSTRAINT FK_" + tableName + "_"
                                    + itemName + " FOREIGN KEY ("
                                    + itemName + ") REFERENCES " + pkTable
                                    + " (" + itemName + ") "
                                    + "ON DELETE CASCADE ON UPDATE CASCADE,\n";
                        } else if (lastFKCounter >= FKNum) {
                            foreignKeyStr = foreignKeyStr
                                    + "  ADD CONSTRAINT FK_" + tableName + "_"
                                    + itemName + " FOREIGN KEY ("
                                    + itemName + ") REFERENCES" + pkTable
                                    + " (" + itemName + ") "
                                    + "ON DELETE CASCADE ON UPDATE CASCADE;\n";
                        }
                    }
                }
            }
            pPRINT.println(primaryKeyStr);
            pPRINT.println(");" + "\n");
            if (FKNum > 0) {
                foreignKeyList.add(foreignKeyStr);
            }
        }

        // ====================
        // output foreignKeyStr here
        // ====================
        for (int i = 0; i < foreignKeyList.size(); i++) {
            pPRINT.println(foreignKeyList.get(i));
        }

        pPRINT.close();
        return true;
    }

    public boolean writeIntoFileMySQL(String filename) throws IOException {
        List<String> errorTable = new ArrayList<>(); // for debug
        File sqlFile;
        sqlFile = new File(filename);
        FileOutputStream oFile;
        PrintStream pPRINT = null;
        if (!sqlFile.exists()) {
            sqlFile.createNewFile();
        }
        oFile = new FileOutputStream(sqlFile, false);
        pPRINT = new PrintStream(oFile);
        pPRINT.println("-- CREATE DATABASE FOR " + filename + "\n");
        String dbName = sqlFile.getName();
        dbName = dbName.substring(0, dbName.length() - 4);
        pPRINT.println("USE " + dbName + ";");

        String tableName = null;

        int PKNum = 0;
        int FKNum = 0;
        List<String> foreignKeyList = new ArrayList<>();
        var entrySet = this.tableItems.entrySet();
        // iterate all tables
        for (var entry : entrySet) {
            tableName = entry.getKey();
            List<String> primaryKeys = getPrimaryKey(tableName);
            if (primaryKeys.size() == 0) {
                continue;
            }
            // get the number of primary keys and foreign keys
            PKNum = hasMultipleItem(tableName, "primaryKey");
            FKNum = hasMultipleItem(tableName, "foreignKey");

            pPRINT.println("--");
            pPRINT.println("-- Table structure for table " + tableName);
            pPRINT.println("--" + "\n");

            // pPRINT.println("CREATE TABLE `"+filename+"`.`"+tableName +"` (");
            pPRINT.println("CREATE TABLE `" + tableName + "` (");
            List<CodeNamePair> tableItems = entry.getValue();
            int arraySize = tableItems.size();
            boolean firstPK = true;

            // primaryKeyStr will write to file at the end of every create table
            // block
            String primaryKeyStr = new String("PRIMARY KEY (`");
            // foreignKeyStr will write to file at the end of file
            String foreignKeyStr = new String("ALTER TABLE `" + tableName
                    + "`\n");
            int lastFKCounter = 0;
            // iterate all items of every table
            for (int i = 0; i < arraySize; i++) {
                // the itemName is the name of this item, like customerID,
                // orderID
                String itemName = tableItems.get(i).getSecond().toString();
                // if(itemName.contains("$")){
                // itemName = itemName.split("$")[0];
                // }
                // itemType is the type of the item, for example, the type of
                // customerID is Ineteger
                String itemType = getTypesByName(itemName);
                if (itemType == null) {
                    itemType = "NULL";
                    errorTable.add(tableName + itemName + itemType);
                }
                if (itemType.equalsIgnoreCase("Integer")) {
                    itemType = "int";
                } else if (itemType.equalsIgnoreCase("Real")) {
                    itemType = "decimal(20,5)";
                } else if (itemType.equalsIgnoreCase("string")) {
                    itemType = "varchar(64)";
                } else if (itemType.equalsIgnoreCase("class")) {
                    itemType = "longblob";
                } else if (itemType.equalsIgnoreCase("DType")) {
                    itemType = "varchar(64)";
                } else if (itemType.equalsIgnoreCase("Bool")) {
                    itemType = "boolean"; // boolean is tinyint in mysql
                } else if (itemType.equalsIgnoreCase("Longblob")) {
                    itemType = "Longblob";
                } else if (itemType.equalsIgnoreCase("Time")) {
                    itemType = "TIMESTAMP";
                }

                String caseName = tableItems.get(i).getFirst().toString();
                if (caseName.equalsIgnoreCase("fields")) {
                    String postfix = isID(itemName, tableName) ? " NOT NULL, \n"
                            : ",\n";
                    pPRINT.print("`" + itemName + "` " + itemType + postfix);
                } else if (caseName.equalsIgnoreCase("primaryKey")) {
                    if (PKNum > 1) {
                        if (firstPK) {
                            primaryKeyStr = primaryKeyStr + itemName + "`";
                            firstPK = false;
                        } else {
                            primaryKeyStr = primaryKeyStr + ",`" + itemName
                                    + "`)";
                        }
                    } else {
                        primaryKeyStr = "PRIMARY KEY (`" + itemName + "`)";
                    }
                } else if (caseName.equalsIgnoreCase("foreignKey")) {
                    pPRINT.println("KEY `FK_" + tableName + "_" + itemName
                            + "_idx` (`" + itemName + "`),");

                    // add constrains for ths table
                    // find the primary tablename
                    String pkTable = tableNameByID(itemName);
                    if (1 == FKNum) {
                        foreignKeyStr = foreignKeyStr + "  ADD CONSTRAINT `FK_"
                                + tableName + "_" + itemName + "` "
                                + "FOREIGN KEY (`" + itemName
                                + "`) REFERENCES `" + pkTable + "` (`"
                                + itemName + "`) "
                                + "ON DELETE CASCADE ON UPDATE CASCADE;\n";
                    } else if (FKNum > 1) {
                        lastFKCounter++;
                        if (lastFKCounter < FKNum) {
                            foreignKeyStr = foreignKeyStr
                                    + "  ADD CONSTRAINT `FK_" + tableName + "_"
                                    + itemName + "` " + "FOREIGN KEY (`"
                                    + itemName + "`) REFERENCES `" + pkTable
                                    + "` (`" + itemName + "`) "
                                    + "ON DELETE CASCADE ON UPDATE CASCADE,\n";
                        } else if (lastFKCounter >= FKNum) {
                            foreignKeyStr = foreignKeyStr
                                    + "  ADD CONSTRAINT `FK_" + tableName + "_"
                                    + itemName + "` " + "FOREIGN KEY (`"
                                    + itemName + "`) REFERENCES `" + pkTable
                                    + "` (`" + itemName + "`) "
                                    + "ON DELETE CASCADE ON UPDATE CASCADE;\n";
                        }
                    }
                }
            }
            pPRINT.println(primaryKeyStr);
            pPRINT.println(");" + "\n");
            if (FKNum > 0) {
                foreignKeyList.add(foreignKeyStr);
            }
        }

        // ====================
        // output foreignKeyStr here
        // ====================
        for (int i = 0; i < foreignKeyList.size(); i++) {
            pPRINT.println(foreignKeyList.get(i));
        }

        pPRINT.close();
        return true;
    }
}
