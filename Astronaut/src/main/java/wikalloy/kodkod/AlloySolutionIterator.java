package wikalloy.kodkod;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Iterator;

public class AlloySolutionIterator implements Iterator<A4Solution> {

    private final Module module;
    private final int commandIdx;
    private final A4Options opts;
    private A4Solution solution;

    public AlloySolutionIterator(Path specFilePath, int commandIdx, A4Options opts) {
        this.module = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, specFilePath.toString());
        this.commandIdx = commandIdx;
        this.opts = opts;
    }

    public AlloySolutionIterator(Path specFilePath, int commandIdx) {
        this(specFilePath, commandIdx, new A4Options());
    }

    public AlloySolutionIterator(Path specFilePath) {
        this(specFilePath, 0);
    }

    /**
     * Test entry point of the application, used to iterate over solutions generated
     * by the Alloy model defined in the given file path, compute a SHA-256 hash
     * of each solution's string representation, and print the encoded hash.
     *
     * @param args The command-line arguments. The first argument is the path to
     *             the Alloy model specification file. This parameter is mandatory.
     */
    public static void main(String[] args) {
        var solIter = new AlloySolutionIterator(Path.of(args[0]));
        int i = 0;
        for (; solIter.hasNext(); i++) {
            var sol = solIter.next();
            try (var sw = new StringWriter()) {
                try (var pw = new PrintWriter(sw)) {
                    sol.writeXML(pw, null, null);
                }
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(sol.toString().getBytes(StandardCharsets.UTF_8));
                System.out.println(Base64.getEncoder().encodeToString(hash));
            } catch (IOException | NoSuchAlgorithmException e) {
                /* no-op */
            }
        }
        System.out.println("Total number of solutions: " + i);
        System.exit(0);
    }

    @Override
    public boolean hasNext() {
        if (this.solution == null) {
            this.solution = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP,
                    module.getAllReachableSigs(), module.getAllCommands().get(commandIdx), opts);
        }
        return solution.satisfiable();
    }

    @Override
    public A4Solution next() {
        var sol = this.solution;
        if (sol.satisfiable()) {
            this.solution = sol.next();
        }
        return sol;
    }
}
