package dev.roshin.tools;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import dev.roshin.tools.config.Config;
import dev.roshin.tools.download_jars.ArtifactDownloader;
import dev.roshin.tools.pom_generator.PomGenerator;
import dev.roshin.tools.userlibs_combiner.UserLibrariesMerger;
import dev.roshin.tools.userlibs_generator.UserLibrariesGenerator;
import dev.roshin.tools.util.AnsiLogger;
import dev.roshin.tools.util.CommonUtils;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "jar-manager", mixinStandardHelpOptions = true,
        description = "JAR Manager Command-Line Utility.",
        subcommands = {
                JarManagerCLI.GeneratePom.class,
                JarManagerCLI.DownloadJars.class,
                JarManagerCLI.GenerateUserLibs.class,
                JarManagerCLI.CombineUserLibs.class
        })
public class JarManagerCLI implements Callable<Integer> {

    @Override
    public Integer call() {
        // This method can be used to handle command logic not specific to subcommands
        System.out.println("Welcome to JAR Manager! Use -h for help.");
        return 0;
    }

    /**
     * Base command class that provides common options and methods.
     */
    abstract static class BaseCommand implements Callable<Integer> {
        @Option(names = {"-c", "--config"}, description = "Path to the configuration file.")
        protected String configPath;

        protected Logger logger;

        protected BaseCommand() {
            logger = LoggerFactory.getLogger(getClass());
            CommonUtils.printLogLocation();
        }

        protected void loadConfig() {
            if (!Strings.isNullOrEmpty(configPath)) {
                loadConfig(configPath);
            }
        }

        /**
         * Load configuration from the external file.
         *
         * @param configPath Path to the configuration file.
         */
        private static void loadConfig(String configPath) {

            Preconditions.checkArgument(!Strings.isNullOrEmpty(configPath), "Config path cannot be null or empty.");
            Path configFilePath = Paths.get(configPath);
            Verify.verify(Files.exists(configFilePath), "Config file does not exist: %s", configFilePath);
            AnsiLogger.info("Loading configuration from: {}", configFilePath.toAbsolutePath());
            // Load configuration from the external file, overriding defaults
            Config.getInstance().loadExternalConfig(configFilePath);
        }
    }

    @Command(name = "generate-pom", description = "Generate POM XML from JARs.")
    static class GeneratePom extends BaseCommand {
        @Option(names = {"--folder-path"}, description = "Path to the folder containing .jar files.", required = true)
        private String folderPath;

        @Option(names = {"--output-path"}, description = "Path to output the generated XML file.", required = true)
        private String outputPath;

        @Option(names = {"--additional-info"}, description = "Generate an additional text file with groupid:artifactname:version.")
        private boolean additionalInfo;

        @Option(names = {"--email-friendly-format"}, description = """
                When used in conjunction with --generate-artifact-list, entries in the additional text file
                  are formatted in a more readable, email-friendly manner. This format includes details such as group ID, artifact ID,
                  version, and a URL to the artifact. Each entry is separated by a separator to enhance clarity when viewed
                  in an email""")
        private boolean additionalFileEmailFriendlyFormat;

        @Override
        public Integer call() {
            AnsiLogger.info("Generating POM XML for JARs in: {}", folderPath);
            loadConfig();
            Preconditions.checkArgument(!Strings.isNullOrEmpty(folderPath), "Folder path cannot be null or empty.");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(outputPath), "Output path cannot be null or empty.");
            try {
                // Call the POM generator utility
                PomGenerator.generatePomEntries(Paths.get(folderPath), Paths.get(outputPath), additionalInfo, additionalFileEmailFriendlyFormat);
            } catch (Exception e) {
                AnsiLogger.error("Failed to generate POM XML: {}", e.getMessage());
                logger.error("Failed to generate POM XML", e);
                return 1;
            }

            return 0;
        }
    }

    @Command(name = "download-jars", description = "Download JARs based on specifications.")
    static class DownloadJars extends BaseCommand {
        @Parameters(index = "0", description = "Path to the specification text file.")
        private String specFile;

        @Option(names = {"--target-folder"}, description = "Path to the target folder where the downloaded JARs will be saved.", required = true)
        private String targetFolder;

        @Option(names = {"--source-target-folder"}, description = "Path where source JAR files will be downloaded.")
        private String sourceTargetFolder;

        @Option(names = {"--update-different-only"}, description = "Only replace JAR files if a different version was requested.")
        private boolean updateDifferentOnly;

        @Option(names = {"--changes-log"}, description = "Path to the changes text file that will be appended to.")
        private String changesLog;

        @Override
        public Integer call() {
            System.out.println("Downloading JARs based on specifications from: " + specFile);
            loadConfig();
            Preconditions.checkArgument(!Strings.isNullOrEmpty(specFile), "Spec path cannot be null or empty.");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(targetFolder), "Output path cannot be null or empty.");
            try {
                // Call the JAR downloader utility
                ArtifactDownloader.downloadArtifacts(Paths.get(specFile), Paths.get(targetFolder), sourceTargetFolder,
                        updateDifferentOnly, Paths.get(changesLog));
            } catch (Exception e) {
                AnsiLogger.error("Failed to download JARs: {}", e.getMessage());
                logger.error("Failed to download JARs", e);
                return 1;
            }


            return 0;
        }
    }

    @Command(name = "generate-userlibs", description = "Generate user libraries XML.")
    static class GenerateUserLibs extends BaseCommand {

        @Parameters(index = "0", description = "Name of the user library.")
        private String libraryName;

        @Parameters(index = "1", description = "Path to the specification text file.")
        private String specFile;

        @Option(names = {"--output-xml"}, description = "Path to output the user libraries XML file.", required = true)
        private String outputXml;

        @Option(names = {"--jars-path"}, description = "Path where JAR files are stored.", required = true)
        private String jarsPath;

        @Option(names = {"--jars-source-path"}, description = "Path where source JAR files are stored.")
        private String jarsSourcePath;

        @Option(names = {"--changes-log"}, description = "Path to the changes text file that will be appended to.")
        private String changesLog;

        @Override
        public Integer call() {
            System.out.println("Generating user libraries XML to: " + outputXml);
            loadConfig();
            // Add logic to generate or manage XML and JAR files
            Preconditions.checkArgument(!Strings.isNullOrEmpty(libraryName), "Library name cannot be null or empty.");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(specFile), "Spec path cannot be null or empty.");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(outputXml), "Output path cannot be null or empty.");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(jarsPath), "JAR target path cannot be null or empty.");
            try {
                // Call the user libraries generator utility
                UserLibrariesGenerator.generateUserLibs(libraryName, Paths.get(specFile), Paths.get(outputXml),
                        Paths.get(jarsPath), jarsSourcePath, Paths.get(changesLog));
            } catch (Exception e) {
                AnsiLogger.error("Failed to generate user libraries XML: {}", e.getMessage());
                logger.error("Failed to generate user libraries XML", e);
                return 1;
            }
            return 0;
        }
    }

    @Command(name = "combine-userlibs", description = "Combine multiple user libraries XML files.")
    static class CombineUserLibs extends BaseCommand {

        @Parameters(index = "0", description = "Paths to the XML files to combine.", split = " ")
        private Path[] xmls;

        @Option(names = {"--output-xml"}, description = "Path to output the combined user libraries XML file.", required = true)
        private String outputXml;

        @Override
        public Integer call() {
            System.out.println("Combining user libraries XML files to: " + outputXml);
            loadConfig();
            // Add logic to combine XML files
            Preconditions.checkArgument(xmls != null && xmls.length > 1, "At least two XML files are required to combine.");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(outputXml), "Output path cannot be null or empty.");
            try {
                // Add logic to combine XML files
                UserLibrariesMerger.mergeUserLibraries(Paths.get(outputXml), List.of(xmls));
            } catch (Exception e) {
                AnsiLogger.error("Failed to combine user libraries XML: {}", e.getMessage());
                logger.error("Failed to combine user libraries XML", e);
                return 1;
            }
            return 0;
        }
    }

    public static void main(String[] args) {
        AnsiConsole.systemInstall(); // enable colors on Windows
        int exitCode = new CommandLine(new JarManagerCLI()).execute(args);
        AnsiConsole.systemUninstall(); // cleanup when done
        System.exit(exitCode);
    }


}
