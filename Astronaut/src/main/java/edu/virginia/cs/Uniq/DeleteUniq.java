package edu.virginia.cs.Uniq;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class DeleteUniq {
    public static void del(String path) {

        String dirPath = path;
        Set<String> uniqFileList = uniqueFiles(cksums(getFileList(dirPath)));

        File folder = new File(dirPath);

        ArrayList<String> delFiles = new ArrayList<String>();
        for (final File fileEntry : folder.listFiles()) {
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
        ArrayList<String> lists = new ArrayList<String>();
        File folder = new File(dir);

        for (final File fileEntry : folder.listFiles()) {
            String filePath = fileEntry.getAbsolutePath();
            if (filePath.endsWith("xml")) {
                lists.add(filePath);
            }
        }
        return lists;
    }

    // get checksum of all files
    public static List<Map.Entry<String, String>> cksums(List<String> files) {
        var cksums = new ArrayList<Map.Entry<String, String>>();
        for (String s : files) {
            try {
                Process p = Runtime.getRuntime().exec("cksum " + s);
                p.waitFor();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                String firstLine = reader.readLine();
                String[] splited = firstLine.split(" ");
                cksums.add(Map.entry(splited[2], splited[0]));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return cksums;
    }

    public static Set<String> uniqueFiles(
            List<Map.Entry<String, String>> filesWithCksums) {
        var sums = new ArrayList<String>();
        for (var p : filesWithCksums) {
            sums.add(p.getValue());
        }

        Set<String> uniqueSums = new HashSet<String>(sums);

        ArrayList<String> files = new ArrayList<String>();
        // find out unique files with HashSet
        HashSet<String> hashSet = new HashSet<String>();

        for (String u : uniqueSums) {
            for (var p : filesWithCksums) {
                if (p.getValue().equals(u)) {
                    hashSet.add(p.getKey());
                    break;
                }
            }
        }

        return hashSet;
    }

    // compare files by contains
    public static List<String> compare(List<String> files) {
        int filesSize = files.size();
        // 0 for same, 1 for different
        int[][] matrix = new int[filesSize][filesSize];
        List<String> fileList = new ArrayList<>();
        Map<String, Integer> fileMap = new HashMap<>();
        Process p;
        try {
            for (int i = 0; i < files.size(); i++) {
                for (int j = 0; j < files.size(); j++) {
                    String iFile = files.get(i);
                    String jFile = files.get(j);
                    p = Runtime.getRuntime()
                            .exec("diff " + iFile + " " + jFile);
                    p.waitFor();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    String firstLine = reader.readLine();
                    if (firstLine != null) {
                        matrix[i][j] = 1;
                    }
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // iterate map and copy to a list
        Set<String> set = fileMap.keySet();
        for (String s : set) {
            fileList.add(s);
        }
        return fileList;
    }
}
