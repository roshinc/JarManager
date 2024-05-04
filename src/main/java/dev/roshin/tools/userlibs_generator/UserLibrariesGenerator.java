package dev.roshin.tools.userlibs_generator;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import dev.roshin.tools.download_jars.domain.Artifact;
import dev.roshin.tools.download_jars.domain.ArtifactPair;
import dev.roshin.tools.pom_generator.PomGenerator;
import dev.roshin.tools.util.AnsiLogger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserLibrariesGenerator {
    public static void generateUserLibs(String libraryName, Path specFIlePath, Path outputXml,
                                        Path jarsPath, String jarsSourcePath, String changeLogFile) {
        Logger logger = LoggerFactory.getLogger(UserLibrariesGenerator.class);
        logger.info("Starting UserLibs generation...");
        Preconditions.checkArgument(Files.exists(specFIlePath), "Spec file does not exist: "
                + specFIlePath);
        Preconditions.checkArgument(Files.exists(outputXml.getParent()), "Output XML file does not exist: "
                + outputXml);

        logger.info("Spec file: {}", specFIlePath);

        // Check if we need to get the source jars
        boolean includeSource = false;
        Path sourcePath = null;
        if (jarsSourcePath != null) {
            includeSource = true;
            sourcePath = Paths.get(jarsSourcePath);
            logger.info("Source JARs will be included.");
            Verify.verify(Files.exists(sourcePath), "Source JARs path does not exist: " + sourcePath);
        }

        // Get the list of JARs in target folder
        File dir = jarsPath.toFile();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (files == null) {
            AnsiLogger.error("No JAR files found in target folder: {}", jarsPath);
            return;
        }
        List<Artifact> artifacts = PomGenerator.createArtifactList(files, true);
        List<Artifact> sourceArtifacts = null;
        if (includeSource) {
            File[] sourceFiles = sourcePath.toFile().listFiles((d, name) -> name.endsWith("-sources.jar"));
            if (sourceFiles == null) {
                AnsiLogger.error("No source JAR files found in source folder: {}", sourcePath);
                return;
            }
            sourceArtifacts = PomGenerator.createArtifactList(sourceFiles, false);
        }

        // Match the JARs and its sources
        List<ArtifactPair> jarPairs = matchJarsAndSources(artifacts, sourceArtifacts);

        generateUserLibraries(jarPairs, libraryName, outputXml, changeLogFile);


    }

    public static void generateUserLibraries(List<ArtifactPair> artifactPairs, String libraryName, Path outputPath, String changelogPath) {
        Logger logger = LoggerFactory.getLogger(UserLibrariesGenerator.class);
        Document document = new Document();
        Element root = new Element("eclipse-userlibraries");
        root.setAttribute("version", "2");
        document.setRootElement(root);

        Element library = new Element("library");
        library.setAttribute("name", libraryName);
        library.setAttribute("systemlibrary", "false");

        for (ArtifactPair pair : artifactPairs) {
            Artifact artifact = pair.artifact();
            Optional<Artifact> sourceArtifact = pair.sourceArtifact();

            String jarPath = artifact.localJarPath().map(path -> path.toString().replace("\\", "/")).orElse("");
            String sourcePath = sourceArtifact.flatMap(Artifact::localJarPath).map(path -> path.toString().replace("\\", "/")).orElse("");

            Element archive = new Element("archive");
            archive.setAttribute("path", jarPath);
            if (!sourcePath.isEmpty()) {
                archive.setAttribute("source", sourcePath);
            }
            library.addContent(archive);
        }

        root.addContent(library);

        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            outputter.output(document, writer);
            AnsiLogger.info("User libraries file generated successfully: {}", outputPath);
        } catch (IOException e) {
            AnsiLogger.error("Error generating user libraries file: {}", e.getMessage());
            logger.error("Error generating user libraries file", e);
        }

        // Add code to write changelog if needed
    }

    /**
     * Match the JARs and its sources
     *
     * @param artifacts       List of JAR artifacts
     * @param sourceArtifacts List of source JAR artifacts
     * @return List of ArtifactPair
     */
    private static List<ArtifactPair> matchJarsAndSources(List<Artifact> artifacts, List<Artifact> sourceArtifacts) {

        // If the artifact list is empty, return empty list
        if (artifacts.isEmpty()) {
            return List.of();
        }

        List<ArtifactPair> pairs = new ArrayList<>();

        // if the source artifact list is empty, or null, return the list of artifacts with empty source
        if (sourceArtifacts == null || sourceArtifacts.isEmpty()) {
            for (Artifact artifact : artifacts) {
                pairs.add(new ArtifactPair(artifact, Optional.empty()));
            }
            return pairs;
        }

        for (Artifact artifact : artifacts) {
            // Check if there is a source artifact for the current artifact
            Artifact sourceArtifact = sourceArtifacts.stream()
                    .filter(a -> a.groupId().equals(artifact.groupId()) && a.artifactId().
                            equals(artifact.artifactId()) && a.version().equals(artifact.version()))
                    .findFirst()
                    .orElse(null);
            pairs.add(new ArtifactPair(artifact, Optional.ofNullable(sourceArtifact)));
        }

        return pairs;
    }
}
