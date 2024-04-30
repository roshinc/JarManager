package dev.roshin.tools.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @TempDir
    Path tempDir;

    String defaultUrl = "https://repo1.maven.org/maven2/";

    private Config config;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        config = Config.getInstance();
        resetConfig();
    }

    @Test
    void testDefaultConfiguration() {
        // Test the default configuration
        assertEquals(defaultUrl, config.getMavenBaseUrl());
    }

    @Test
    void testExternalConfigurationLoading() throws IOException {
        // Create a temporary external configuration file
        Path externalConfigFile = tempDir.resolve("external-config.properties");
        String externalConfig = "base.url=https://external-base-url.com";
        Files.writeString(externalConfigFile, externalConfig);

        // Load the external configuration
        config.loadExternalConfig(externalConfigFile);

        // Test the overridden configuration
        assertEquals("https://external-base-url.com", config.getMavenBaseUrl());
    }

    @Test
    void testExternalConfigurationLoadingWithInvalidFile() {
        // Create a non-existent external configuration file path
        Path invalidConfigFile = tempDir.resolve("invalid-config.properties");

        // Load the external configuration (should log an error)
        config.loadExternalConfig(invalidConfigFile);

        // Test that the default configuration is still used
        assertEquals(defaultUrl, config.getMavenBaseUrl());
    }

    @Test
    void testSingletonInstance() {
        Config instance1 = Config.getInstance();
        Config instance2 = Config.getInstance();

        // Test that both instances are the same object
        assertSame(instance1, instance2);
    }

    private void resetConfig() throws NoSuchFieldException, IllegalAccessException {
        Field configField = Config.class.getDeclaredField("config");
        configField.setAccessible(true);
        Properties defaultConfig = new Properties();
        defaultConfig.setProperty("base.url", defaultUrl);
        configField.set(config, defaultConfig);
    }
}