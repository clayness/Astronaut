package edu.virginia.cs.Synthesizer;

/**
 * @author ct4ew
 */
public class FileOperation {

    public FileOperation() {
    }

    public static String getMappingRun(String specFile) {
        FrontParser fp = new FrontParser(specFile);
        fp.eraseComment();
        fp.parseFile();
        return fp.createMappingRun(specFile);
    }

}
