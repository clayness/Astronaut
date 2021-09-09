package edu.virginia.cs.Uniq;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteUniq {
    public static void del(String path) {
        try (var walk = Files.walk(Paths.get(path))) {
            var uniqFileList = new HashSet<>(cksums(getFileList(path)).stream()
                    .collect(Collectors.toMap(Pair::getSecond, Pair::getFirst, (x, y) -> x))
                    .values());
            //noinspection ResultOfMethodCallIgnored
            walk.sorted(Comparator.reverseOrder())
                    .filter(p -> p.endsWith("xml"))
                    .map(Path::toFile)
                    .filter(f -> !uniqFileList.contains(f.getAbsolutePath()))
                    .forEach(File::delete);
        } catch (IOException e) {
            /* no-op */
        }
    }

    // get file list by path
    public static List<String> getFileList(String dir) throws IOException {
        try (var list = Files.list(Paths.get(dir))) {
            return list.filter(p -> p.endsWith("xml"))
                    .map(p -> p.toAbsolutePath().toString())
                    .collect(Collectors.toList());
        }
    }

    // get checksum of all files
    public static List<Pair<String>> cksums(List<String> files) {
        return files.stream().map(s -> {
            try {
                Process p = Runtime.getRuntime().exec("cksum " + s);
                p.waitFor();

                try (var reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String[] splited = reader.readLine().split(" ");
                    return new Pair<>(splited[2], splited[0]);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

}
