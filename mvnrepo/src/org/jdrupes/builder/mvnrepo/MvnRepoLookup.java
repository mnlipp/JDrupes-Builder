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
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.core.DefaultFileResource;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.java.JavaResourceFactory;

import static org.jdrupes.builder.java.JavaTypes.*;

public class MvnRepoLookup implements ResourceProvider<JarFile> {

    private List<String> coordinates = new ArrayList<>();

    public MvnRepoLookup() {
    }

    public MvnRepoLookup artifact(String coordinate) {
        coordinates.add(coordinate);
        return this;
    }

    @Override
    public <T extends Resource> Stream<T> provide(ResourceRequest<T> request) {
        if (!request.type().isAssignableFrom(JarFileType)) {
            return Stream.empty();
        }

        ContextOverrides overrides = ContextOverrides.create()
            .withUserSettings(true).build();
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(overrides)) {
            DefaultArtifact artifact = new DefaultArtifact(coordinates.get(0));
            CollectRequest collectRequest = new CollectRequest()
                .setRepositories(context.remoteRepositories())
                .addDependency(new Dependency(artifact, null));

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
                return (Stream<T>) dependencyNodes.stream()
                    .filter(d -> d.getArtifact() != null)
                    .map(d -> d.getArtifact().getFile().toPath())
                    .map(p -> ResourceFactory.create(JarFileType, p));
            } catch (DependencyResolutionException e) {
                throw new BuildException(
                    "Cannot resolve: " + e.getMessage(), e);
            }
        }
    }

}
