package wikalloy.concrete;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class ConcreteLoad implements Serializable {

    private final Set<ConcreteInsert> rows = new HashSet<>();
    private final Set<ConcreteSelect> selects = new HashSet<>();

    public void addInsert(Object table, Collection<Object> columns, Collection<Object> values) {
        this.rows.add(new ConcreteInsert(table, columns, values));
    }

    public void addSelect(Object table, Map<Object, Object> columns,
                          Collection<Map.Entry<Object, Map<Object, Object>>> joins,
                          Collection<ConcreteFilter> filters) {
        this.selects.add(new ConcreteSelect(table, columns, joins, filters));
    }

    public Collection<String> getInsertQueries() {
        return rows.stream().map(r -> "INSERT IGNORE INTO `%s` (%s) VALUES (%s);".formatted(r.table,
                        r.columns.stream().map(v -> "`" + v + "`").collect(Collectors.joining(",")),
                        r.values.stream().map(v -> "'" + v + "'").collect(Collectors.joining(","))))
                .toList();
    }

    public Collection<String> getSelectQueries() {
        return selects.stream().map(s -> {
            var proj = s.columns.entrySet().stream()
                    .map(c -> "`%s`.`%s`".formatted(c.getValue(), c.getKey()))
                    .collect(Collectors.joining(","));
            var from = new StringBuilder("`%s`".formatted(s.table));
            for (var j : s.joins) {
                if (!j.getValue().isEmpty()) {
                    // either all of the fields will have a value or none of them will
                    if (j.getValue().values().stream().allMatch(Objects::nonNull)) {
                        from.append(" JOIN `%s` ON %s".formatted(j.getKey(),
                                j.getValue().entrySet().stream()
                                        .map((e) -> "`%s`.`%s` = `%s`.`%s`".formatted(e.getValue(), e.getKey(), s.table(), e.getKey()))
                                        .collect(Collectors.joining(" AND "))));
                    } else {
                        from.append(" JOIN `%s` USING (%s)".formatted(j.getKey(),
                                j.getValue().keySet().stream()
                                        .map("`%s`"::formatted).collect(Collectors.joining(","))));

                    }
                }
            }
            var where = new StringBuilder();
            if (!s.filters.isEmpty()) {
                where.append(s.filters.stream()
                        .map((f) -> "`%s` %s '%s'".formatted(f.column(), f.operator(), f.value()))
                        .collect(Collectors.joining(" AND ")));
            } else {
                where.append("1 = 1");
            }
            return "SELECT DISTINCT %s FROM %s WHERE %s;".formatted(proj, from.toString(), where.toString());
        }).toList();
    }

    private record ConcreteInsert(Object table, Collection<Object> columns,
                                  Collection<Object> values) implements Serializable {
        /* no-op */
    }

    private record ConcreteSelect(Object table, Map<Object, Object> columns,
                                  Collection<Map.Entry<Object, Map<Object, Object>>> joins,
                                  Collection<ConcreteFilter> filters) implements Serializable {
        /* no-op */
    }

    public record ConcreteFilter(Object column, String operator, Object value) implements Serializable {
        /* no-op */
    }
}
