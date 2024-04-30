package dev.roshin.tools.download_jars;

import com.google.common.base.VerifyException;
import dev.roshin.tools.download_jars.domain.Artifact;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactDownloaderTest {

    @TempDir
    Path tempDir;

    private Path targetFolderPath;
    private Path sourceTargetFolderPath;
    private Path specFilePath;
    private Path changesLogPath;




    @BeforeEach
    void setUp() throws IOException {
        targetFolderPath = tempDir.resolve("target");
        sourceTargetFolderPath = tempDir.resolve("source-target");
        Files.createDirectories(targetFolderPath);
        Files.createDirectories(sourceTargetFolderPath);

        specFilePath = tempDir.resolve("spec.txt");
        changesLogPath = tempDir.resolve("changes.log");
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void downloadArtifact() {
        Artifact testArtifact = new Artifact("com.google.guava", "guava", Optional.empty(),
                Optional.empty());

        // Call the downloadArtifact method
        Optional<Artifact> downloadedArtifact = ArtifactDownloader.downloadArtifact(
                testArtifact, null, false,
                "https://repo1.maven.org/maven2/com/google/guava/guava",
                targetFolderPath, sourceTargetFolderPath, true, false, ""
        );

        // Assert that the artifact was downloaded successfully
        assertTrue(downloadedArtifact.isPresent());
        Artifact artifact = downloadedArtifact.get();
        assertEquals("com.google.guava", artifact.groupId());
        assertEquals("guava", artifact.artifactId());
        assertTrue(artifact.version().isPresent());

        // Assert that the artifact and sources were downloaded to the correct paths
        assertTrue(targetFolderPath.resolve("guava.jar").toFile().exists());
        assertTrue(sourceTargetFolderPath.resolve("guava-sources.jar").toFile().exists());
    }

    @Test
    void downloadArtifact_WithExistingArtifact() {
        // Create a test artifact and an existing artifact with the same version
        Artifact testArtifact = new Artifact("com.example", "test-artifact", Optional.of("1.0.0"), Optional.empty());
        Artifact existingArtifact = new Artifact("com.example", "test-artifact", Optional.of("1.0.0"), Optional.empty());

        // Call the downloadArtifact method with replaceOnlyIfDifferent set to true
        Optional<Artifact> downloadedArtifact = ArtifactDownloader.downloadArtifact(
                testArtifact, existingArtifact, true, "com/example/test-artifact",
                targetFolderPath, sourceTargetFolderPath, false, false, ""
        );

        // Assert that the existing artifact was returned
        assertTrue(downloadedArtifact.isPresent());
        Artifact artifact = downloadedArtifact.get();
        assertEquals(existingArtifact, artifact);
    }

    @Test
    void downloadArtifact_WithInvalidUrl() {
        // Create a test artifact with an invalid URL
        Artifact testArtifact = new Artifact("com.example", "invalid-artifact",
                Optional.empty(), Optional.empty());

        // Call the downloadArtifact method with an invalid URL
        Optional<Artifact> downloadedArtifact = ArtifactDownloader.downloadArtifact(
                testArtifact, null, false, "com/example/invalid-artifact",
                targetFolderPath, sourceTargetFolderPath, false, false,"your-api-key"
        );

        // Assert that the artifact download failed
        assertFalse(downloadedArtifact.isPresent());
    }

    @Test
    void downloadArtifacts() throws IOException {
        // Arrange
        String sourceTargetFolder = sourceTargetFolderPath.toString();
        boolean updateNewOnly = false;
        Files.writeString(specFilePath, "com.google.guava:guava:33.1.0-jre\norg.jetbrains.kotlinx:kotlinx-serialization-json-jvm");

        // Act
        ArtifactDownloader.downloadArtifacts(specFilePath, targetFolderPath, sourceTargetFolder, updateNewOnly, changesLogPath);

        // Assert
        assertTrue(Files.exists(targetFolderPath.resolve("kotlinx-serialization-json-jvm.jar")));
        assertTrue(Files.exists(targetFolderPath.resolve("guava.jar")));
        assertTrue(Files.exists(changesLogPath));

        // Read the changes log file and assert its content
        String changesLogContent = Files.readString(changesLogPath);
        assertTrue(changesLogContent.contains("Added: guava.jar (com.google.guava:guava:33.1.0-jre)"));
        assertTrue(changesLogContent.contains("Added: kotlinx-serialization-json-jvm.jar" +
                " (org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.3)"));
    }

    @Test
    void downloadArtifacts_shouldUpdateOnlyNewArtifacts() throws Exception {
        // Arrange
        String sourceTargetFolder = sourceTargetFolderPath.toString();
        Files.writeString(specFilePath, "com.example:test:1.0.0\n");

        // Create a temporary JAR file with pom.properties
        // Create existing artifact files
        Path jarPath = targetFolderPath.resolve("test.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("META-INF/maven/com.example/test/pom.properties"));
            Properties props = new Properties();
            props.setProperty("groupId", "com.example");
            props.setProperty("artifactId", "test");
            props.setProperty("version", "1.0.0");
            props.store(jos, null);
            jos.closeEntry();
        }
        boolean updateNewOnly = true;

        // Get the update time of the existing artifact
        long existingArtifactUpdateTime = Files.getLastModifiedTime(jarPath).toMillis();

        // Act
        ArtifactDownloader.downloadArtifacts(specFilePath, targetFolderPath, sourceTargetFolder, updateNewOnly, changesLogPath);

        // Assert
        assertTrue(Files.exists(targetFolderPath.resolve("test.jar")));
        // Assert that the existing artifact was not updated
        long updatedArtifactUpdateTime = Files.getLastModifiedTime(jarPath).toMillis();
        assertEquals(existingArtifactUpdateTime, updatedArtifactUpdateTime);

       // There were no changes, so the changes log file should not exist
        assertFalse(Files.exists(changesLogPath));
    }

    @Test
    void downloadArtifacts_shouldUpdateOnlyOldArtifacts() throws Exception {
        // Arrange
        String sourceTargetFolder = sourceTargetFolderPath.toString();
        Files.writeString(specFilePath, "com.google.guava:guava:33.1.0-jre\n");

        // Create a temporary JAR file with pom.properties
        // Create existing artifact files
        Path jarPath = targetFolderPath.resolve("guava.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("META-INF/maven/com.example/test/pom.properties"));
            Properties props = new Properties();
            props.setProperty("groupId", "com.google.guava");
            props.setProperty("artifactId", "guava");
            props.setProperty("version", "1.0.0");
            props.store(jos, null);
            jos.closeEntry();
        }
        boolean updateNewOnly = true;

        // Get the update time of the existing artifact
        long existingArtifactUpdateTime = Files.getLastModifiedTime(jarPath).toMillis();

        // Act
        ArtifactDownloader.downloadArtifacts(specFilePath, targetFolderPath, sourceTargetFolder, updateNewOnly, changesLogPath);

        // Assert
        assertTrue(Files.exists(targetFolderPath.resolve("guava.jar")));
        // Assert that the existing artifact was updated
        long updatedArtifactUpdateTime = Files.getLastModifiedTime(jarPath).toMillis();
        assertNotEquals(existingArtifactUpdateTime, updatedArtifactUpdateTime);

        // Read the changes log file and assert its content
        String changesLogContent = Files.readString(changesLogPath);
        assertTrue(changesLogContent.contains("Updated: guava.jar (com.google.guava:guava:33.1.0-jre)"));
    }

    @Test
    void downloadArtifacts_shouldThrowExceptionWhenSpecFileDoesNotExist() {
        // Arrange
        Path nonExistentSpecFilePath = tempDir.resolve("nonexistent.txt");
        String sourceTargetFolder = sourceTargetFolderPath.toString();
        boolean updateNewOnly = false;

        // Act & Assert
        assertThrows(VerifyException.class, () -> {
            ArtifactDownloader.downloadArtifacts(nonExistentSpecFilePath, targetFolderPath, sourceTargetFolder,
                    updateNewOnly, changesLogPath);
        });
    }

    @Test
    void downloadArtifacts_shouldThrowExceptionWhenTargetFolderDoesNotExist() {
        // Arrange
        Path nonExistentTargetFolderPath = tempDir.resolve("nonexistent");
        String sourceTargetFolder = sourceTargetFolderPath.toString();
        boolean updateNewOnly = false;

        // Act & Assert
        assertThrows(VerifyException.class, () -> {
            ArtifactDownloader.downloadArtifacts(specFilePath, nonExistentTargetFolderPath, sourceTargetFolder,
                    updateNewOnly, changesLogPath);
        });
    }

    @Test
    void downloadArtifacts_shouldThrowExceptionWhenSourceTargetFolderDoesNotExist() {
        // Arrange
        String nonExistentSourceTargetFolder = tempDir.resolve("nonexistent").toString();
        boolean updateNewOnly = false;

        // Act & Assert
        assertThrows(VerifyException.class, () -> {
            ArtifactDownloader.downloadArtifacts(specFilePath, targetFolderPath, nonExistentSourceTargetFolder,
                    updateNewOnly, changesLogPath);
        });
    }

    @Test
    void createArtifactPath() {
        String baseUrl = "https://repo.example.com";
        Artifact artifact = new Artifact("com.example", "my-artifact", Optional.empty(), Optional.empty());

        String expectedPath = "https://repo.example.com/com/example/my-artifact";
        String actualPath = ArtifactDownloader.createArtifactPath(baseUrl, artifact);

        assertEquals(expectedPath, actualPath);
    }

    @Test
    void parseSpecFile() throws IOException {

        // Create a temporary spec file
        Path specFilePath = tempDir.resolve("spec.txt");
        String specFileContent = """
                com.google.guava:guava:30.1-jre
                com.example:artifact1
                invalid-entry
                """;
        Files.writeString(specFilePath, specFileContent);

        // Call the parseSpecFile method
        List<Artifact> artifacts = ArtifactDownloader.parseSpecFile(specFilePath);

        // Assert the expected artifacts
        assertEquals(2, artifacts.size());

        Artifact artifact1 = artifacts.get(0);
        assertEquals("com.google.guava", artifact1.groupId());
        assertEquals("guava", artifact1.artifactId());
        assertTrue(artifact1.version().isPresent());
        assertEquals("30.1-jre", artifact1.version().get());

        Artifact artifact2 = artifacts.get(1);
        assertEquals("com.example", artifact2.groupId());
        assertEquals("artifact1", artifact2.artifactId());
        assertTrue(artifact2.version().isEmpty());
    }

    @Test
    void parseSpecFile_WithInvalidFile() {
        // Create a non-existent spec file path
        Path specFilePath = tempDir.resolve("invalid.txt");

        // Call the parseSpecFile method and expect an exception
        assertThrows(RuntimeException.class, () -> ArtifactDownloader.parseSpecFile(specFilePath));
    }
}