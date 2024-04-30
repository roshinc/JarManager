package dev.roshin.tools.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonUtilsTest {

    @Test
    void getFileNameWithoutExtension() {
        // Original file path
        Path originalPath = Paths.get("example.txt");

        // Get the file name without extension
        String fileNameWithoutExtension = CommonUtils.getFileNameWithoutExtension(originalPath);
        assertEquals("example", fileNameWithoutExtension);

        // New extension
        String newExtension = "md";

        // Create new file path with the same name but a different extension
        Path newPath = originalPath.resolveSibling(fileNameWithoutExtension + "." + newExtension);
        assertEquals("example.md", newPath.getFileName().toString());
        System.out.println("New Path: " + newPath);
    }
}