package edu.virginia.cs.Synthesizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ORMParser {
    private final String input;
    private final String output;
    private final DataProvider dataProvider;
    private final ArrayList<CodeNamePair> reverseTAssociate;
    private final ArrayList<CodeNamePair> foreignKeys;
    // HashMap<Association Name, pair<src, dst>, src and dst are class name
    private final HashMap<String, CodeNamePair> associations;
    private final ArrayList<CodeNamePair> primaryKeys;
    private final ArrayList<CodeNamePair> fields;
    private final ArrayList<String> allFields;
    private final ArrayList<CodeNamePair> fieldsTable;
    private final ArrayList<Sig> sigs;


    public ORMParser(String input, String output, ArrayList<Sig> sigs) {
        this.input = input;
        this.output = output;
        this.reverseTAssociate = new ArrayList<>();
        this.foreignKeys = new ArrayList<>();
        this.associations = new HashMap<>();
        this.primaryKeys = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.allFields = new ArrayList<>();
        this.fieldsTable = new ArrayList<>();
        this.sigs = sigs;
        this.dataProvider = new DataProvider();

    }

    public ArrayList<CodeNamePair> getReverseIds() {
        return this.dataProvider.getReverseIds();
    }

    public DataProvider getDataProvider() {
        return dataProvider;
    }

    public HashMap<String, CodeNamePair> getAssociations() {
        return associations;
    }

    public ArrayList<CodeNamePair> getReverseTAssociate() {
        return this.reverseTAssociate;
    }

    public HashMap<String, CodeNamePair> getAssociation() {
        return this.associations;
    }

    public ArrayList<CodeNamePair> getFields() {
        for (CodeNamePair field : this.fields) {
            field.setFirst(this.dataProvider.getSecondByFirst(field.getFirst()));
            field.setSecond(this.dataProvider.getSecondByFirst(field.getSecond()));
        }
        return fields;
    }

    public ArrayList<CodeNamePair> getFieldType() {
        return this.dataProvider.getTypes();
    }

    // this function will change the value of schemas and return it
    public HashMap<String, ArrayList<CodeNamePair>> getDataSchemas() {
        return this.dataProvider.getTables();
    }

    public ArrayList<CodeNamePair> getParents() {
        return this.dataProvider.getParents();
    }

    public ArrayList<CodeNamePair> getForeignKey() {
        // refine the the foreign key list first
        for (CodeNamePair fKey : this.foreignKeys) {
            fKey.setFirst(this.dataProvider.getSecondByFirst(fKey.getFirst()));
            fKey.setSecond(this.dataProvider.getSecondByFirst(fKey.getSecond()));
        }
        return this.foreignKeys;
    }

    public ArrayList<CodeNamePair> getFieldsTable() {
        // refine the the foreign key list first
        for (CodeNamePair fKey : this.fieldsTable) {
            fKey.setFirst(this.dataProvider.getSecondByFirst(fKey.getFirst()));
            fKey.setSecond(this.dataProvider.getSecondByFirst(fKey.getSecond()));
        }
        return fieldsTable;
    }

    public ArrayList<String> getallFields() {
        // refine the the foreign key list first
        ArrayList<String> result = new ArrayList<>();
        for (String fKey : this.allFields) {
            //to not add DType$0
            String fieldName = this.dataProvider.getSecondByFirst(fKey);
            if (!fieldName.startsWith("DType$"))
                result.add(fieldName);
        }
        return result;

    }

    public ArrayList<CodeNamePair> getPrimaryKeys() {
        for (CodeNamePair pair : this.primaryKeys) {
            pair.setFirst(this.dataProvider.getSecondByFirst(pair.getFirst()));
            pair.setSecond(this.dataProvider.getSecondByFirst(pair.getSecond()));
        }
        // remove those tables which are associations
//        Iterator<CodeNamePair> it = this.primaryKeys.iterator();
//        while (it.hasNext()) {
        // if the table is association
//            if (isTableAAssociation(it.next().getFirst())) {
//                it.remove();
//            }
//        }
        return this.primaryKeys;
    }

    public void createSchemas() {
        // the input file will be a XML solution file
        parseXML();
        try {
            this.dataProvider.refineTable();
            //this.dataProvider.outputData(this.output); //For debug
            this.dataProvider.writeIntoFile(this.output);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    // parse the input file, and store the meta data into data provider
    public void parseXML() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            //parse using builder to get DOM representation of the XML file
            Document dom = db.parse(this.input);

            //get the root element
            Element docEle = dom.getDocumentElement();
            //get a nodelist of  elements
            NodeList fieldnodes = docEle.getElementsByTagName("field");
            // handle parent first
            for (int i = 0; i < fieldnodes.getLength(); i++) {
                Node node = fieldnodes.item(i);
                // find different sub nodes based on label value
                // find node if the "label" attribute is "primaryKey"
                if (node.hasAttributes()) {
                    Element element = (Element) node;
                    String labelValue = element.getAttribute("label");
                    if (labelValue.equalsIgnoreCase("parent")) {
                        parseParent(element);
                    }
                }
            }
            // handle other labels
            for (int i = 0; i < fieldnodes.getLength(); i++) {
                Node node = fieldnodes.item(i);
                // find different sub nodes based on label value
                // find node if the "label" attribute is "primaryKey"
                if (node.hasAttributes()) {
                    Element element = (Element) node;
                    String labelValue = element.getAttribute("label");
                    if (labelValue.equalsIgnoreCase("primarykey")) {
                        parsePK(element);
                    } else if (labelValue.equalsIgnoreCase("fields")) {
                        parseFields(element);
                    } else if (labelValue.equalsIgnoreCase("foreignKey")) {
                        parseFK(element);
                    } else if (labelValue.equalsIgnoreCase("tAssociate")) {
                        parse_tAssociate(element);
                    } else if (labelValue.equalsIgnoreCase("fAssociate")) {
                        parse_fAssociate(element);
                    } else if (labelValue.equalsIgnoreCase("attrSet")) {
                        parseAttrSet(element);
                    } else if (labelValue.equalsIgnoreCase("id")) {
                        parseId(element);
                    } else if (labelValue.equalsIgnoreCase("src")) {
                        parseSrc(element);
                    } else if (labelValue.equalsIgnoreCase("dst")) {
                        parseDst(element);
                    } else if (labelValue.equalsIgnoreCase("src_multiplicity")) {
                        parseSrcMultiplicity(element);
                    } else if (labelValue.equalsIgnoreCase("dst_multiplicity")) {
                        parseDstMultiplicity(element);
                    }
                }
            }

            // get sig nodes
            NodeList signodes = docEle.getElementsByTagName("sig");
            for (int i = 0; i < signodes.getLength(); i++) {
                Node node = signodes.item(i);
                // find different subnodes based on label value
                // find node if the "label" attribute is "primaryKey"
                if (node.hasAttributes()) {
                    Element element = (Element) node;
                    String labelValue = element.getAttribute("label");
                    String[] tmp = labelValue.split("/");
                    String type = tmp[tmp.length - 1];
                    if (type.equalsIgnoreCase("Real") || type.equalsIgnoreCase("Integer")
                            || type.equalsIgnoreCase("string") || type.equalsIgnoreCase("Class")
                            || type.equalsIgnoreCase("DType") || type.equalsIgnoreCase("Bool")
                            || type.equalsIgnoreCase("Longblob") || type.equalsIgnoreCase("Time")) {
                        NodeList atoms = element.getElementsByTagName("atom");
                        for (int j = 0; j < atoms.getLength(); j++) {
                            Node node1 = atoms.item(j);
                            Element tableCode = (Element) node1;
                            String code = tableCode.getAttribute("label");
                            String[] atomLabel = code.split("/");
                            String name = atomLabel[atomLabel.length - 1];
                            this.dataProvider.addType(name, type);
                        }
                    }
                }
            }
        } catch (Exception pce) {
            pce.printStackTrace();
        }
    }

    /**
     * Return the root table among a set of tables
     *
     * @return root table or null if any error happened
     */
    public String getRootTable(String tableName) {
        // iterate the Sig set to find the root
        for (Sig sig : this.sigs) {
            if (sig.sigName.equalsIgnoreCase(tableName)) {
                if (!sig.hasParent) { // this table is the root
                    return sig.sigName;
                } else { // this table is not the root, return its parent
                    return getRootTable(sig.parent);
                }
            }
        }
        return null; //error, return null
    }

    /**
     * This method is used to parse the "tAssociate" tag of XML file
     * "tAssociate" tag includes the mapping of table code like "Table$0" to
     * table name like "Customer". The code and name will be stored in DataProvider
     * variable "TableName"
     *
     * @param element: the input "tAssociate" tag
     */
    public void parse_tAssociate(Element element) {
        NodeList children = element.getElementsByTagName("tuple");

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String code = getSingleAtom(singleTuple, 0);    // Get the first part
                String name = getSingleAtom(singleTuple, 1);    // Get the second part
                //System.out.println(code + "+" + name + "+" + "Pair");

                // if there existed code in table pair, return value will be the collision table name
                boolean hasCode = this.dataProvider.hasPairCode(code);
                // there is one table code existed, means some table share a table code
                // need to find the root among these table
                if (hasCode) {
                    name = getRootTable(name);
                    this.dataProvider.removePairByCode(code);
                }
                this.dataProvider.addPair(code, name);

                // if there is no parent table in pairs, need to remove the pair with code "code" and add this table into pair
                // if this table is parent, replace the existed pair with this one
//                if (hasCode) {
//                    boolean isParent = this.dataProvider.isParent(name);
//                    if (isParent) {
//                        this.dataProvider.removePairByCode(code);
//                        this.dataProvider.addPair(code, name);
//                    }
//                } else {
//                    this.dataProvider.addPair(code, name);
//                }
            }
        }

/////////////////////////////////////////////////////
        // create hashmap from elements in Alloy-OM, like classes and associations, to tables in schema.
        children = element.getElementsByTagName("tuple");

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String code = getSingleAtom(singleTuple, 0);    // Get the first part
                String name = getSingleAtom(singleTuple, 1);    // Get the second part

                String tableName = this.dataProvider.getSecondByFirst(code);
                this.reverseTAssociate.add(new CodeNamePair(name, tableName));
            }
        }
    }

    /**
     * handle primary key
     *
     * @param element: the primary key tag
     */
    public void parsePK(Element element) {
        NodeList children = element.getElementsByTagName("tuple");
        //System.out.println("Tuple tags: " + children.getLength());
        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) {
                Element singleTuple = (Element) child;
                // Get the first atom
                String table = getSingleAtom(singleTuple, 0);
                // Get the second atom
                String key = getSingleAtom(singleTuple, 1);
                //System.out.println(table + "+" + "PrimaryKey" + "+" + key);
                this.dataProvider.addItem(table, "primaryKey", key);
                this.primaryKeys.add(new CodeNamePair(table, key));
            }
        }
    }

    public void parseFK(Element element) {
        NodeList children = element.getElementsByTagName("tuple");

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String table = getSingleAtom(singleTuple, 0);    // Get the first part
                String key = getSingleAtom(singleTuple, 1);    // Get the second part
                //System.out.println(table + "+" + "foreignKey" + "+" + key);
                this.dataProvider.addItem(table, "foreignKey", key);
                this.foreignKeys.add(new CodeNamePair(table, key));
            }
        }
    }

    public void parseFields(Element element) {
        NodeList children = element.getElementsByTagName("tuple");

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String table = getSingleAtom(singleTuple, 0);    // Get the first part
                String field = getSingleAtom(singleTuple, 1);    // Get the second part
                //System.out.println(table + "+" + "fields" + "+" + field);
                this.dataProvider.addItem(table, "fields", field);
                this.fields.add(new CodeNamePair(table, field));
                this.allFields.add(field);
                this.fieldsTable.add(new CodeNamePair(field, table));
            }
        }
    }

    public void parse_fAssociate(Element element) {
        NodeList children = element.getElementsByTagName("tuple");

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String code = getSingleAtom(singleTuple, 0);    // Get the first part
                String name = getSingleAtom(singleTuple, 1);    // Get the second part
                //System.out.println(code + "+" + name + "+" + "Pair");
                this.dataProvider.addPair(code, name);
            }
        }
    }

    public void parseId(Element element) {
        NodeList children = element.getElementsByTagName("tuple");

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String table = getSingleAtom(singleTuple, 0);    // Get the first part
                String Id = getSingleAtom(singleTuple, 1);    // Get the second part
                //System.out.println(table + "+" + "Id" + "+" + Id);
                this.dataProvider.addItem(table, "Id", Id);
            }
        }
    }

    public void parseParent(Element element) {
        NodeList children = element.getElementsByTagName("tuple");

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String childTable = getSingleAtom(singleTuple, 0);    // Get the first part
                String parentTable = getSingleAtom(singleTuple, 1);    // Get the second part
                //System.out.println(table + "+" + "Parent" + "+" + parentTable);
                this.dataProvider.addParent(childTable, parentTable);
            }
        }
    }

    public void parseSrc(Element element) {
        NodeList children = element.getElementsByTagName("tuple");
        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String table = getSingleAtom(singleTuple, 0);    // Get the first part
                String srcTable = getSingleAtom(singleTuple, 1);    // Get the second part
                //System.out.println(table + "+" + srcTable);
                this.dataProvider.addItem(table, "src", srcTable);
                // store association into hashmap
                if (this.associations.containsKey(table)) {
                    setAssociation(table, "src", srcTable);
                } else {
                    this.associations.put(table, new CodeNamePair(srcTable, ""));
                }
            }
        }
    }

    public void setAssociation(String assName, String target, String value) {
        for (Map.Entry<String, CodeNamePair> ass : this.associations.entrySet()) {
            if (ass.getKey().equalsIgnoreCase(assName)) {
                if (target.equalsIgnoreCase("src")) {
                    ass.getValue().setFirst(value);
                } else if (target.equalsIgnoreCase("dst")) {
                    ass.getValue().setSecond(value);
                }
            }
        }
    }

    public void parseDst(Element element) {
        NodeList children = element.getElementsByTagName("tuple");

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String table = getSingleAtom(singleTuple, 0);    // Get the first part
                String dstTable = getSingleAtom(singleTuple, 1);    // Get the second part
                //System.out.println(table + "+" + dstTable);
                this.dataProvider.addItem(table, "dst", dstTable);
                // store association into hashmap
                if (this.associations.containsKey(table)) {
                    setAssociation(table, "dst", dstTable);
                } else {
                    this.associations.put(table, new CodeNamePair("", dstTable));
                }
            }
        }
    }

    public void parseSrcMultiplicity(Element element) {
        NodeList children = element.getElementsByTagName("tuple");

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String table = getSingleAtom(singleTuple, 0);    // Get the first part
                String parentTable = getSingleAtom(singleTuple, 1);    // Get the second part
                //System.out.println(table + "+" + parentTable);
                this.dataProvider.addItem(table, "srcMul", parentTable);
            }
        }
    }

    public void parseDstMultiplicity(Element element) {
        NodeList children = element.getElementsByTagName("tuple");

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String table = getSingleAtom(singleTuple, 0);    // Get the first part
                String parentTable = getSingleAtom(singleTuple, 1);    // Get the second part
                //System.out.println(table + "+" + parentTable);
                this.dataProvider.addItem(table, "dstMul", parentTable);
            }
        }
    }

    public void parseAttrSet(Element element) {
        NodeList children = element.getElementsByTagName("tuple");

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.hasChildNodes()) { // for every <tuple> tag
                Element singleTuple = (Element) child;
                // every tuple pair includes two atom solo tags
                // the label attribute is what we need
                String table = getSingleAtom(singleTuple, 0);    // Get the first part
                String attr = getSingleAtom(singleTuple, 1);    // Get the second part
                //System.out.println(table + "+" + "attr" + "+" + attr);
                this.dataProvider.addItem(table, "attr", attr);
            }
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
