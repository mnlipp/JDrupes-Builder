package org.jdrupes.builder.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Provider;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;

public class DefaultProject implements Provider, Project {

    private final Project parent;
    private final String name;
    private final Path directory;
    private final List<Provider<?>> providers = new ArrayList<>();
    private final List<Provider<?>> dependencies = new ArrayList<>();
    private Build build;

    public DefaultProject(Project parent, String name, Path directory) {
        this.parent = parent;
        if (name == null) {
            name = getClass().getSimpleName();
        }
        this.name = name;
        if (directory == null) {
            directory = Paths.get("").toAbsolutePath().getParent().resolve(name)
                .toAbsolutePath();
        }
        this.directory = directory.toAbsolutePath();
        if (!Files.exists(directory)) {
            throw new IllegalStateException(
                "Invalid project path: " + directory);
        }
        if (parent != null) {
            parent.dependency(this);
        }
    }

    public DefaultProject(Project parent, String name) {
        this(parent, name, null);
    }

    public DefaultProject(Project parent, Path directory) {
        this(parent, null, directory);
    }

    public DefaultProject(Project parent) {
        this(parent, null, null);
    }

    @Override
    public Optional<Project> parent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public Project rootProject() {
        if (parent == null) {
            return this;
        }
        return parent.rootProject();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Path directory() {
        return directory;
    }

    @Override
    public Path buildDirectory() {
        return directory.resolve("build");
    }

    @Override
    public Project provider(Provider<?> provider) {
        providers.add(provider);
        return this;
    }

    @Override
    public Project providers(List<Provider<?>> providers) {
        this.providers.clear();
        this.providers.addAll(providers);
        return this;
    }

    @Override
    public Project dependency(Provider<?> provider) {
        if (!dependencies.contains(provider)) {
            dependencies.add(provider);
        }
        return this;
    }

    @Override
    public Project dependencies(List<Provider<?>> providers) {
        this.dependencies.clear();
        this.dependencies.addAll(providers);
        return this;
    }

    @Override
    public Resources<?> provided(Resource resource) {
        return dependencies.stream().map(p -> build().provide(p, resource))
            .map(Resources::stream).flatMap(r -> r)
            .collect(Resources.into(ResourceSet::new));
    }

    @Override
    public Resources<? extends Resource> provide(Resource resource) {
        return Stream.concat(
            dependencies.stream().map(p -> build().provide(p, resource)),
            providers.stream().map(p -> build().provide(p, resource)))
            .toList().stream()
            .map(Resources::stream).flatMap(r -> r)
            .collect(Resources.into(ResourceSet::new));
    }

    @Override
    public Build build() {
        if (parent != null) {
            return parent.build();
        }
        if (build != null) {
            return build;
        }
        return build = new DefaultBuild();
    }
}
