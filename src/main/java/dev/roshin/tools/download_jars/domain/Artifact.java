package dev.roshin.tools.download_jars.domain;

import java.util.Optional;

public record Artifact(String groupId, String artifactId, Optional<String> version) {
}
