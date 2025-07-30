package edu.virginia.cs.Synthesizer;

import edu.virginia.cs.AppConfig;

import java.io.*;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class AlloyOMToAlloyDM {
    private final Boolean isDebugOn = AppConfig.getDebug();

    private final Map<String, String> typeList = new HashMap<>();
    private final List<Sig> sigs = new ArrayList<>();

    public Map<String, String> getTypeList() {
        return this.typeList;
    }

    public List<Sig> getSigs() {
        return this.sigs;
    }

    public List<String> getAss() {
        List<String> ss = new ArrayList<>();
        for (Sig s : sigs) {
            if (s.category == 1) {
                ss.add(s.sigName);
            }
        }
        return ss;
    }

    public List<String> getIDs() {
        List<String> ss = new ArrayList<>();
        for (Sig s : sigs) {
            if (s.category == 0) {
                ss.add(s.id.trim());
            } else if (s.category == 1) {
                String id = getID(s.src).trim();
                ss.add(id);
                id = getID(s.dst).trim();
                ss.add(id);
            }
        }
        return ss;
    }

    public void run(String AlloyOM, String AlloyDM, int intScope) {
        // File alloyOM = new File(AlloyOM);
        String noComment = eraseComment(AlloyOM);
        File file = new File(AlloyDM);
        // delete alloy_dm if it existed
        if (file.delete()) {
            if (isDebugOn) {
                System.out.println(file.getName() + " is deleted!");
            }
        } else {
            if (isDebugOn) {
                System.out.println("Delete operation is failed.");
            }
        }

        try {
            FileReader fileReader = new FileReader(noComment);
            BufferedReader br = new BufferedReader(fileReader);
            // scan the file and get the type of field
            String line;
            boolean hasString = false;
            boolean hasBool = false;
            while ((line = br.readLine()) != null) {
                if (line.contains("sig") && line.contains("extends")) {
                    if (!line.contains("Class") && !line.contains("Association")) {
                        String[] field_type = line.split(" ");
                        String type = field_type[field_type.length - 1];
                        type = type.split("\\{")[0];
                        if (type.trim().equalsIgnoreCase("Integer")) {
                            type = "Int";
                        } else if (type.trim().equalsIgnoreCase("string")) {
                            type = "string";
                            hasString = true;
                        } else if (type.trim().equalsIgnoreCase("Real")) {
                            type = "Int";
                        } else if (type.trim().equalsIgnoreCase("Bool")) {
                            type = "Bool";
                            hasBool = true;
                        }
                        String fieldName = field_type[2];
                        typeList.put(fieldName.trim(), type.trim());
                    }
                }
            }
            br.close();
            fileReader = new FileReader(noComment);
            br = new BufferedReader(fileReader);
            FileWriter fileWriter = new FileWriter(AlloyDM);
            PrintWriter pw = new PrintWriter(fileWriter);
            // String[] alloyPaths = Alloy.split("\\\\");
            String pattern = Pattern.quote(FileSystems.getDefault().getSeparator());
            String[] alloyPaths = AlloyDM.split(pattern);
            String fileName = alloyPaths[alloyPaths.length - 1];
            alloyPaths = fileName.split("\\.");

            String moduleName = alloyPaths[0];
            pw.println("module " + moduleName);
            pw.println("");

            while ((line = br.readLine()) != null) {
                if (line.contains("sig") && line.contains("extends") && line.contains("Class")) {
                    Sig sig = new Sig();
                    sig.category = 0; // Class
                    String[] sig_tmp = line.split(" ");
                    sig.sigName = sig_tmp[2].trim();

                    // read fields
                    while (!(line = br.readLine()).contains("}")) {
                        if (line.contains("attrSet")) {
                            String[] tmps = line.split("=");
                            String[] attrSets = tmps[1].split("\\+");
                            for (String s : attrSets) {
                                sig.attrSet.add(s.trim());
                            }
                            continue;
                        }
                        if (line.contains("id") && line.contains("=")) {
                            String[] tmps = line.split("=");
                            sig.id = tmps[1].trim();
                            continue;
                        }
                        if (line.contains("parent") && line.contains("in")) {
                            sig.parent = line.split(" ")[2].trim();
                            sig.hasParent = true;
                        }
                    }
                    sigs.add(sig);
                    continue;
                }

                // handle association
                if (line.contains("sig") && line.contains("extends") && line.contains("Association")) {
                    Sig sig = new Sig();
                    sig.category = 1; // Association
                    sig.sigName = line.split(" ")[2].trim();
                    while (!(line = br.readLine()).contains("}")) {
                        // the order of if statement is very important
                        if (line.contains("src_multiplicity") && line.contains("=")) {
                            sig.src_mul = line.split("=")[1].trim();
                            if (sig.src_mul.equalsIgnoreCase("ONE")) {
                                sig.src_mul = "one";
                            } else if (sig.src_mul.equalsIgnoreCase("MANY")) {
                                sig.src_mul = "some";
                            }
                            continue;
                        }
                        if (line.contains("dst_multiplicity") && line.contains("=")) {
                            sig.dst_mul = line.split("=")[1].trim();
                            if (sig.dst_mul.equalsIgnoreCase("ONE")) {
                                sig.dst_mul = "one";
                            } else if (sig.dst_mul.equalsIgnoreCase("MANY")) {
                                sig.dst_mul = "some";
                            }
                            continue;
                        }
                        if (line.contains("src") && line.contains("=")) {
                            String[] tmps = line.split("=");
                            sig.src = tmps[1].trim();
                            continue;
                        }
                        if (line.contains("dst") && line.contains("=")) {
                            sig.dst = line.split("=")[1].trim();
                        }
                    }
                    sigs.add(sig);
                }
            }
            br.close();
            fileReader.close();
            if (hasBool) {
                pw.println("open util/boolean");
            }
            if (hasString) {
                pw.println("sig string{}");
            }

            // output into file
            for (Sig sig : sigs) {
                if (sig.category == 0) {
                    pw.println("sig " + sig.sigName + " {");
                    // check if id is one of the attrSet
                    boolean isIDInAttr = isIDInAttr(sig);
                    boolean hasForeignKey = false;
                    String id;
                    String foreignTable = "";
                    if (isIDInAttr) {
                        id = sig.id;
                        id = sig.sigName + "_" + id;
                    } else {
                        // id is a foreign key
                        hasForeignKey = true;
                        foreignTable = getForeignTable(sig.id);
                        id = sig.sigName + "_" + sig.id;
                        pw.println(id + ": one " + getType(sig.id) + ",");
                    }
                    for (String s : sig.attrSet) {
                        String type = getType(s);
                        pw.println(sig.sigName + "_" + s + ": one " + type + ",");
                    }
                    pw.println("}");
                    pw.println("fact {");
                    // pw.println("all o1,o2:" + sig.sigName +
                    // "|o1."+sig.sigName+"_"+sig.id +
                    // " = o2."+sig.sigName+"_"+sig.id +" => o1=o2");
                    // for id
                    pw.println("all o1,o2:" + sig.sigName + "|o1." + id + " = o2." + id + " => o1=o2");
                    // for foreign key
                    if (hasForeignKey) {
                        pw.println("all o:" + sig.sigName + "| one c:" + foreignTable + "| o." + id + " = c." + foreignTable + "_" + sig.id);
                    }
                    pw.println("}");
                    pw.println("");
                }
                if (sig.category == 1) {
                    pw.println("sig " + sig.sigName + "{");
                    String src_id = getID(sig.src);
                    // String src_table = getForeignTable()
                    String src = sig.sigName + "_" + src_id;
                    String dst_id = getID(sig.dst);
                    String dst = sig.sigName + "_" + dst_id;
                    String src_id_type = getType(src_id);
                    String dst_id_type = getType(dst_id);
                    pw.println(src + ": one " + src_id_type + ",");
                    pw.println(dst + ": one " + dst_id_type + ",");
                    pw.println("}");
                    pw.println("fact {");
                    pw.println("all o1,o2:" + sig.sigName + "|o1." + src + "=o2." + src + "&&o1." + dst + "=o2." + dst + " => o1=o2");
                    pw.println("all o:" + sig.sigName + "| one c:" + sig.src + "| o." + src + " = c." + sig.src + "_" + src_id);
                    pw.println("all o:" + sig.sigName + "| one c:" + sig.dst + "| o." + dst + " = c." + sig.dst + "_" + dst_id);

                    pw.println("}");
                    pw.println("");
                }
            }

            pw.println("pred show{");
            for (Sig sig : sigs) {
                pw.println("some " + sig.sigName);
            }
            // pw.println("some univ");
            pw.println("}");
            pw.println("run show for " + intScope + " int");
            pw.close();
            fileWriter.close();
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace(); // To change body of catch statement use File |
        }
    }

    public String getForeignTable(String id) {
        for (Sig sig : sigs) {
            if (sig.id.equalsIgnoreCase(id)) {
                return sig.sigName;
            }
        }
        return null;
    }

    public boolean isIDInAttr(Sig sig) {
        String id = sig.id;
        for (String s : sig.attrSet) {
            if (id.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    public String getType(String field) {
        for (Map.Entry<String, String> entry : typeList.entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase(field.trim())) {
                return entry.getValue();
            }
        }
        return "";
    }

    public String getID(String sigName) {
        String pk = "";
        for (Sig s : sigs) {
            if (s.sigName.equalsIgnoreCase(sigName)) {
                return s.id;
            }
        }
        return pk;
    }

    public String eraseComment(String fileName) {
        FileInputStream fileInputStream;
        DataInputStream dataInputStream;
        BufferedReader br;
        String fileContent = "";
        try {
            // open file
            fileInputStream = new FileInputStream(fileName);
            dataInputStream = new DataInputStream(fileInputStream);
            br = new BufferedReader(new InputStreamReader(dataInputStream));
            String strTmp;
            while ((strTmp = br.readLine()) != null) {
                fileContent = fileContent.concat(strTmp);
                fileContent = fileContent.concat("\n");
                if (strTmp.contains("//")) {
                    fileContent = fileContent.concat("\n");
                }
            }
            br.close();
            dataInputStream.close();
            fileInputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(AlloyOMToAlloyDM.class.getName()).log(Level.SEVERE, null, ex);
        }
        // if comment with //, delete the content between "//" and '\n'
        // if comment with /**/
        // findDoubleSlash(), return index of "//"
        // findBlockCommentBegin(), return index
        fileContent = fileContent.replaceAll("//.*\\n|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "");

        // String[] paths = fileName.split("\\\\");
        String pattern = Pattern.quote(FileSystems.getDefault().getSeparator());
        String[] paths = fileName.split(pattern);
        paths[paths.length - 1] = "noComment_" + paths[paths.length - 1];
        StringBuilder outputFile = new StringBuilder();
        for (String s : paths) {
            outputFile.append(s);
            outputFile.append(File.separator);
        }
        outputFile = new StringBuilder(outputFile.substring(0, outputFile.length() - 1));

        try {
            PrintWriter out = new PrintWriter(outputFile.toString());
            out.print(fileContent);
            out.close();
        } catch (FileNotFoundException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }

        return outputFile.toString();
    }
}
