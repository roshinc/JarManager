package dev.roshin.tools.config;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    private static Config instance;
    private final Properties config = new Properties();

    private Config() {
        // Load default configuration from resources
        try (InputStream resourceStream = Config.class.getResourceAsStream("/config.properties")) {
            if (resourceStream != null) {
                config.load(resourceStream);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load default configuration from resources.", e);
        }
    }

    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    public void loadExternalConfig(Path configFilePath) {
        // Load configuration from the external file, overriding defaults
        try (InputStream fis = Files.newInputStream(configFilePath)) {
            Properties externalConfig = new Properties();
            externalConfig.load(fis);
            config.putAll(externalConfig); // Override the defaults with the external config
        } catch (IOException e) {
            LoggerFactory.getLogger(Config.class).error("Failed to load external configuration file: {}", configFilePath, e);
        }
    }

    private String getProperty(String key) {
        return config.getProperty(key);
    }

    public String getMavenBaseUrl() {
        return getProperty("base.url");
    }
}
