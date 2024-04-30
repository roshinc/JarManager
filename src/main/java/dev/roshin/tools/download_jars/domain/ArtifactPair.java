package dev.roshin.tools.download_jars.domain;

import java.util.Optional;

public record ArtifactPair(Artifact artifact, Optional<Artifact> sourceArtifact) {
}
