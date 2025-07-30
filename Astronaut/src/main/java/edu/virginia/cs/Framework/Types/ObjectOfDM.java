package edu.virginia.cs.Framework.Types;

import edu.virginia.cs.Synthesizer.CodeNamePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author tang object is a single solution
 */
public class ObjectOfDM {
    private final String objPath;

    public ObjectOfDM(String path) {
        this.objPath = path;
    }

    public Map<String, Map<String, List<CodeNamePair>>> parseDocument() {

        Map<String, Map<String, List<CodeNamePair>>> allInstances = new HashMap<>();

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom = db.parse(objPath);
            // get the root element
            Element docEle = dom.getDocumentElement();
            // get instance
            // get a nodelist of elements
            NodeList signodes = docEle.getElementsByTagName("sig");

            // handle sig nodes
            // there maybe multiple instances for one table, for each
            // instance, we create a Arraylist<CodeNamePair> for it
            for (int i = 0; i < signodes.getLength(); i++) {
                Node node = signodes.item(i);
                Element tmpElement = (Element) node;
                if (node.hasAttributes()) {
                    // Element element = (Element) node;
                    String table_name = tmpElement.getAttribute("label");
                    if (!table_name.equalsIgnoreCase("univ")
                            && !table_name.equalsIgnoreCase("int")
                            && !table_name.equalsIgnoreCase("string")
                            && !table_name.contains("/")) {
                        Map<String, List<CodeNamePair>> instances = new HashMap<>();
                        NodeList multiple_instances = tmpElement
                                .getElementsByTagName("atom");
                        int instances_num = multiple_instances.getLength();
                        for (int j = 0; j < instances_num; j++) {
                            Element single_instance = (Element) multiple_instances.item(j);
                            String s_instance_name = single_instance.getAttribute("label");
                            List<CodeNamePair> single_table = new ArrayList<>();
                            instances.put(s_instance_name, single_table);
                        }
                        allInstances.put(table_name, instances);
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
                    NodeList children = element
                            .getElementsByTagName("tuple");
                    for (int j = 0; j < children.getLength(); j++) {
                        Node child = children.item(j);
                        if (child.hasChildNodes()) { // for every
                            // <tuple> tag
                            Element singleTuple = (Element) child;
                            String instanceName = getSingleAtom(
                                    singleTuple, 0); // Get the first
                            // part
                            String value = getSingleAtom(singleTuple, 1); // Get
                            // the
                            // second
                            // part
                            // get table name
                            String[] subs = instanceName.split("\\$");
                            String table_name = subs[0];
                            allInstances
                                    .get(table_name)
                                    .get(instanceName)
                                    .add(new CodeNamePair(field,
                                            value));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            //noinspection CallToPrintStackTrace
            ex.printStackTrace();
        }
        // } else if (AppConfig.getIsRandom() == 1) {
        // int randomRange = AppConfig.getRandomRange();
        // for (int i = 0; i < randomRange; i++) {
        //
        // }
        // }
        return allInstances;
    }

    private String getSingleAtom(Element singleTuple, int i) {
        NodeList atoms = singleTuple.getElementsByTagName("atom");
        Element tableCode = (Element) atoms.item(i);
        String code = tableCode.getAttribute("label");
        String[] tmp = code.split("/");
        code = tmp[tmp.length - 1];
        return code;
    }
}
