package dev.roshin.tools.download_jars;

import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import dev.roshin.tools.config.Config;
import dev.roshin.tools.config.util.MavenSettingsParser;
import dev.roshin.tools.download_jars.domain.Artifact;
import dev.roshin.tools.download_jars.util.MavenMetadataUtility;
import dev.roshin.tools.pom_generator.PomGenerator;
import dev.roshin.tools.util.AnsiLogger;
import dev.roshin.tools.util.ChangesFileUtil;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class ArtifactDownloader {

    /*
     * Download the artifact from the Maven repository.
     *
     * @param artifact The artifact to download.
     * @param existingArtifact The existing artifact in the target folder.
     * @param replaceOnlyIfDifferent Replace the existing artifact only if different.
     * @param artifactPath The path to the artifact.
     * @param targetFolderPath The target folder to download the artifact.
     * @param sourceTargetFolderPath The target folder to download the sources.
     * @param downloadSources Download the sources.
     * @param apiKey The API key to access the Maven repository.
     *
     * @return The downloaded artifact.
     */
    protected static Optional<Artifact> downloadArtifact(final Artifact artifact, final Artifact existingArtifact,
                                                       final boolean replaceOnlyIfDifferent, final String artifactPath,
                                                       final Path targetFolderPath, final Path sourceTargetFolderPath,
                                                       final boolean downloadSources, final boolean updateChangesLog,
                                                         final String apiKey) {
        Logger logger = LoggerFactory.getLogger(ArtifactDownloader.class);

        logger.info("Requested version of {} is {}", artifact.artifactId(), artifact.version());
        String versionString = artifact.version().orElse("latest");
        //Get the latest version of the artifact, if version is latest
        if (versionString.equals("latest")) {
            Optional<String> versionOptional = MavenMetadataUtility.getLatestVersion(artifactPath, apiKey);
            if (versionOptional.isPresent()) {
                versionString = versionOptional.get();
            } else {
                AnsiLogger.error(logger, "Failed to get the latest version of {}", artifact.artifactId());
                return Optional.empty();
            }
            AnsiLogger.info(logger,"Latest Version of {} is {}", artifact.artifactId(), versionString);
        }

        // Check if the artifact of the same version already exists, if we need to replace only if different
        if (replaceOnlyIfDifferent && existingArtifact != null && existingArtifact.version().isPresent() &&
                existingArtifact.version().get().equals(versionString)) {
            AnsiLogger.info(logger, "Artifact {} of version {} already exists, skipping download",
                    artifact.artifactId(), versionString);
            return Optional.of(existingArtifact);
        }

        // Url to download the artifact
        String downloadUrl = String.format("%s/%s/%s-%s.jar", artifactPath, versionString, artifact.artifactId(),
                versionString);
        logger.info("Downloading artifact from {}", downloadUrl);

        // Url to download the artifact sources
        String sourcesUrl = String.format("%s/%s/%s-%s-sources.jar", artifactPath, versionString, artifact.artifactId(),
                versionString);
        logger.info("Downloading sources from {}", sourcesUrl);

        // Download the artifact
        try {
            URI downloadUri = new URI(downloadUrl);
            HttpClientResponseHandler<byte[]> responseHandler = response -> {
                if (response.getCode() == 200) { // Check for HTTP 200 OK
                    return EntityUtils.toByteArray(response.getEntity());
                } else {
                    throw new RuntimeException("Failed to download artifact: HTTP " + response.getCode());
                }
            };

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(downloadUri);
                request.setHeader("X-JFrog-Art-Api", apiKey);

                String jarName = artifact.artifactId() + ".jar";

                byte[] artifactData = client.execute(request, responseHandler);
                try (FileOutputStream outstream = new FileOutputStream(targetFolderPath + "/" + jarName)) {
                    outstream.write(artifactData);
                }

                // Download the sources if required
                if (downloadSources) {
                    URI sourcesUri = new URI(sourcesUrl);
                    byte[] sourcesData = client.execute(new HttpGet(sourcesUri), responseHandler);
                    try (FileOutputStream outstream = new FileOutputStream(sourceTargetFolderPath + "/"
                            + artifact.artifactId() + "-sources.jar")) {
                        outstream.write(sourcesData);
                    }
                }

                // Update the changes log, if required
                if (updateChangesLog) {
                    // Figure out action
                    ChangesFileUtil.Action action = existingArtifact == null ? ChangesFileUtil.Action.ADDED
                            : ChangesFileUtil.Action.UPDATED;
                    String previousVersion = existingArtifact == null ? "" : existingArtifact.version()
                            .orElse("");
                    ChangesFileUtil.getInstance(Paths.get("")).addEntry(action, jarName, artifact.groupId(),
                            artifact.artifactId(), versionString, previousVersion);
                }

                // Create the artifact response object
                return Optional.of(new Artifact(artifact.groupId(), artifact.artifactId(), Optional.of(versionString)));
            }
        } catch (Exception e) {
            AnsiLogger.error(logger, "Error downloading artifact: {}", e.getMessage());
            logger.error("Error downloading artifact", e);
            return Optional.empty();
        }
    }


    /*
     * Download the artifacts specified in the spec file.
     *
     * @param specFilePath The path to the spec file.
     * @param targetFolderPath The target folder to download the artifacts.
     * @param sourceTargetFolder The target folder to download the sources.
     * @param updateDifferentOnly Replace the existing artifacts only if different.
     * @param changesLogPath The path to the changes log file.
     */
    public static void downloadArtifacts(final Path specFilePath, final Path targetFolderPath,
                                         final String sourceTargetFolder, final boolean updateDifferentOnly,
                                         final Path changesLogPath) {
        Logger logger = LoggerFactory.getLogger(ArtifactDownloader.class);

        // Check if spec file exists
        Verify.verify(Files.exists(specFilePath), "Spec file does not exist: %s", specFilePath);

        // Get the list of artifacts to download
        List<Artifact> artifacts = parseSpecFile(specFilePath);

        // Check if the target folder exists
        Verify.verify(Files.exists(targetFolderPath), "Target folder does not exist: %s",
                targetFolderPath);

        // We only need to download source files if the sourceTargetFolderPath is not null
        boolean downloadSources = false;
        Path sourceTargetFolderPath = null;
        if (!Strings.isNullOrEmpty(sourceTargetFolder)) {
            downloadSources = true;
            sourceTargetFolderPath = Paths.get(sourceTargetFolder);
            Verify.verify(Files.exists(sourceTargetFolderPath), "Source target folder does not" +
                            " exist: %s",
                    sourceTargetFolderPath);
        }

        // We only need to update the changelog if the changesLogPath is not null
        boolean updateChangesLog = false;
        if (changesLogPath != null) {
            updateChangesLog = true;
            Verify.verify(Files.exists(changesLogPath.getParent()), "Changes log file does " +
                    "not exist: %s", changesLogPath);
            ChangesFileUtil.getInstance(changesLogPath);
        }

        // If we are updating only new files, get the current list of files in the target folder
        List<Artifact> existingArtifacts = Lists.newArrayList();
        if (updateDifferentOnly) {
           // Get jar files in target folder
            File dir = targetFolderPath.toFile();
            File[] files = dir.listFiles((d, name) -> name.endsWith(".jar"));
            if (files == null) {
                AnsiLogger.warning(logger, "No JAR files found in target folder: {}, " +
                        "update only flag is ignored", targetFolderPath);
            } else {
                // Get artifacts in the target folder
                existingArtifacts = PomGenerator.createArtifactList(files);
                // Remove existing artifacts from the list
                // this only removes jars with versions, not the latest
                artifacts.removeAll(existingArtifacts);
            }
        }

        // Get the api key and base url from the configuration
        String baseUrl = Config.getInstance().getMavenBaseUrl();
        String apiKey = Config.getInstance().getMavenApiKey().orElse("");
        // Download the artifacts
        for (Artifact artifact : artifacts) {
            // Download the artifact
            Artifact existingArtifact = existingArtifacts.stream()
                    .filter(a -> a.artifactId().equals(artifact.artifactId()))
                    .findFirst()
                    .orElse(null);

            Optional<Artifact> downloadedArtifact = downloadArtifact(artifact,existingArtifact,
                    updateDifferentOnly, createArtifactPath(baseUrl, artifact), targetFolderPath, sourceTargetFolderPath,
                    downloadSources,updateChangesLog, apiKey);
        }

    }


    /*
    * Creates the artifact path from the base URL and the artifact.
    *
    * @param baseUrl The base URL of the Maven repository.
    * @param artifact The artifact to download.
    *
    * @return The path to the artifact.
     */
    protected static String createArtifactPath(String baseUrl, Artifact artifact) {
        return String.format("%s/%s", baseUrl,
                artifact.groupId().replace('.', '/') + "/" + artifact.artifactId());
    }


    /*
     * Parse the spec file and return a list of artifacts to download.
     * The spec file is assumed to exist.
     *
     * @param specFilePath The path to the spec file.
     * @return A list of artifacts to download.
     */
    protected static List<Artifact> parseSpecFile(Path specFilePath) {
        Logger logger = LoggerFactory.getLogger(ArtifactDownloader.class);
        // The file is assumed to exist
        List<Artifact> artifacts = Lists.newArrayList();

        // Parse the file and add the artifacts to the list
        // Example format: groupId:artifactId:version the version is optional
        // Example: com.google.guava:guava:30.1-jre
        // Example: com.google.guava:guava (latest version)
        try(BufferedReader reader = Files.newBufferedReader(specFilePath);) {
            String artifactLine;

            while ((artifactLine = reader.readLine()) != null) {
                String[] parts = artifactLine.split(":");
                if (parts.length == 2) {
                    String groupId = parts[0].trim();
                    String artifactId = parts[1].trim();
                    // Assume version is the latest
                    artifacts.add(new Artifact(groupId, artifactId, Optional.empty()));
                } else if(parts.length == 3) {
                    String groupId = parts[0].trim();
                    String artifactId = parts[1].trim();
                    String version = parts[2].trim();
                    artifacts.add(new Artifact(groupId, artifactId, Optional.of(version)));
                } else {
                    AnsiLogger.warning(logger, "Invalid artifact entry: {}", artifactLine);
                }
            }
        } catch (IOException e) {
            AnsiLogger.error(logger, "Failed to read artifact file: {}", e.getMessage());
            logger.error("Failed to read artifact file", e);
            throw new RuntimeException("Failed to read artifact file", e);
        }
        return artifacts;
    }
}
