---
title: "Git based versioning"
description: >-
  Describes how JDrupes Builder can derive project versions from tags in a Git
  repository and create new tags for increments.
layout: jdbld
---

# Git based versioning

JDrupes Builder supports deriving a project's version from Git tags and
creating new release tags. Deriving the versions from tags is typically
based on the
[JDrupes Git Versioning](https://mnlipp.github.io/jdrupes-gitversioning/index.html)
library. Creating new release tags is supported by the
[VersionTagger](javadoc/org/jdrupes/builder/ext/git/VersionTagger.html)
from the
[jdbld-ext-git](https://codeberg.org/JDrupes/-/packages/maven/org.jdrupes:jdbld-ext-git)
extension.

## Simple configuration

Starting from a basic project, adding Git based versioning requires three
things: the extension dependency, a Git API instance, and the versioning
generators.

```java
import org.eclipse.jgit.api.Git;
import static org.jdrupes.builder.ext.git.GitProperties.*;
import org.jdrupes.builder.ext.git.VersionTagger;
import org.jdrupes.builder.core.VersionReporter;
import org.jdrupes.gitversioning.api.VersionEvaluator;
import org.jdrupes.gitversioning.core.DefaultTagFilter;

public class SimpleApp extends AbstractRootProject {

    public SimpleApp() {
        super(name("demo-project-simple-app"));

        // Git based versioning
        set(GitApi, VersionTagger.setGitApi(this));
        var evaluator = VersionEvaluator.forRepository(get(GitApi).getRepository())
            .subDirectory(directory())
            .tagFilter(new DefaultTagFilter().prepend("v"));
        set(Version, evaluator.version());
        generator(VersionReporter::new);
        generator(VersionTagger::new).prefixEvalutor(_ -> "v");

        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        generator(UberJarBuilder::new).addFrom(providers()
            .select(Supply)).mainClass("jdbld.demo.simpleapp.App");

        commandAlias("build")
            .resources(of(new ResourceType<AppJarFile>() {}));
        commandAlias("version")
            .resources(of(ProjectVersionType).using(Supply));
        commandAlias("releaseTag")
            .resources(of(GitVersionTagType).using(Supply));
    }
}
```

The following sections explain each part of this configuration.

### Extension dependency

The Git versioning support lives in an extension that must be listed in
your `.jdbld.properties`:

```properties
buildExtensions = org.jdrupes:jdbld-ext-git:1.0.0
```

This makes the classes from `org.jdrupes.builder.ext.git` available to your
build configuration. The latest version of the plugin can be found on
[Codeberg](https://codeberg.org/JDrupes/-/packages/maven/org.jdrupes:jdbld-ext-git).

### Git API instance

A [Git](https://javadoc.io/versions/org.eclipse.jgit/org.eclipse.jgit)
instance is needed to access the repository. The helper method
[VersionTagger.setGitApi](javadoc/org/jdrupes/builder/ext/git/VersionTagger.html)
opens the repository from the project directory and stores the instance under
the
[GitApi property](javadoc/org/jdrupes/builder/ext/git/GitProperties.html#GitApi)
on the root project:

```java
set(GitApi, VersionTagger.setGitApi(this));
```

This is a no-op if the property is already set. In a multi-project build,
call this only once for the root project, as shown in the
[common configuration](multi-project.html) pattern.

### Version from tags

The project version is derived from Git tags using the 
[JDrupes Git Versioning](https://mnlipp.github.io/jdrupes-gitversioning/index.html)
library, in particular its 
[VersionEvaluator](https://mnlipp.github.io/jdrupes-gitversioning/javadoc/org/jdrupes/gitversioning/api/VersionEvaluator.html).
The evaluator inspects the tag history and computes a semantic version based
on the most recent tag and the current commit state:

```java
var evaluator = VersionEvaluator.forRepository(get(GitApi).getRepository())
    .subDirectory(directory())
    .tagFilter(new DefaultTagFilter().prepend("v"));
set(Version, evaluator.version());
```

The `subDirectory` call is needed when the project lives in a subdirectory
of the Git repository root. The `tagFilter` restricts which tags are
considered. Here, `DefaultTagFilter().prepend("v")` matches tags that start
with `v` followed by a semver string, such as `v1.2.3`. The prefix is
stripped when computing the version. Details on how the version is
computed from tags are documented in the `VersionEvaluator` javadoc.

Add the
[VersionReporter](javadoc/org/jdrupes/builder/core/VersionReporter.html)
to the build as shown above. This makes it possible to query the evaluated
project version from the command line.

### Incrementing versions

Using Git based versioning, incrementing the version means creating a new
tag. This is supported by
he [VersionTagger](javadoc/org/jdrupes/builder/ext/git/VersionTagger.html)
which provides resoures of type
[GitVersionTag](javadoc/org/jdrupes/builder/ext/git/GitVersionTag.html).

```java
generator(VersionTagger::new).prefixEvalutor(_ -> "v");
```

Its behavior is controlled by the `jdbld.versionTagger.mode` property, which
can be set via the `-P` flag. See the
[VersionTagger javadoc](javadoc/org/jdrupes/builder/ext/git/VersionTagger.html)
for the available modes and their effect on version incrementing.

The `prefixEvalutor` configures the tag prefix. The tag is formed by
concatenating the prefix with the computed version.

### Release tag command

To create a release tag conveniently, define a command alias:

```java
commandAlias("releaseTag")
    .resources(of(GitVersionTagType).using(Supply));
```

Then invoke it with a version increment mode, for example:

```bash
./jdbld -Pjdbld.versionTagger.mode=nextPatch releaseTag
```

The command will create an annotated tag with the incremented version. See the
[VersionTagger javadoc](javadoc/org/jdrupes/builder/ext/git/VersionTagger.html)
for the available increment modes.

## Multi-project setup

In a multi-project build, the versioning setup is typically placed in the
root project's [prepareProject](javadoc/org/jdrupes/builder/api/RootProject.html#prepareProject(org.jdrupes.builder.api.Project))
method:

```java
@Override
public void prepareProject(Project project) {
    if (project instanceof RootProject rootPrj) {
        project.set(GitApi, VersionTagger.setGitApi(rootPrj));
    }
    if (project instanceof RootProject || project instanceof VersionedProject) {
        var prefix = project instanceof RootProject ? ""
            : project.name() + "-";
        var evaluator = VersionEvaluator
            .forRepository(project.<Git> get(GitApi).getRepository())
            .subDirectory(project.directory())
            .tagFilter(new DefaultTagFilter().prepend(prefix));
        project.set(Version, evaluator.version());
        project.generator(VersionReporter::new);
        project.generator(VersionTagger::new).prefixEvalutor(_ -> prefix);
    }
}
```

Each versioned project gets its own tag prefix, so tags like
`module-a-1.0.0` and `module-b-2.1.0` can coexist in the same repository.
Depending on your project layout, a marker interface `VersionedProject` or some
other project property can be used to distinguishes projects that should be
versioned from those that should not.
