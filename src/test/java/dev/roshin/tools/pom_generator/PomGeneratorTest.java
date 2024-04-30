package dev.roshin.tools.pom_generator;

import dev.roshin.tools.config.Config;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PomGeneratorTest {

    @TempDir
    Path tempDir;

    @Mock
    private Config config;

    private AutoCloseable mocks;

    @Mock
    private BufferedWriter writer;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void generatePomEntries() {
    }

    @Test
    void writePomEntry() {
    }

    @Test
    public void writeAdditionalEntry() throws IOException {
        String testGroupId = "com.example";
        String testArtifactId = "demo-artifact";
        String testVersion = "1.0.0";
        String testBaseUrl = "http://maven.repo.example.com/";

        when(config.getMavenBaseUrl()).thenReturn(testBaseUrl);

        try (MockedStatic<Config> configMockedStatic = Mockito.mockStatic(Config.class)) {
            configMockedStatic.when(Config::getInstance).thenReturn(config);
            when(config.getMavenBaseUrl()).thenReturn(testBaseUrl);
            PomGenerator.writeAdditionalEntry(writer, testGroupId, testArtifactId, testVersion, true);

            // Expected output
            String expected = String.format("Group ID: %s\nArtifact ID: %s\nVersion: %s\nURL: %s%s/%s/%s/%s-%s.jar\n--------------------------------------------------------\n",
                    testGroupId, testArtifactId, testVersion, testBaseUrl, testGroupId.replace('.', '/'), testArtifactId, testVersion, testArtifactId, testVersion);

            // Verify the writer.write() method was called with the expected output
            verify(writer).write(expected);
        }
    }

    @Test
    void writeAdditionalEntry_conciseFormat() throws IOException {
        // Arrange
        String groupId = "com.example";
        String artifactId = "my-artifact";
        String version = "1.0.0";
        boolean emailFriendlyFormat = false;
        String expectedEntry = "com.example:my-artifact:1.0.0\n";
        try (MockedStatic<Config> configMockedStatic = Mockito.mockStatic(Config.class)) {
            configMockedStatic.when(Config::getInstance).thenReturn(config);
            // Act
            PomGenerator.writeAdditionalEntry(writer, groupId, artifactId, version, emailFriendlyFormat);

            // Assert
            verify(writer).write(expectedEntry);
        }
    }

    @Test
    void extractPomProperties() throws IOException {
        // Create a temporary JAR file with pom.properties
        Path jarPath = tempDir.resolve("test.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("META-INF/maven/com.example/test/pom.properties"));
            Properties props = new Properties();
            props.setProperty("groupId", "com.example");
            props.setProperty("artifactId", "test");
            props.setProperty("version", "1.0.0");
            props.store(jos, null);
            jos.closeEntry();
        }

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Properties extractedProps = PomGenerator.extractPomProperties(jar);
            assertNotNull(extractedProps, "Extracted properties should not be null");
            assertEquals("com.example", extractedProps.getProperty("groupId"));
            assertEquals("test", extractedProps.getProperty("artifactId"));
            assertEquals("1.0.0", extractedProps.getProperty("version"));
        }
    }

    @Test
    void extractPomProperties_NotFound() throws IOException {
        // Create a temporary JAR file without pom.properties
        Path jarPath = tempDir.resolve("test.jar");

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            jos.closeEntry();
        }

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Properties extractedProps = PomGenerator.extractPomProperties(jar);
            assertNull(extractedProps, "Extracted properties should be null");
        }
    }


}