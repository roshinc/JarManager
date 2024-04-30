package dev.roshin.tools.pom_generator;

import com.google.common.collect.Lists;
import dev.roshin.tools.config.Config;
import dev.roshin.tools.download_jars.domain.Artifact;
import dev.roshin.tools.util.AnsiLogger;
import dev.roshin.tools.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * PomGenerator is a utility class that generates a POM file with dependencies based on JAR files in a directory.
 */
public class PomGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PomGenerator.class);


    /**
     * Generates POM entries for JAR files in the specified directory and writes them to the target file.
     *
     * @param directoryPath                     The directory containing the JAR files.
     * @param targetFileName                    The name of the target file to write the POM entries to.
     * @param createAdditionalFile              Whether to create an additional file with URLs for the dependencies.
     *                                          If true, an additional file will be created with the same name as the
     *                                          target file but with the suffix ".additional".
     * @param additionalFileEmailFriendlyFormat Whether to create the additional file in an email-friendly format. Only used if
     *                                          {@code createAdditionalFile} is true.
     * @throws IOException If an I/O error occurs while reading the JAR files or writing to the target file.
     */
    public static void generatePomEntries(Path directoryPath, Path targetFileName, boolean createAdditionalFile,
                                          boolean additionalFileEmailFriendlyFormat) throws IOException {
        File dir = directoryPath.toFile();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".jar"));

        // Create the path for the additional file even though it may not be used
        Path additionalFilePath = null;

        if (files != null) {
            try (BufferedWriter writer = Files.newBufferedWriter(targetFileName)) {
                writer.write("<dependencies>\n");

                if (createAdditionalFile) {
                    additionalFilePath = targetFileName.resolveSibling(
                            Paths.get(CommonUtils.getFileNameWithoutExtension(targetFileName) + ".additional")
                    );
                    try (BufferedWriter additionalWriter = Files.newBufferedWriter(additionalFilePath)) {
                        writeEntries(writer, additionalWriter, files, additionalFileEmailFriendlyFormat);
                    } catch (IOException e) {
                        logger.error("An error occurred while creating the additional file: {}", additionalFilePath, e);
                    }
                } else {
                    writeEntries(writer, null, files, additionalFileEmailFriendlyFormat);
                }
                writer.write("</dependencies>\n");
            }
            AnsiLogger.success("POM entries written to: {}", targetFileName.toAbsolutePath());
            if (createAdditionalFile) {
                AnsiLogger.success("Additional information written to: {}", additionalFilePath.toAbsolutePath());
            }
        } else {
            AnsiLogger.warning(logger, "No JAR files found in the directory: {}", directoryPath);
        }
    }

    /**
     * Generates POM entries for the {@code File}s in the {@code files} array and writes them to the specified writer(s).
     *
     * @param writer                            The writer to write the POM entries to.
     * @param additionalWriter                  The writer to write the additional entries to, or null if no additional file is
     *                                          being created.
     * @param files                             The array of {@code File}s to generate POM entries for.
     * @param additionalFileEmailFriendlyFormat Whether to create the additional file in an email-friendly format.
     *                                          Only used if {@code additionalWriter} is not null.
     * @throws IOException If an I/O error occurs while writing to the writer(s).
     */
    private static void writeEntries(BufferedWriter writer, BufferedWriter additionalWriter, File[] files,
                                     boolean additionalFileEmailFriendlyFormat) throws IOException {
        List<Artifact> artifacts = createArtifactList(files);
        for (Artifact artifact : artifacts) {

            writePomEntry(writer, artifact);
            if (additionalWriter != null) {
                writeAdditionalEntry(additionalWriter, artifact, additionalFileEmailFriendlyFormat);
            }
        }
    }

    /**
     * Creates a list of {@code Artifact}s from the specified array of {@code File}s.
     *
     * @param files The array of {@code File}s to create {@code Artifact}s from.
     * @return A list of {@code Artifact}s created from the specified array of {@code File}s.
     */
    public static List<Artifact> createArtifactList(File[] files){
        List<Artifact> artifacts = Lists.newArrayList();
        for (File file : files) {
            if (file.getName().contains("sources")) {
                continue;
            }
            try (JarFile jar = new JarFile(file)) {
                Properties props = extractPomProperties(jar);
                if (props != null) {
                    String groupId = props.getProperty("groupId");
                    String artifactId = props.getProperty("artifactId");
                    String version = props.getProperty("version");
                    artifacts.add(new Artifact(groupId, artifactId, Optional.of(version), Optional.of(file.toPath())));
                }
            } catch (IOException e) {
                AnsiLogger.error("An error occurred while processing JAR file: {}", file.getName());
                logger.error("An error occurred while processing JAR file: {}", file.getName(), e);
            }
        }
        return artifacts;
    }

    /**
     * Writes a POM dependency entry to the specified writer.
     *
     * @param writer     The writer to write the POM entry to.
     * @param artifact   The {@code Artifact} to write the POM entry for.
     * @throws IOException If an I/O error occurs while writing to the writer.
     */
    protected static void writePomEntry(BufferedWriter writer, Artifact artifact)
            throws IOException{
        writePomEntry(writer, artifact.groupId(), artifact.artifactId(), artifact.version().orElse("latest"));
    }

    /**
     * Writes a POM dependency entry to the specified writer.
     *
     * @param writer     The writer to write the POM entry to.
     * @param groupId    The group ID of the dependency.
     * @param artifactId The artifact ID of the dependency.
     * @param version    The version of the dependency.
     * @throws IOException If an I/O error occurs while writing to the writer.
     */
    protected static void writePomEntry(BufferedWriter writer, String groupId, String artifactId, String version)
            throws IOException {
        String xmlEntry = String.format("""
                 <dependency>
                 <groupId>%s</groupId>
                 <artifactId>%s</artifactId>
                 <version>%s</version>
                 </dependency>
                """, groupId, artifactId, version);
        writer.write(xmlEntry);
    }

    /**
     * Writes an additional entry with the URL of the dependency to the specified writer.
     * If emailFriendlyFormat is true, the entry format is more detailed and includes a URL
     * with a separator for clarity in emails. If false, the format is concise, suitable for file storage.
     *
     * @param writer              The writer to write the additional entry to.
     * @param artifact             The {@code Artifact} to write the additional entry for.
     * @param emailFriendlyFormat Whether to create the additional file in an email-friendly format.
     * @throws IOException If an I/O error occurs while writing to the writer.
     */
    protected static void writeAdditionalEntry(BufferedWriter writer,Artifact artifact, boolean emailFriendlyFormat)
            throws IOException {
        writeAdditionalEntry(writer, artifact.groupId(), artifact.artifactId(), artifact.version()
                .orElse("latest"), emailFriendlyFormat);
    }

    /**
     * Writes an additional entry with the URL of the dependency to the specified writer.
     * If emailFriendlyFormat is true, the entry format is more detailed and includes a URL
     * with a separator for clarity in emails. If false, the format is concise, suitable for file storage.
     *
     * @param writer              The writer to write the additional entry to.
     * @param groupId             The group ID of the dependency.
     * @param artifactId          The artifact ID of the dependency.
     * @param version             The version of the dependency.
     * @param emailFriendlyFormat Whether to create the additional file in an email-friendly format.
     * @throws IOException If an I/O error occurs while writing to the writer.
     */
    protected static void writeAdditionalEntry(BufferedWriter writer, String groupId, String artifactId,
                                               String version, boolean emailFriendlyFormat) throws IOException {
        String baseUrl = Config.getInstance().getMavenBaseUrl();
        String artifactUrl = String.format("%s%s/%s/%s/%s-%s.jar", baseUrl,
                groupId.replace('.', '/'), artifactId, version, artifactId, version);

        if (emailFriendlyFormat) {
            String emailFormattedEntry = String.format("Group ID: %s\nArtifact ID: %s\nVersion: %s\nURL: %s",
                    groupId, artifactId, version, artifactUrl);
            String separator = "--------------------------------------------------------";
            writer.write(emailFormattedEntry + "\n" + separator + "\n");
        } else {
            String conciseEntry = String.format("%s:%s:%s", groupId, artifactId, version);
            writer.write(conciseEntry + "\n");
        }
    }


    /**
     * Extracts POM properties from the specified JAR file.
     *
     * @param jar The JAR file to extract POM properties from.
     * @return The extracted POM properties, or null if no POM properties are found.
     */
    protected static Properties extractPomProperties(JarFile jar) {
        String[] possibleLocations = {
                "META-INF/maven/",
                "META-INF/"
        };

        for (String baseDir : possibleLocations) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.getName().startsWith(baseDir) && entry.getName().endsWith("pom.properties")) {
                    Properties props = new Properties();
                    try {
                        props.load(jar.getInputStream(entry));
                        return props;
                    } catch (IOException e) {
                        logger.error("An error occurred while loading POM properties from JAR: {}", jar.getName(), e);
                    }
                }
            }
        }
        AnsiLogger.warning("No POM properties found in JAR: {}", jar.getName());
        logger.debug("No POM properties found in JAR: {}", jar.getName());
        return null;
    }


}