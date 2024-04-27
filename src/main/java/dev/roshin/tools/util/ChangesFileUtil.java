package dev.roshin.tools.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChangesFileUtil {
    private static ChangesFileUtil instance;
    private final String changesFile;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Logger logger;

    public enum Action {
        ADDED, UPDATED, SKIPPED
    }

    private ChangesFileUtil(String changesFile) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(changesFile), "Changes file path cannot be null or empty.");
        this.changesFile = changesFile;
        this.logger = LoggerFactory.getLogger(ChangesFileUtil.class);
    }

    public static ChangesFileUtil getInstance(String changesFile) {
        if (instance == null) {
            instance = new ChangesFileUtil(changesFile);
        }
        return instance;
    }

    public void addEntry(Action action, String jarName, String groupId, String artifactId, String version, String previousVersion) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String entry;

        switch (action) {
            case ADDED:
                entry = String.format("%s Added: %s (%s:%s:%s)", timestamp, jarName, groupId, artifactId, version);
                break;
            case UPDATED:
                entry = String.format("%s Updated: %s (%s:%s:%s) [Previous: %s]", timestamp, jarName, groupId, artifactId, version, previousVersion);
                break;
            case SKIPPED:
                entry = String.format("%s Skipped: %s (%s:%s:%s)", timestamp, jarName, groupId, artifactId, version);
                break;
            default:
                throw new IllegalArgumentException("Invalid action: " + action);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(changesFile, true))) {
            writer.write(entry);
            writer.newLine();
        } catch (IOException e) {
            logger.error("Failed to write changes entry to file: {}", changesFile, e);
        }
    }

    public String readChangesFile() {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(changesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            logger.error("Failed to read changes file: {}", changesFile, e);
        }

        return content.toString();
    }
}