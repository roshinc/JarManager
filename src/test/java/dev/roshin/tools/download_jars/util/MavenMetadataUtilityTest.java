package dev.roshin.tools.download_jars.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MavenMetadataUtilityTest {

    @Test
    void getLatestVersion() {
        String baseUrl = "https://repo1.maven.org/maven2";
        String groupId = "com.google.guava";
        String artifactId = "guava";
        String apiKey = "";
        String artifactParentPath = String.format("%s/%s", baseUrl,
                groupId.replace('.', '/') + "/" + artifactId);
        String latestVersion = MavenMetadataUtility.getLatestVersion(artifactParentPath, apiKey).orElseThrow();
        System.out.println("Latest Version: " + latestVersion);
        assertEquals("33.1.0-jre", latestVersion);
    }


}