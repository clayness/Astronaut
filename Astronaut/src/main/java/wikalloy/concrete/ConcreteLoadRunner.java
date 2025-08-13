package wikalloy.concrete;

import picocli.CommandLine;
import wikalloy.AlloySolutionIterator;
import wikalloy.ObjectModel;
import wikalloy.generic.AbstractLoad;
import wikalloy.generic.AbstractLoadFactory;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

public class ConcreteLoadRunner implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The path to the OODM Alloy specification file.")
    private Path oodmPath;

    @CommandLine.Option(names = {"-l", "--num-loads"}, description = "The number of loads to generate.")
    private int numLoads = 10;

    @CommandLine.Option(names = {"-i", "--num-instances"}, description = "The maximum number of instances per abstract class.")
    private int numInstances = 100;

    @CommandLine.Option(names = {"-q", "--num-queries"}, description = "The maximum number of queries.")
    private int numQueries = 100;

    @CommandLine.Option(names = {"-m", "--num-models"}, description = "The maximum number of models to generate.")
    private int numModels = Integer.MAX_VALUE;

    @CommandLine.Option(names = {"-o", "--output"}, description = "The path to the root output directory.")
    private Path outputPath = Paths.get("", "out");

    public static void main(String[] args) {
        System.exit(new CommandLine(new ConcreteLoadRunner()).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        // create the output path
        var runOutput = outputPath.resolve(Instant.now().toString().replaceAll("\\D", ""));
        runOutput.toFile().mkdirs();
        // get the object model
        var it = new AlloySolutionIterator(oodmPath);
        if (!it.hasNext()) {
            throw new IllegalArgumentException("No solution found!");
        }
        var kkdm = it.next();
        var oodm = new ObjectModel.Factory(kkdm).create();
        // create a new abstract load generator from the object model
        var alg = new AbstractLoadFactory(kkdm, oodm);
        var als = IntStream.range(0, numLoads).mapToObj(i -> alg.create(numInstances, numQueries)).toArray(AbstractLoad[]::new);
        // get the instances from the "map" model
        var mapPath = oodmPath.resolveSibling("map." + oodmPath.getFileName().toString());
        var mt = new AlloySolutionIterator(mapPath);
        for (int j = 0; mt.hasNext() && j < numModels; j++) {
            var sol1 = mt.next();
            try (var pw = new PrintWriter(runOutput.resolve("MODL_%05d.xml".formatted(j)).toAbsolutePath().toFile())) {
                sol1.writeXML(pw, null, null);
            }
            var clg = new ConcreteLoadFactory(sol1, oodm);
            try (var pw = new PrintWriter(runOutput.resolve("MODL_%05d_CREATE.sql".formatted(j)).toAbsolutePath().toFile())) {
                pw.println("/*-------------------------------------------------------------*/");
                pw.printf("/*-------------------- OBJECT MODEL #%05d --------------------*/%n", j);
                pw.println(String.join("\n", clg.getCreateQueries()));
            }
            for (int i = 0; i < numLoads; i++) {
                var cl = clg.create(als[i]);
                try (var pw = new PrintWriter(runOutput.resolve("MODL_%05d_LOAD_%05d.sql".formatted(j, i)).toAbsolutePath().toFile())) {
                    pw.println("/*-------------------    INSERT  LOAD    ----------------------*/");
                    pw.println(String.join("\n", cl.getInsertQueries()));
                    pw.println("/*-------------------    SELECT  LOAD    ----------------------*/");
                    pw.println(String.join("\n", cl.getSelectQueries()));
                }
            }
        }
        return 0;
    }
}
