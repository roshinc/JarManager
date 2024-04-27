package dev.roshin.tools.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

class CommonUtilsTest {

    @Test
    void getFileNameWithoutExtension() {
        // Original file path
        Path originalPath = Paths.get("example.txt");

        // Get the file name without extension
        String fileNameWithoutExtension = CommonUtils.getFileNameWithoutExtension(originalPath);

        // New extension
        String newExtension = "md";

        // Create new file path with the same name but a different extension
        Path newPath = originalPath.resolveSibling(fileNameWithoutExtension + "." + newExtension);

        System.out.println("New Path: " + newPath);
    }
}