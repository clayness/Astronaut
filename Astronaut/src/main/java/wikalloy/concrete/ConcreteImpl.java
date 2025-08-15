package wikalloy.concrete;

import wikalloy.objmodel.ObjField;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ConcreteImpl implements Serializable {

    private final Map<Object, Set<ConcreteField>> fieldmap;
    private final Collection<ConcreteLoad> loads;

    public ConcreteImpl(Map<Object, Set<ConcreteField>> fieldmap, Collection<ConcreteLoad> loads) {
        this.fieldmap = fieldmap;
        this.loads = loads;
    }

    public Collection<String> getCreateQueries() {
        return fieldmap.entrySet().stream()
                .map(grp -> createTableQuery(grp.getKey(), grp.getValue()))
                .toList();
    }

    public Collection<ConcreteLoad> getConcreteLoads() {
        return loads;
    }

    private String createTableQuery(Object table, Collection<ConcreteField> columns) {
        return "CREATE TABLE `%s` (%n   %s,%n   PRIMARY KEY (%s)%n);".formatted(table,
                columns.stream().map(f -> "`%s` %s%s".formatted(
                                f.atom(), f.type(), f.isKey() ? " NOT NULL DEFAULT -1" : ""))
                        .collect(Collectors.joining(",\n   ")),
                columns.stream().filter(ConcreteField::isKey)
                        .map(ConcreteField::atom)
                        .map("`%s`"::formatted)
                        .collect(Collectors.joining(", ")));
    }

    public record ConcreteField(Object atom, Object type, boolean isKey) implements Serializable {

        public ConcreteField(ObjField objField) {
            this(objField.getAtom(), getSqlType(objField.getDataType()), objField.isKey());
        }

        private static String getSqlType(Object type) {
            //@formatter:off
            return switch (type.toString()) {
                case "oodm/TBool$0"   -> "BOOLEAN";
                case "oodm/TInt$0"    -> "INTEGER";
                case "oodm/TFloat$0"  -> "FLOAT";
                case "oodm/TString$0" -> "VARCHAR(63)";
                case "oodm/TDate$0"   -> "DATETIME";
                case "oodm/TBlob$0"   -> "BLOB";
                default -> throw new IllegalArgumentException("Could not determine field type for atom: " + type);
            };
            //@formatter:on
        }
    }
}
