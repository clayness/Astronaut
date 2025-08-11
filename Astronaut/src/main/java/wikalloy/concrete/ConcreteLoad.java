package wikalloy.concrete;

import picocli.CommandLine;
import wikalloy.AbstractLoad;
import wikalloy.AlloySolutionIterator;
import wikalloy.ObjectModel;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConcreteLoad {

    private final Set<ConcreteRow> rows = new HashSet<>();

    public void addRow(Object table, Collection<Object> columns, Collection<Object> values) {
        this.rows.add(new ConcreteRow(table, columns, values));
    }

    public Collection<String> getInsertQueries() {
        return rows.stream().map(r -> "INSERT INTO `%s` (%s) VALUES (%s);".formatted(r.table,
                        r.columns.stream().map(v -> "`" + v + "`").collect(Collectors.joining(",")),
                        r.values.stream().map(v -> "'" + v + "'").collect(Collectors.joining(","))))
                .toList();
    }

    public record ConcreteRow(Object table, Collection<Object> columns, Collection<Object> values) {
        /* no-op */
    }

}
