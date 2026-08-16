# JDrupes-Builder (jdbld) — Agent Usage Guide

This document explains how to use the JDrupes-Builder build tool in a project
that consumes jdbld. It covers invoking the tool, understanding the build
configuration, and performing common tasks.

## What Is jdbld

jdbld is a build automation tool that:
- Uses **Java code** as its configuration language (no XML, no DSL).
- Models a build as a **collection of resources** produced on demand, not as
  an ordered sequence of tasks.
- Discovers capabilities via `ServiceLoader` per classloader.

## Invoking jdbld

The entry point is the `jdbld` shell script in the project root.

### Basic Commands

```bash
./jdbld build        # Build all artifacts (command name depends on the project)
./jdbld test         # Run all tests
./jdbld clean        # Remove generated resources
```

### CLI Flags

| Flag                        | Description                                          |
|-----------------------------|------------------------------------------------------|
| `-P key=value`              | Override a property from `.jdbld.properties`.        |
| `-h`              | List available commands and their descriptions.        |

### Bootstrap Mechanism

`jdbld` is a self-bootstrapping script:
1. It reads `.jdbld.properties` to find the jdbld version.
2. If `JDBLD_JAR` is set, it uses that JAR path directly.
3. Otherwise, it downloads the builder JAR from a Maven repository.
4. It compiles the build configuration (Java code in `_jdbld/src/`) using the
   downloaded JAR.
5. It runs the actual build in a new classloader with the compiled config.

## Project Structure

A typical project using jdbld:

```
project-root/
  .jdbld.properties          # Build properties (version, extensions, repos)
  jdbld                      # Bootstrap script
  _jdbld/
    src/jdbld/
      Root.java              # Main build configuration
      Api.java               # Sub-project definitions
      Impl.java
      App.java
    test/                    # Tests for the build configuration itself
    resources/               # Resources used by build config
  src/                       # Application source (per project convention)
  test/                      # Application tests
```

## `.jdbld.properties`

This file configures the build environment:

| Property                     | Description                                      |
|------------------------------|--------------------------------------------------|
| `jdbldDirectory`             | Directory with build config Java sources         |
|                              | (default: `_jdbld`)                              |
| `jdbldVersion`               | Version of the builder JAR to download           |
| `jdbldCommonDirectory`       | Shared cache directory (default: `~/.jdbld`)     |
| `javaHome`                   | Java home override                               |
| `extensionsRepositories`     | Comma-separated Maven repos for build extensions |
| `extensionsSnapshotRepository` | Snapshot repo for build extensions            |
| `buildExtensions`            | Comma-separated Maven coordinates of build-time  |
|                              | extensions loaded before the user config compiles |
| `runtimeExtensions`          | Comma-separated Maven coordinates of runtime     |
|                              | extensions loaded in the build-phase classloader  |

A user-level `~/.jdbld/jdbld.properties` can provide defaults overridden by
the project-level file.

## Build Configuration (Java Code)

The build configuration lives under `_jdbld/src/` and is written in Java.

### Root Project

The root project extends `AbstractRootProject` and defines commands:

```java
package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.core.AbstractRootProject;
import org.jdrupes.builder.java.JavaCompiler;
import static org.jdrupes.builder.java.JavaTypes.*;

public class Root extends AbstractRootProject {

    public Root() {
        super(name("my-project"));

        // Sub-projects
        dependency(Expose, project(Api.class));
        dependency(Expose, project(Impl.class));

        // Commands
        commandAlias("build")
            .description("Build all jars")
            .resources(of(LibraryJarFileType).using(Supply, Forward));
        commandAlias("test")
            .description("Run all tests")
            .projects("**")
            .resources(of(TestResultType).using(Supply));
    }

    @Override
    public void prepareProject(Project project) {
        // Applied to every project — good place for common setup
        if (project instanceof JavaProject) {
            project.generator(JavaCompiler::new)
                .addSources(Path.of("src"), "**/*.java")
                .options("--release", "25");
        }
    }
}
```

### Sub-Projects

Sub-projects extend `AbstractProject`:

```java
public class Api extends AbstractProject implements JavaProject {
    public Api() {
        super(name("api"), directory(Path.of("api")));
    }
}

public class Impl extends AbstractProject implements JavaProject {
    public Impl() {
        super(name("impl"), directory(Path.of("impl")));
        dependency(Expose, project(Api.class));
        dependency(Consume, new MvnRepoLookup()
            .resolve("com.google.guava:guava:33.0.0-jre"));
    }
}
```

### Test Projects (Merged)

A `MergedTestProject` shares its directory with the parent and compiles from
`test/` rather than `src/`:

```java
public class ImplTest extends AbstractProject
        implements JavaProject, MergedTestProject {
    public ImplTest() {
        super(parent(Impl.class));
        dependency(Consume, project(Impl.class));
        dependency(Supply, JUnitTestRunner::new);
    }
}
```

## Core Concepts

### Resources

A resource is the fundamental build unit. Key types:
- `FileResource` — a single file
- `FileTree<T>` — files matching a glob pattern
- `JavaSourceFile` / `JavaSourceTree` — Java source files/trees
- `ClassFile` / `ClassTree` — compiled class files/trees
- `ClasspathElement` / `Classpath` — classpath entries
- `JarFile` / `LibraryJarFile` / `AppJarFile` — JAR artifacts
- `TestResult` — test execution result
- `ProjectVersion` — version information
- `Cleanliness` — triggers cache invalidation (for `clean`)

Resources are requested via `of(ResourceType)`, e.g.
`of(LibraryJarFileType)` or `of(TestResultType)`.

Resource types are created as anonymous subclasses:
`new ResourceType<SomeType>() {}` — this captures the actual type for generics.

### Providers and Generators

- **Provider** — anything that can produce resources in response to a request.
  Extend `AbstractProvider` and implement `doProvide(ResourceRequest<T>)`.
- **Generator** — a provider that generates new artifacts. Extend
  `AbstractGenerator`. Must handle `CleanlinessType` for the `clean` command.

Add a generator to a project:
```java
project.generator(JavaCompiler::new)
    .addSources(Path.of("src"), "**/*.java")
    .options("--release", "25");
```

### Dependencies and Intents

Intents control resource visibility across the project hierarchy:

| Intent     | Used by own generators | Visible to dependents            |
|------------|------------------------|----------------------------------|
| `Supply`   | Yes                    | Yes                              |
| `Consume`  | Yes                    | No                               |
| `Reveal`   | Yes                    | Only on explicit request         |
| `Expose`   | Yes                    | Yes (default forwarding)         |
| `Forward`  | No                     | Yes (pass-through only)          |

Declare dependencies:
```java
dependency(Expose, project(Api.class));          // Project dependency
dependency(Consume, new MvnRepoLookup()          // Maven compile-only dep
    .resolve("org.slf4j:slf4j-api:2.0.17"));
dependency(Forward, project(Bnd.class));         // Pass to dependents only
```

### Maven Dependencies

Use `MvnRepoLookup` to resolve dependencies from Maven repositories:

```java
new MvnRepoLookup()
    .resolve("com.google.guava:guava:33.0.0-jre")
    .resolve("org.slf4j:slf4j-api:2.0.17");
```

Supports BOMs, transitive resolution, and source/javadoc downloads. Set
`LookupRepositories` property on the root project to configure repositories.

### Properties

Set typed properties on projects. Properties are inherited from parent to
child. Core properties:
- `Version` — project version (default: `"0.0.0"`)
- `BuildDirectory` — build output directory (default: `"build"`)
- `Encoding` — file encoding (default: `"UTF-8"`)

Maven properties (`MvnProperties`): `GroupId`, `ArtifactId`, etc.

```java
project.set(GroupId, "org.example");
project.set(ArtifactId, "my-app");
project.set(Version, "1.0.0");
```

## Built-in Extensions

| Extension  | Capabilities                                       |
|------------|----------------------------------------------------|
| `java`     | `JavaCompiler`, `JavaResourceCollector`,           |
|            | `JarBuilder`, `UberJarBuilder`, `Javadoc`,         |
|            | `JavaExecutor`                                     |
| `junit`    | `JUnitTestRunner` — runs JUnit 5 tests             |
| `mvnrepo`  | `MvnRepoLookup`, `MvnPublisher`, `PomFileGenerator`,|
|            | `SourcesJarBuilder`, `JavadocJarBuilder`           |
| `bnd`      | `BndAnalyzer` — OSGi manifest computation          |
| `git`      | `VersionTagger` — Git version tags                 |
| `nodejs`   | `NpmExecutor`, `NodeJsDownloader`                  |
| `eclipse`  | `EclipseConfigurator` — generates `.project`,      |
|            | `.classpath`                                       |
| `vscode`   | `VscodeConfigurator` — generates `tasks.json`,     |
|            | `launch.json`                                      |

External extensions are loaded via `buildExtensions` or
`runtimeExtensions` in `.jdbld.properties`.

## Common Patterns

### Defining a Build Command

```java
commandAlias("build")
    .description("Build all jars")
    .resources(of(LibraryJarFileType).using(Supply, Forward));
```

The command triggers the resource requests, which causes the build engine to
execute the necessary generators.

### Multi-Project Setup with `prepareProject`

Override `prepareProject(Project)` in the root to apply common configuration:

```java
@Override
public void prepareProject(Project project) {
    if (project instanceof JavaProject) {
        project.generator(JavaCompiler::new)
            .addSources(Path.of("src"), "**/*.java")
            .options("--release", "25");
        project.generator(JavaResourceCollector::new)
            .add(Path.of("resources"), "**/*");
    }
}
```

### Publishing to Maven

```java
generator(MvnPublisher::new)
    .destinations(get(PublishingDestinations));
```

Then use commands:
```java
commandAlias("mavenPublication")
    .projects("**")
    .resources(of(MvnPublicationType).using(Supply));
commandAlias("mavenInstallation")
    .projects("**")
    .resources(of(MvnInstallationType).using(Supply));
```

## How the Build Engine Works (Briefly)

1. The bootstrap phase compiles `_jdbld/src/` Java sources.
2. A new classloader loads the compiled config plus the builder classes.
3. The root project is discovered by scanning for `RootProject` implementations.
4. Commands map to resource requests. When a command runs, the engine
   evaluates resource requests, which triggers providers (generators) to
   produce the requested resources.
5. Providers are cached per `(provider, request)` pair. A `Cleanliness`
   request purges the cache.
6. Circular dependency detection is built in.

## Tips for Agents

- **Always read `_jdbld/src/jdbld/Root.java`** to understand the project
  structure and available commands.
- **Read `.jdbld.properties`** to know the jdbld version, extensions, and
  repository configuration.
- **Commands are project-defined** — there are no fixed "build" or "test"
  commands. Use `./jdbld -h` to discover what's available.
- **The `clean` command is built-in** and works on all projects.
- **Sub-projects are defined as Java classes** — each class extending
  `AbstractProject` is a sub-project. The directory structure mirrors the
  class names by convention.
- **`prepareProject()` in the root** is where common generator setup lives.
  Look there to understand what generators run on each project.
- **Intents matter** — `Expose` vs `Consume` vs `Forward` determines
  dependency visibility. When adding a dependency, choose the right intent.
- **Merged test projects** implement `MergedTestProject` and compile from
  `test/`. They automatically get the parent's classpath.
