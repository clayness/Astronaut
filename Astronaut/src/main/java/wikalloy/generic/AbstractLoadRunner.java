package wikalloy.generic;

import com.google.gson.GsonBuilder;
import wikalloy.AlloySolutionIterator;
import wikalloy.ObjectModel;

import java.nio.file.Path;

public class AbstractLoadRunner {
    public static void main(String[] args) {
        // get the arguments
        var modelPath = Path.of(args[0]);
        var numInstances = args.length < 2 ? 10 : Integer.parseInt(args[1]);
        var numQueries = args.length < 3 ? 10 : Integer.parseInt(args[2]);
        // get the object model
        var it = new AlloySolutionIterator(modelPath);
        if (!it.hasNext()) {
            throw new IllegalArgumentException("No solution found!");
        }
        var sol = it.next();
        var gen = new AbstractLoadFactory(sol, new ObjectModel.Factory(sol).create());
        var instances = gen.create(numInstances, numQueries);
        var gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(AbstractLoad.class, new AbstractLoad.Adapter())
                .registerTypeAdapter(AbstractLoad.AbstractInst.class, new AbstractLoad.AbstractInst.Adapter())
                .create();
        System.out.println(gson.toJson(instances));
        System.exit(0);
    }
}
