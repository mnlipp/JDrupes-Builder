/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jdrupes.builder.mvnrepo;

import java.util.stream.Collectors;

/// Converts between maven models and aether dependencies.
///
public final class DependencyConverter {

    private DependencyConverter() {
        // utility class
    }

    /// Convert an aether dependency to a maven model dependency.
    ///
    /// @param aetherDep the aether dependency
    /// @return the maven model dependency
    ///
    public static org.apache.maven.model.Dependency
            convert(org.eclipse.aether.graph.Dependency aetherDep) {
        org.eclipse.aether.artifact.Artifact artifact = aetherDep.getArtifact();

        // Create base model
        org.apache.maven.model.Dependency modelDependency
            = new org.apache.maven.model.Dependency();
        modelDependency.setGroupId(artifact.getGroupId());
        modelDependency.setArtifactId(artifact.getArtifactId());
        modelDependency.setVersion(artifact.getVersion());
        modelDependency.setScope(aetherDep.getScope());
        modelDependency.setOptional(aetherDep.isOptional());

        // Get type from extension
        modelDependency.setType(artifact.getExtension() != null
            && !artifact.getExtension().isEmpty() ? artifact.getExtension()
                : "jar");

        // Optionally copy classifier
        if (artifact.getClassifier() != null
            && !artifact.getClassifier().isEmpty()) {
            modelDependency.setClassifier(artifact.getClassifier());
        }

        // Copy exclusions
        if (!aetherDep.getExclusions().isEmpty()) {
            modelDependency.setExclusions(
                aetherDep.getExclusions().stream()
                    .map(e -> {
                        org.apache.maven.model.Exclusion exclusion
                            = new org.apache.maven.model.Exclusion();
                        exclusion.setGroupId(e.getGroupId());
                        exclusion.setArtifactId(e.getArtifactId());
                        return exclusion;
                    })
                    .collect(Collectors.toList()));
        }
        return modelDependency;
    }

    /// Convert a maven model dependency to an aether dependency.
    ///
    /// @param modelDep the maven model dependency
    /// @return the aether dependency
    ///
    public static org.eclipse.aether.graph.Dependency
            convert(org.apache.maven.model.Dependency modelDep) {
        // Get type from extension
        String extension
            = modelDep.getType() != null && !modelDep.getType().isEmpty()
                ? modelDep.getType()
                : "jar";

        // Create aether base artifact
        org.eclipse.aether.artifact.Artifact artifact
            = new org.eclipse.aether.artifact.DefaultArtifact(
                modelDep.getGroupId(), modelDep.getArtifactId(),
                modelDep.getClassifier(), extension, modelDep.getVersion());

        // Create aether dependency
        return new org.eclipse.aether.graph.Dependency(
            artifact, modelDep.getScope(), modelDep.isOptional(),
            modelDep.getExclusions().stream()
                .map(x -> new org.eclipse.aether.graph.Exclusion(
                    x.getGroupId(), x.getArtifactId(), "*", "*"))
                .collect(Collectors.toList()));
    }

    /// Convert a maven model dependency to an aether dependency.
    ///
    /// @param mvnResource the maven resource
    /// @param scope the scope of aether dependency
    /// @return the aether dependency
    ///
    public static org.apache.maven.model.Dependency
            convert(MvnRepoDependency mvnResource, String scope) {
        var dep = new org.apache.maven.model.Dependency();
        dep.setGroupId(mvnResource.groupId());
        dep.setArtifactId(mvnResource.artifactId());
        dep.setType(
            mvnResource.mvnType().isBlank() ? "jar" : mvnResource.mvnType());
        if (!mvnResource.classifier().isBlank()) {
            dep.setClassifier(mvnResource.classifier());
        }
        if (!mvnResource.version().isBlank()) {
            dep.setVersion(mvnResource.version());
        }
        dep.setScope(scope);
        return dep;
    }

}
