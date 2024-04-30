package dev.roshin.tools.download_jars.domain;

import java.nio.file.Path;
import java.util.Optional;

public record Artifact(String groupId, String artifactId, Optional<String> version,
                       Optional<Path> localJarPath) {
}
