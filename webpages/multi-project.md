---
title: "Multi-project builds"
description: >-
  How to specify a multi-project build configuration in JDrupes Builder.
layout: jdbld
---

# Multi-project builds

## Overview

JDrupes Builder supports multi-project builds. A multi-project build
configuration manages a collection of projects that each provide resources,
such as a Java library. In a typical layout, the projects are located in
subdirectories of the project's root directory. The root directory corresponds
to a root project in the build configuration, that is, a class implementing
[RootProject](javadoc/org/jdrupes/builder/api/RootProject.html). 

The root project provides resources by requesting them from its subprojects,
represented in the build configuration by classes that implement
[Project](javadoc/org/jdrupes/builder/api/Project.html). The root
project can also provide resources itself, typically something like a
uber-jar or the combined javadoc for all subprojects.

Examples of multi-project builds can be found in the GitHub repository as
[demo-project-subprojects](https://github.com/mnlipp/JDrupes-Builder/tree/main/test-projects/demo-project-subprojects)
and
[demo-project-library](https://github.com/mnlipp/JDrupes-Builder/tree/main/test-projects/demo-project-library).

## Dependencies

Projects in a multi-project build configuration can depend on each other.
Project `demo-project-library` shows this for an API and its implementation.

```java
public class Api extends AbstractProject implements JavaLibraryProject {

    public Api() {
        super(name("api"));
    }
}
```

```java
public class Impl extends AbstractProject implements JavaLibraryProject {

    public Impl() {
        super(name("impl"));
        dependency(Expose, project(Api.class));
    }
}
```

The dependency between the implementation and the API project is established
using the `Expose` intent. The effect of the chosen intent on the handling of
resource requests is described in detail in the documentation for
[Project](javadoc/org/jdrupes/builder/api/Project.html#behavior-as-resource-provider-heading).

## Common configuration

Neither `Api` nor `Impl` as shown above define any generators for 
providing classes (or libraries). Doing this for each project individually
in a multi-project build configuration would be tedious and error-prone.

Common configuration for projects can be defined in the root project using
[RootProject.prepareProjects](javadoc/org/jdrupes/builder/api/RootProject.html#prepareProject(org.jdrupes.builder.api.Project))).
This method is automatically executed for each project after the invocation
of `super` in the project's constructor.

How a builder project makes use of this mechanism is entirely up to the
project's author. One approach is to handle only configuration that applies to
*all* projects in the `prepareProject` method. Common configuration for
subsets of projects can be defined in a static method that is explicitly
invoked in these project's constructors.

Another option is to mark projects of a particular type by implementing
a marker interface and use this to control the behavior of `prepareProjects`.
Although this does not strictly follow classic object oriented design
principles, it is a convenient and pragmatic pattern in this context.

The latter approach is used by
[demo-project-library](https://github.com/mnlipp/JDrupes-Builder/tree/main/test-projects/demo-project-library).
Because there are a lot of details to configure, the `prepareProjects`
method invokes two static helper methods. 

```java
    @Override
    public void prepareProject(Project project) {
        setupCommonGenerators(project);
        setupEclipseConfigurator(project);
    }
```

The distinctions between the project types can be seen in
`setupCommonGenerators`:

```java
    private static void setupCommonGenerators(Project project) {
        if (project instanceof JavaProject) {
            if (project instanceof MergedTestProject) {
                // ...
            } else {
                // ...
            }
        }
        if (project instanceof JavaLibraryProject) {
            // ...
        }
    }
```

At first glance, the complete example on GitHub may appear complex.
However, it demonstrates a real-world setup that configures everything
required for building and testing artifacts, and for preparing them for
publication in a Maven repository.

Keep in mind that no special DSL knowledge is needed. Everything is
expressed in plain, easy-to-understand Java code.
