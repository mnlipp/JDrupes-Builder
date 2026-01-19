---
title: "Test projects"
description: >-
  How to define test projects with JDrupes Builder.
layout: jdbld
---

# Test Projects

## JDrupes Builder's perspective

JDrupes Builder itself does not require any special configuration for
test projects. From its perspective, test projects are projects that
have a dependency on another project (the project under test), add a
[Generator](javadoc/org/jdrupes/builder/api/Generator.html) that provides
the code for the tests, and another generator that provides 
[TestResult](javadoc/org/jdrupes/builder/api/TestResult.html)s.
The test results are the only resource supplied by the test project, all
other resources (test classes etc.) are consumed only.

However, it has become a common practice to combine the sources for
the delivered artifacts and the test code into a single project. This
leads to special (and sometimes complicated) definitions for the test
aspects of a project. The JDrupes Builder cannot free itself fully from
this practice, but it tries to introduce as little special handling
as possible.

## Need for special handling

The need for special handling mainly originates from generators for
IDE configurations, as generating a common configuration for a project
under test and the associated test project without marking the test
project as such is almost impossible. However, if marking
a project as a test project causes it to behave in a special way
when opened in an IDE, the build tool should provide a minimum of
automatic special handling as well for consistency.

## What JDrupes Builder's core supports

Test projects can be marked as
[MergedTestProject](javadoc/org/jdrupes/builder/api/MergedTestProject.html)s.
As the name suggests, such a project is conceptually merged with the project
under test. The project under test must be the test project's
[parent project](javadoc/org/jdrupes/builder/api/Project.html#parentProject())
and both projects share the same directory, i.e. the test project must not
specify a directory of its own.

The test project specifies generators for artifacts just like any other
project. As mentioned in the first section, test projects usually do not
supply (or expose) any resources. Therefore adding a
[Generator](javadoc/org/jdrupes/builder/api/Generator.html) to a project with
[generator](javadoc/org/jdrupes/builder/api/Project.html#generator(org.jdrupes.builder.api.Generator))
adds a dependency with intend
[Consume](javadoc/org/jdrupes/builder/api/Intend.html#Consume)
(instead of `Supply` as it is the case for regular projects).

This special handling of `generate` corresponds to the behavior of IDEs,
where artifacts from test sources are not made available to projects that
depend on the project containing the test sources. This prevents test
classes from being unintentionally provided by the test project. The
drawback is that the provider for the
[TestResult](javadoc/org/jdrupes/builder/api/TestResult.html)s cannot be
added with method `generator`, but instead must be added as a dependency with
intend [Supply](javadoc/org/jdrupes/builder/api/Intend.html#Supply).

A typical configuration for the Java compiler that takes merged test
projects into account might look like this:

```java
  if (project instanceof JavaProject) {
      if (!(project instanceof MergedTestProject)) {
          // "Normal" project
          project.generator(JavaCompiler::new)
              .addSources(Path.of("src"), "**/*.java");
          project.generator(JavaResourceCollector::new)
              .add(Path.of("resources"), "**/*");
      } else {
          // Test project
          project.generator(JavaCompiler::new).addSources(Path.of("test"),
              "**/*.java");
          project.generator(JavaResourceCollector::new).add(Path.of(
              "test-resources"), "**/*");
          project.dependency(Consume, new MvnRepoLookup()
              .bom("org.junit:junit-bom:5.14.2")
              .resolve("org.junit.jupiter:junit-jupiter-api")
              .resolve("org.junit.jupiter:junit-jupiter-engine"));
          project.dependency(Supply, JUnitTestRunner::new);
      }
  }
```

## Support from providers

Marking a project as a merged test project has the greatest impact on the
[EclipseConfigurator](javadoc/org/jdrupes/builder/eclipse/EclipseConfigurator.html).
See the documentation of this generator for details.

The [JavaCompiler](javadoc/org/jdrupes/builder/java/JavaCompiler.html)
changes the default destination directory for the compiled classes if
the project is marked as a merged test project.

The default class path used by the
[JUnitTestRunner](javadoc/org/jdrupes/builder/junit/JUnitTestRunner.html) is
also affected by the test project being marked as a merged test project.
The test runner automatically includes resources used by the project under
test as if they were resources of the test project.
