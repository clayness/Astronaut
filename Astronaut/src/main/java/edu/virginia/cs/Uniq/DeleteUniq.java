package edu.virginia.cs.Uniq;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

public class DeleteUniq {
    public static void del(String folder) {
        // Get all XML files in the directory
        Path dirPath = Path.of(folder);
        if (!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Provided path is not a directory");
        }
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(dirPath)) {
            Set<String> seen = new HashSet<>();
            for (Path path : paths) {
                if (Files.isRegularFile(path)) {
                    // Generate the SHA-256 hash
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(Files.readAllBytes(path));
                    // Convert hash to hex string
                    StringBuilder hexString = new StringBuilder();
                    for (byte b : hash) {
                        String hex = Integer.toHexString(0xff & b);
                        if (hex.length() == 1) {
                            hexString.append('0');
                        }
                        hexString.append(hex);
                    }
                    if (!seen.add(hexString.toString())) {
                        // if the hash is in the set, this file is a duplicate
                        Files.delete(path);
                    }
                }
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }
}