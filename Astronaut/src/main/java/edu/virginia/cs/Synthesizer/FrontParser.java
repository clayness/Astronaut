package edu.virginia.cs.Synthesizer;

import java.io.*;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author ct4ew
 */
public class FrontParser {

    private final String fileName;
    private String fileContent = "";
    private final List<String> allAssociation = new ArrayList<>();
    private final List<String> allClass = new ArrayList<>();
    private int counter = 0;

    public FrontParser(String name) {
        this.fileName = name;
    }

    public void eraseComment() {
        FileInputStream fstream;
        DataInputStream in;
        BufferedReader br;
        try {
            // open file
            fstream = new FileInputStream(this.fileName);
            in = new DataInputStream(fstream);
            br = new BufferedReader(new InputStreamReader(in));
            String strTmp;
            while ((strTmp = br.readLine()) != null) {
                this.fileContent = this.fileContent.concat(strTmp);
                this.fileContent = this.fileContent.concat("\n");
                if (strTmp.contains("//")) {
                    this.fileContent = this.fileContent.concat("\n");
                }
            }
            br.close();
            in.close();
            fstream.close();
        } catch (IOException ex) {
            Logger.getLogger(FrontParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        // if comment with //, delete the content between "//" and '\n'
        // if comment with /**/
        // findDoubleSlash(), return index of "//"
        // findBlockCommentBegin(), return index
        this.fileContent = this.fileContent.replaceAll("//.*\\n|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "");
        // delete this file

        FileWriter fWriter;
        BufferedWriter bw;
        try {
            fWriter = new FileWriter(this.fileName);
            bw = new BufferedWriter(fWriter);
            bw.write(this.fileContent);
            bw.close();
            fWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(FrontParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * parse the uploaded file get all Association Notice: still need to get all
     * independent class
     */
    public void parseFile() {
        String lastClassName;
        FileInputStream fstream;
        DataInputStream in;
        BufferedReader br;
        try {
            // open file
            fstream = new FileInputStream(this.fileName);
            in = new DataInputStream(fstream);
            br = new BufferedReader(new InputStreamReader(in));
            String strTmp;
            while ((strTmp = br.readLine()) != null) {
                if (strTmp.contains("one sig")) {
                    this.counter++;
                }
                if (strTmp.contains("Association")) {
                    String[] subStr = strTmp.split(" ");
                    this.allAssociation.add(subStr[2].trim());
                    int lines = 0;
                    int i = 4;
                    while (i > 0) {
                        String inAss = br.readLine();
                        if (inAss.contains("src")) {
                            String[] inAssSubStr = inAss.split("=");
                            deleteFromClassList(inAssSubStr[1].trim());
                            lines++;
                        }
                        if (inAss.contains("dst")) {
                            String[] inAssSubStr = inAss.split("=");
                            deleteFromClassList(inAssSubStr[1].trim());
                            lines++;
                        }
                        if (lines >= 2) {
                            break;
                        }
                        i--;
                    }
                }
                if (strTmp.contains("Class")) {
                    String[] subStr = strTmp.split(" ");
                    lastClassName = subStr[2].trim();
                    this.allClass.add(lastClassName);
                }
            }
            br.close();
            in.close();
            fstream.close();
        } catch (IOException ex) {
            Logger.getLogger(FrontParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void deleteFromClassList(String className) {
        Iterator<String> itr = this.allClass.iterator();
        while (itr.hasNext()) {
            String s = itr.next();
            if (s.equalsIgnoreCase(className)) {
                itr.remove();
                break;
            }
        }
    }

    public String createMappingRun() {
        this.eraseComment();
        this.parseFile();

        FileWriter fWriter;
        PrintWriter out;
        String pattern = Pattern.quote(FileSystems.getDefault().getSeparator());
        String[] paths = this.fileName.split(pattern);

        String[] mappingrunFileName = paths[paths.length - 1].split("\\.");

        StringBuilder fName = new StringBuilder();
        for (int i = 1; i < paths.length - 1; i++) {
            fName.append(File.separator).append(paths[i]);
        }
        fName.append(File.separator).append(mappingrunFileName[0]).append("_mapping_run.").append(mappingrunFileName[1]);

        // String fName = prefix + "mapping_run_" + spec;
        try {
            fWriter = new FileWriter(fName.toString());
            out = new PrintWriter(fWriter);
            out.println("module " + mappingrunFileName[0] + "_mapping_run");
            out.println("open ORMStrategies");
            out.println("open AssociationMappings");
            // int subStart = fName.indexOf("Parser/mapping");
            out.println("open " + mappingrunFileName[0]);
            out.println("open assertions");
//			out.println("pred show{");
            out.println("fact{");
            if (!this.allAssociation.isEmpty()) {
                String mixed = "mixedAssociationStrategy[";
                for (String s : this.allAssociation) {
                    mixed = mixed.concat(s);
                    mixed = mixed.concat("+");
                }
                mixed = mixed.substring(0, mixed.length() - 1);
                mixed = mixed.concat("]");
                out.println(mixed);
            }
            if (!this.allClass.isEmpty()) {
                String mixed = "mixedStrategy[";
                for (String s : this.allClass) {
                    mixed = mixed.concat(s);
                    mixed = mixed.concat("+");
                }
                mixed = mixed.substring(0, mixed.length() - 1);
                mixed = mixed.concat("]");
                out.println(mixed);
            }
            out.println("}");
            out.println("");

            out.println("pred mapping_run_" + mappingrunFileName[0] + "{}");
            String num = String.valueOf(this.counter + 2);
            out.println("run mapping_run_" + mappingrunFileName[0] + " for " + num);
            out.println("");

            out.println("");
            // out.println("run show for " + String.valueOf(this.counter + 3));
            out.close();
            fWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(FrontParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return fName.toString();
    }
}
