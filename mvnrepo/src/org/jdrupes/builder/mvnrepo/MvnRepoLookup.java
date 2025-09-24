/*
 * JDrupes Builder
 * Copyright (C) 2025 Michael N. Lipp
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

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.java.JavaTypes.*;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

/// The Class MvnRepoLookup.
///
public class MvnRepoLookup implements ResourceProvider {

    private final Project project;
    private final List<String> coordinates = new ArrayList<>();

    /// Instantiates a new mvn repo lookup.
    ///
    /// @param project the project
    ///
    public MvnRepoLookup(Project project) {
        this.project = project;
    }

    /// Artifact.
    ///
    /// @param coordinate the coordinate
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup artifact(String coordinate) {
        coordinates.add(coordinate);
        return this;
    }

    /// Provide.
    ///
    /// @param <T> the generic type
    /// @param requested the requested resources
    /// @return the stream
    ///
    @Override
    public <T extends Resource> Stream<T>
            provide(ResourceRequest<T> requested) {
        if (requested.wants(MvnRepoDependenciesType)) {
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) coordinates.stream()
                .map(c -> project.newResource(MvnRepoDependencyType, c));
            return result;
        }
        if (requested.wants(CompilationResourcesType)
            && requested.includes(JarFileType)) {
            return provideJars(requested);
        }
        return Stream.empty();
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private <T extends Resource> Stream<T>
            provideJars(ResourceRequest<T> requested) {
        ContextOverrides overrides = ContextOverrides.create()
            .withUserSettings(true).build();
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(overrides)) {
            CollectRequest collectRequest = new CollectRequest()
                .setRepositories(context.remoteRepositories());
            for (var coord : coordinates) {
                collectRequest.addDependency(
                    new Dependency(new DefaultArtifact(coord),
                        requested.wants(CompilationResourcesType) ? "compile"
                            : "runtime"));
            }

            DependencyRequest dependencyRequest
                = new DependencyRequest(collectRequest, null);
            DependencyNode rootNode;
            try {
                rootNode = context.repositorySystem()
                    .resolveDependencies(context.repositorySystemSession(),
                        dependencyRequest)
                    .getRoot();
// For maven 2.x libraries:
//                List<DependencyNode> dependencyNodes = new ArrayList<>();
//                rootNode.accept(new PreorderDependencyNodeConsumerVisitor(
//                    dependencyNodes::add));
                PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
                rootNode.accept(nlg);
                List<DependencyNode> dependencyNodes = nlg.getNodes();
                @SuppressWarnings("unchecked")
                var result = (Stream<T>) dependencyNodes.stream()
                    .filter(d -> d.getArtifact() != null)
                    .map(d -> d.getArtifact().getFile().toPath())
                    .map(p -> ResourceFactory.create(JarFileType, p));
                return result;
            } catch (DependencyResolutionException e) {
                throw new BuildException(
                    "Cannot resolve: " + e.getMessage(), e);
            }
        }
    }

}
