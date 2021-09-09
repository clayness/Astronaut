package edu.virginia.cs.Synthesizer;

import edu.virginia.cs.AppConfig;
import edu.virginia.cs.Uniq.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DataProvider implements Serializable {
    // pairs is used to store all code and real name
    // for example, a table pair will be <Table$0, Customer>
    // a field pair will be <field$1, customerID>
    private final ArrayList<Pair<String>> pairs;
    private final ArrayList<Pair<String>> types;
    private final ArrayList<Pair<String>> parents;
    private HashMap<String, ArrayList<Pair<String>>> tableItems;

    public DataProvider() {
        pairs = new ArrayList<>();
        tableItems = new HashMap<>();
        types = new ArrayList<>();
        parents = new ArrayList<>();
        // assert TableName.size() == TableItem.size();
    }

    public HashMap<String, ArrayList<Pair<String>>> getTables() {
        return this.tableItems;
    }

    public ArrayList<Pair<String>> getParents() {
        return this.parents;
    }

    public ArrayList<String> getAttrByTableName(String table) {
        ArrayList<String> attrs = new ArrayList<>();
        for (Map.Entry<String, ArrayList<Pair<String>>> entry : this.tableItems
                .entrySet()) {
            String tableName = entry.getKey();
            if (tableName.equalsIgnoreCase(table)) {
                for (Pair<String> pair : entry.getValue()) {
                    if (pair.getFirst().equalsIgnoreCase("attr")) {
                        attrs.add(pair.getSecond());
                    }
                }
            }
        }

        return attrs;
    }

    public Boolean isClassAssociate(String className) {
        for (Map.Entry<String, ArrayList<Pair<String>>> entry : this.tableItems
                .entrySet()) {
            String tableName = entry.getKey();
            if (tableName.equalsIgnoreCase(className)) {
                for (Pair<String> pair : entry.getValue()) {
                    if (pair.getFirst().equalsIgnoreCase("src")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ArrayList<Pair<String>> getTypes() {
        ArrayList<Pair<String>> list = new ArrayList<>();
        for (Pair<String> pair : this.types) {
            Pair<String> tmp = new Pair<>(pair.getFirst(), pair.getSecond());
            list.add(tmp);
        }
        return list;
    }

    public ArrayList<Pair<String>> getReverseIds() {
        ArrayList<Pair<String>> list = new ArrayList<>();

        for (Map.Entry<String, ArrayList<Pair<String>>> entry : this.tableItems
                .entrySet()) {
            String tableName = entry.getKey();
            ArrayList<Pair<String>> pair = entry.getValue();
            for (Pair<String> p : pair) {
                if (p.getFirst().equalsIgnoreCase("id")) {
                    list.add(new Pair<>(p.getSecond(), tableName));
                }
            }
        }
        return list;
    }

    public String addPair(String code, String name) {
        // check if same first existed
        for (Pair<String> pair : this.pairs) {
            if (pair.getFirst().equalsIgnoreCase(code)) {
                if (code.startsWith("Table")) {
                    return pair.getSecond();
                }
            }
        }
        Pair<String> myPair = new Pair<>(code, name);
        this.pairs.add(myPair);
        return "true";
    }

    public void addItem(String table, String key, String value) {
        if (this.tableItems.containsKey(table)) {
            Pair<String> tmp = new Pair<>(key, value);
            this.tableItems.get(table).add(tmp);
        } else {
            ArrayList<Pair<String>> tmpArray = new ArrayList<>();
            Pair<String> tmpPair = new Pair<>(key, value);
            tmpArray.add(tmpPair);
            this.tableItems.put(table, tmpArray);
        }
    }

    public boolean addType(String filed, String type) {
        Pair<String> newPair = new Pair<>(filed, type);
        if (this.types.contains(newPair)) {
            return false;
        } else {
            this.types.add(newPair);
        }
        return true;
    }

    public boolean addParent(String child, String parent) {
        Pair<String> newPair = new Pair<>(child, parent);
        if (this.parents.contains(newPair)) {
            return false;
        } else {
            this.parents.add(newPair);
        }
        return true;
    }

    public String getSecondByFirst(String first) {
        String second = null;
        for (Pair<String> pair : this.pairs) {
            if (pair.getFirst().equals(first)) {
                second = pair.getSecond();
            }
        }
        return second;
    }

    /**
     * replace all items with $ symbol and add them to the table, then delete
     * all table items with $ symbol
     */
    public void refineTable() {
        HashMap<String, ArrayList<Pair<String>>> tmpTableItems = new HashMap<>();
        Set<Map.Entry<String, ArrayList<Pair<String>>>> tableItemSet = this.tableItems
                .entrySet();
        // replace all items with $ symbol
        for (Map.Entry<String, ArrayList<Pair<String>>> entry : tableItemSet) {
            String tableName = entry.getKey();
            ArrayList<Pair<String>> tableContents = entry.getValue();

            if (tableName.contains("$")) {
                tableName = getSecondByFirst(tableName);
                if (!tmpTableItems.containsKey(tableName)) {
                    tmpTableItems.put(tableName,
                            new ArrayList<>());
                }

                for (Pair<String> tableContent : tableContents) {
                    String firstField = tableContent.getFirst();
                    if (firstField.contains("$")) {
                        firstField = this.getSecondByFirst(firstField);
                    }
                    String secondField = tableContent.getSecond();
                    if (secondField.contains("$")) {
                        secondField = this.getSecondByFirst(secondField);
                    }
                    // add new items into tmpTableItems
                    Pair<String> tmpPair = new Pair<>(firstField,
                            secondField);
                    tmpTableItems.get(tableName).add(tmpPair);
                }
            } else {
                if (tmpTableItems.containsKey(tableName)) {
                    // add all items in this.tableItems in to tmpTableItems
                    for (Pair<String> tableContent : tableContents) {
                        tmpTableItems.get(tableName).add(tableContent);
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
        for (Pair<String> type : this.types) {
            String typeName = type.getFirst();
            if (typeName.contains("$")) {
                type.setFirst(typeName.split("\\$")[0]);
            }
        }

        // remove pairs from parents which the child is an independent table
        for (Pair<String> pair : this.pairs) {
            if (pair.getFirst().startsWith("Table")) {
                int j;
                for (j = 0; j < this.parents.size(); j++) {
                    if (pair
                            .getSecond()
                            .equalsIgnoreCase(
                                    this.parents.get(j).getFirst())) {
                        break;
                    }
                }
                if (j < this.parents.size()) {
                    this.parents.remove(j);
                }
            }
        }

        for (Pair<String> parent : this.parents) {
            // change DType$0 in parent to DType
            ArrayList<Pair<String>> tmpList1 = tmpTableItems
                    .get(parent.getSecond());
            if (tmpList1 == null) {
                continue;
            }
            for (int j = 0; j < tmpList1.size(); j++) {
                String tmp = tmpList1.get(j).getSecond();
                if (tmp.contains("$")) {
                    String tmp1 = tmp.split("\\$")[0];
                    tmpTableItems
                            .get(parent.getSecond())
                            .get(j).setSecond(tmp1);
                    // tmpList1.get(i).setSecond(tmp1);
                }
            }

            ArrayList<Pair<String>> tmpList = tmpTableItems
                    .get(parent.getFirst());
            for (Pair<String> codeNamePair : tmpList) {
                if (!hasItemInArray(
                        tmpTableItems.get(parent.getSecond()),
                        codeNamePair)) {

                    tmpTableItems.get(parent.getSecond()).add(
                            codeNamePair);
                }
            }
            tmpTableItems.remove(parent.getFirst());
        }

        this.tableItems.clear();
        this.tableItems = tmpTableItems;
    }

    public boolean hasItemInArray(ArrayList<Pair<String>> list,
                                  Pair<String> pair) {
        boolean has = false;
        for (Pair<String> codeNamePair : list) {
            if (codeNamePair.getFirst()
                    .equalsIgnoreCase(pair.getFirst())
                    && codeNamePair.getSecond()
                    .equalsIgnoreCase(pair.getSecond())) {
                return true;
            }
        }
        return has;
    }

    public ArrayList<String> getPrimaryKey(String tableName) {
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<Pair<String>> items = this.tableItems.get(tableName);
        for (Pair<String> pair : items) {
            String tmp = pair.getFirst();
            if (tmp.equalsIgnoreCase("primaryKey")) {
                keys.add(pair.getSecond());
            }
        }
        return keys;
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
        Set<Map.Entry<String, ArrayList<Pair<String>>>> tableItemSet = this.tableItems
                .entrySet();
        for (Map.Entry<String, ArrayList<Pair<String>>> entry : tableItemSet) {

            ArrayList<Pair<String>> tmpArray = entry.getValue();
            int arraySize = tmpArray.size();
            for (int i = 0; i < arraySize; i++) {
                // if
                // (tmpArray.get(i).getFirst().toString().equalsIgnoreCase("attr"))
                // {
                if (tmpArray.get(i).getFirst()
                        .equalsIgnoreCase("ID")) {
                    if (tmpArray.get(i).getSecond()
                            .equalsIgnoreCase(ID)) {
                        // tableName = entry.getKey();
                        // System.out.println("Find Table: "+ tableName);
                        // check the ID is also in attr
                        for (Pair<String> codeNamePair : tmpArray) {
                            if (codeNamePair.getFirst()
                                    .equalsIgnoreCase("attr")) {
                                if (codeNamePair.getSecond()
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

    public int hasMultipleItem(String tableName, String item) {
        int key = 0;
        ArrayList<Pair<String>> table1 = this.tableItems.get(tableName);

        for (Pair<String> codeNamePair : table1) {
            if (codeNamePair.getFirst().equalsIgnoreCase(item)) {
                key++;
            }
        }

        return key;
    }

    public boolean isID(String field, String table) {
        boolean isID = false;
        ArrayList<Pair<String>> table1 = this.tableItems.get(table);

        for (Pair<String> codeNamePair : table1) {
            // if (table1.get(i).getFirst().toString().equalsIgnoreCase("Id")) {
            if (codeNamePair.getFirst()
                    .equalsIgnoreCase("primaryKey")) {
                if (codeNamePair.getSecond()
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
        for (Pair<String> type : this.types) {
            if (type.getFirst().equalsIgnoreCase(name)) {
                second = type.getSecond();
            }
        }
        return second;
    }

    public boolean hasPairCode(String code) {
        for (Pair<String> tmp : this.pairs) {
            if (tmp.getFirst().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    public void removePairByCode(String code) {
        int i;
        for (i = 0; i < this.pairs.size(); i++) {
            Pair<String> tmp = this.pairs.get(i);
            if (tmp.getFirst().equalsIgnoreCase(code)) {
                break;
            }
        }
        if (i < this.pairs.size()) {
            this.pairs.remove(i);
        }

    }

    public void writeIntoFile(String filename) throws IOException {
        String testDB = AppConfig.getTestDB().trim();
        if (testDB.equalsIgnoreCase("mysql")) {
            writeIntoFileMySQL(filename);
        } else if (testDB.equalsIgnoreCase("postgres")) {
            writeIntoFilePostgreSQL(filename);
        }
    }

    public void writeIntoFilePostgreSQL(String filename) throws IOException {
        ArrayList<String> errorTable = new ArrayList<>(); // for debug
        File sqlFile;
        sqlFile = new File(filename);
        FileOutputStream oFile;
        PrintStream pPRINT;
        if (!sqlFile.exists()) {
            sqlFile.createNewFile();
        }
        oFile = new FileOutputStream(sqlFile, false);
        pPRINT = new PrintStream(oFile);
        pPRINT.println("-- CREATE DATABASE FOR " + filename + "\n");

        String tableName;

        int PKNum, FKNum;
        ArrayList<String> foreignKeyList = new ArrayList<>();
        Set<Map.Entry<String, ArrayList<Pair<String>>>> entrySet = this.tableItems
                .entrySet();
        // iterate all tables
        for (Map.Entry<String, ArrayList<Pair<String>>> entry : entrySet) {
            tableName = entry.getKey();
            ArrayList<String> primaryKeys = getPrimaryKey(tableName);
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
            ArrayList<Pair<String>> tableItems = entry.getValue();
            boolean firstPK = true;

            // primaryKeyStr will write to file at the end of every create table
            // block
            StringBuilder primaryKeyStr = new StringBuilder("PRIMARY KEY (");
            // foreignKeyStr will write to file at the end of file
            StringBuilder foreignKeyStr = new StringBuilder("ALTER TABLE " + tableName
                    + "\n");
            int lastFKCounter = 0;
            // iterate all items of every table
            for (Pair<String> tableItem : tableItems) {
                // the itemName is the name of this item, like customerID,
                // orderID
                String itemName = tableItem.getSecond();
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

                String caseName = tableItem.getFirst();
                if (caseName.equalsIgnoreCase("fields")) {
                    String postfix = isID(itemName, tableName) ? " NOT NULL, \n"
                            : ",\n";
                    pPRINT.print(itemName + " " + itemType + postfix);
                } else if (caseName.equalsIgnoreCase("primaryKey")) {
                    if (PKNum > 1) {
                        if (firstPK) {
                            primaryKeyStr.append(itemName);
                            firstPK = false;
                        } else {
                            primaryKeyStr.append(",").append(itemName).append(")");
                        }
                    } else {
                        primaryKeyStr = new StringBuilder("PRIMARY KEY (" + itemName + ")");
                    }
                } else if (caseName.equalsIgnoreCase("foreignKey")) {
                    // add constrains for ths table
                    // find the primary tablename
                    String pkTable = tableNameByID(itemName);
                    if (1 == FKNum) {
                        foreignKeyStr.append("  ADD CONSTRAINT FK_").append(tableName).append("_").append(itemName).append(" ").append("FOREIGN KEY (").append(itemName).append(") REFERENCES ").append(pkTable).append(" (").append(itemName).append(") ").append("ON DELETE CASCADE ON UPDATE CASCADE;\n");
                    } else if (FKNum > 1) {
                        lastFKCounter++;
                        if (lastFKCounter < FKNum) {
                            foreignKeyStr.append("  ADD CONSTRAINT FK_").append(tableName).append("_").append(itemName).append(" FOREIGN KEY (").append(itemName).append(") REFERENCES ").append(pkTable).append(" (").append(itemName).append(") ").append("ON DELETE CASCADE ON UPDATE CASCADE,\n");
                        } else {
                            foreignKeyStr.append("  ADD CONSTRAINT FK_").append(tableName).append("_").append(itemName).append(" FOREIGN KEY (").append(itemName).append(") REFERENCES").append(pkTable).append(" (").append(itemName).append(") ").append("ON DELETE CASCADE ON UPDATE CASCADE;\n");
                        }
                    }
                }
            }
            pPRINT.println(primaryKeyStr);
            pPRINT.println(");" + "\n");
            if (FKNum > 0) {
                foreignKeyList.add(foreignKeyStr.toString());
            }
        }

        // ====================
        // output foreignKeyStr here
        // ====================
        for (String s : foreignKeyList) {
            pPRINT.println(s);
        }

        pPRINT.close();
    }

    public void writeIntoFileMySQL(String filename) throws IOException {
        ArrayList<String> errorTable = new ArrayList<>(); // for debug
        File sqlFile;
        sqlFile = new File(filename);
        FileOutputStream oFile;
        PrintStream pPRINT;
        if (!sqlFile.exists()) {
            sqlFile.createNewFile();
        }
        oFile = new FileOutputStream(sqlFile, false);
        pPRINT = new PrintStream(oFile);
        pPRINT.println("-- CREATE DATABASE FOR " + filename + "\n");
        String dbName = sqlFile.getName();
        dbName = dbName.substring(0, dbName.length() - 4);
        pPRINT.println("USE " + dbName + ";");

        String tableName;

        int PKNum, FKNum;
        ArrayList<String> foreignKeyList = new ArrayList<>();
        Set<Map.Entry<String, ArrayList<Pair<String>>>> entrySet = this.tableItems
                .entrySet();
        // iterate all tables
        for (Map.Entry<String, ArrayList<Pair<String>>> entry : entrySet) {
            tableName = entry.getKey();
            ArrayList<String> primaryKeys = getPrimaryKey(tableName);
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
            ArrayList<Pair<String>> tableItems = entry.getValue();
            boolean firstPK = true;

            // primaryKeyStr will write to file at the end of every create table
            // block
            StringBuilder primaryKeyStr = new StringBuilder("PRIMARY KEY (`");
            // foreignKeyStr will write to file at the end of file
            StringBuilder foreignKeyStr = new StringBuilder("ALTER TABLE `" + tableName
                    + "`\n");
            int lastFKCounter = 0;
            // iterate all items of every table
            for (Pair<String> tableItem : tableItems) {
                // the itemName is the name of this item, like customerID,
                // orderID
                String itemName = tableItem.getSecond();
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

                String caseName = tableItem.getFirst();
                if (caseName.equalsIgnoreCase("fields")) {
                    String postfix = isID(itemName, tableName) ? " NOT NULL, \n"
                            : ",\n";
                    pPRINT.print("`" + itemName + "` " + itemType + postfix);
                } else if (caseName.equalsIgnoreCase("primaryKey")) {
                    if (PKNum > 1) {
                        if (firstPK) {
                            primaryKeyStr.append(itemName).append("`");
                            firstPK = false;
                        } else {
                            primaryKeyStr.append(",`").append(itemName).append("`)");
                        }
                    } else {
                        primaryKeyStr = new StringBuilder("PRIMARY KEY (`" + itemName + "`)");
                    }
                } else if (caseName.equalsIgnoreCase("foreignKey")) {
                    pPRINT.println("KEY `FK_" + tableName + "_" + itemName
                            + "_idx` (`" + itemName + "`),");

                    // add constrains for ths table
                    // find the primary tablename
                    String pkTable = tableNameByID(itemName);
                    if (1 == FKNum) {
                        foreignKeyStr.append("  ADD CONSTRAINT `FK_").append(tableName).append("_").append(itemName).append("` ").append("FOREIGN KEY (`").append(itemName).append("`) REFERENCES `").append(pkTable).append("` (`").append(itemName).append("`) ").append("ON DELETE CASCADE ON UPDATE CASCADE;\n");
                    } else if (FKNum > 1) {
                        lastFKCounter++;
                        if (lastFKCounter < FKNum) {
                            foreignKeyStr.append("  ADD CONSTRAINT `FK_").append(tableName).append("_").append(itemName).append("` ").append("FOREIGN KEY (`").append(itemName).append("`) REFERENCES `").append(pkTable).append("` (`").append(itemName).append("`) ").append("ON DELETE CASCADE ON UPDATE CASCADE,\n");
                        } else {
                            foreignKeyStr.append("  ADD CONSTRAINT `FK_").append(tableName).append("_").append(itemName).append("` ").append("FOREIGN KEY (`").append(itemName).append("`) REFERENCES `").append(pkTable).append("` (`").append(itemName).append("`) ").append("ON DELETE CASCADE ON UPDATE CASCADE;\n");
                        }
                    }
                }
            }
            pPRINT.println(primaryKeyStr);
            pPRINT.println(");" + "\n");
            if (FKNum > 0) {
                foreignKeyList.add(foreignKeyStr.toString());
            }
        }

        // ====================
        // output foreignKeyStr here
        // ====================
        for (String s : foreignKeyList) {
            pPRINT.println(s);
        }

        pPRINT.close();
    }
}
