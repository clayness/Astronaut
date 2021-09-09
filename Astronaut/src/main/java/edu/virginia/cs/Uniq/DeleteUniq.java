package edu.virginia.cs.Uniq;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DeleteUniq {
    public static void del(String path) {

        HashSet<String> uniqFileList = uniqueFiles(cksums(getFileList(path)));

        File folder = new File(path);

        ArrayList<String> delFiles = new ArrayList<>();
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            String filePath = fileEntry.getAbsolutePath();
            if (filePath.endsWith("xml") && !uniqFileList.contains(filePath)) {
                delFiles.add(filePath);
            }
        }

        // delete xml files
        for (String s : delFiles) {
            new File(s).delete();
        }

    }

    // get file list by path
    public static ArrayList<String> getFileList(String dir) {
        ArrayList<String> lists = new ArrayList<>();
        File folder = new File(dir);

        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            String filePath = fileEntry.getAbsolutePath();
            if (filePath.endsWith("xml")) {
                lists.add(filePath);
            }
        }
        return lists;
    }

    // get checksum of all files
    public static ArrayList<Pair<String>> cksums(ArrayList<String> files) {
        ArrayList<Pair<String>> cksums = new ArrayList<>();
        for (String s : files) {
            try {
                Process p = Runtime.getRuntime().exec("cksum " + s);
                p.waitFor();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                String firstLine = reader.readLine();
                String[] splited = firstLine.split(" ");
                cksums.add(new Pair<>(splited[2], splited[0]));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return cksums;
    }

    public static HashSet<String> uniqueFiles(
            ArrayList<Pair<String>> filesWithCksums) {
        ArrayList<String> sums = new ArrayList<>();
        for (Pair<String> p : filesWithCksums) {
            sums.add(p.getSecond());
            //System.out.println(p.getSecond());
        }

        Set<String> uniqueSums = new HashSet<>(sums);

        // find out unique files with HashSet
        HashSet<String> hashSet = new HashSet<>();

        for (String u : uniqueSums) {
            for (Pair<String> p : filesWithCksums) {
                if (p.getSecond().equals(u)) {
                    hashSet.add(p.getFirst());
                    break;
                }
            }
        }

        return hashSet;
    }
}
