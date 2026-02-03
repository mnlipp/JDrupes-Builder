---
title: "Requesting Resources"
description: >-
  Describes a consistent way to define queries for resource types.
layout: jdbld
---

# Requesting Resources

## Resource requests

Resources are obtained by sending a
[ResourceRequest](javadoc/org/jdrupes/builder/api/ResourceRequest.html)
to a provider. A request usually specifies a resource *type* rather than
a concrete name. This requires a consistent way to define resource types
for queries.

Consider the Java classpath as an example. A Java classpath consists of
elements that may either be JAR files or directory trees containing
class files (denoted on the classpath by their root directory).

By using
[ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html)
as common supertype for JAR files and class trees, we can ask for 
resource type
[ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html).
Requests for this type are handled by providers for JAR files
as well as by providers for class trees.

![Java base types](javadoc/java-base-types.svg)

A commonly available resource that all generators must support is
"[Cleanliness](javadoc/org/jdrupes/builder/api/Cleanliness.html)".
Requesting this resource type from a generator makes it remove all
resources that it has created.

Admittedly, this pushes the concept of "everything being a resource"
to its limits. It solves the problem of cleaning up after a build.
Depending on one's point of view, "cleanliness" may represent the absence
of something, but it can also be regarded as a resource that can be
provided.

## Resource providers

Resources are provided by
[ResourceProvider](javadoc/org/jdrupes/builder/api/ResourceProvider.html)s via
their [SPI](javadoc/org/jdrupes/builder/api/ResourceProviderSpi.html).
The SPI's
[provide](javadoc/org/jdrupes/builder/api/ResourceProviderSpi.html#provide(org.jdrupes.builder.api.ResourceRequest))
method must never be invoked directly. Instead, requests must be passed through
the [BuildContext](javadoc/org/jdrupes/builder/api/BuildContext.html)'s
[resources](javadoc/org/jdrupes/builder/api/BuildContext.html#resources(org.jdrupes.builder.api.ResourceProvider,org.jdrupes.builder.api.ResourceRequest))
method. This allows the `BuildContext` to synchronize requests and 
cache results. To simplify the invocation,
[ResourceProvider](javadoc/org/jdrupes/builder/api/ResourceProvider.html)
defines a method 
[resources](javadoc/org/jdrupes/builder/api/ResourceProvider.html#resources(org.jdrupes.builder.api.ResourceRequest))
that performs the invocation via the build context and makes this mechanism
transparent to the user.

The `BuildContext` invokes the
[provide](javadoc/org/jdrupes/builder/api/ResourceProviderSpi.html#provide(org.jdrupes.builder.api.ResourceRequest))
method asynchronously and wraps the resulting `Future` in a `Stream` that
calls the `Future`'s `get` method only when this "wrapper `Stream`" is
terminated. This makes the invocation of `BuildContext.resources` non-blocking.
The "wrapper `Stream`s" can be freely passed around in a
[Project](javadoc/org/jdrupes/builder/api/Project.html)'s constructor,
but they must never be terminated in this context. Effectively, the
first termination of a such a "wrapper `Stream`" ends the configuration
phase and starts the build phase.

## Projects as resource providers

The resources provided by a
[Project](javadoc/org/jdrupes/builder/api/Project.html) do not only
depend on the type of resource requested, but also on the 
providers that the project selects when forwarding the request. This
is controlled by the intents associated with the request via
[using](javadoc/org/jdrupes/builder/api/ResourceRequest.html#using(java.util.Set)).

Certain combinations of requested type and intents map directly to
concepts commonly used in other build tools. For example, a request
for classpath elements using `Supply` and `Expose` yields the classes
produced by the project together with the resources used to create them.
When a project serves as a build dependency, this result constitutes
the project's API.

More details on combining intents with resource requests can be found
in the class documentation of
[Project](javadoc/org/jdrupes/builder/api/Project.html).

## Requesting resources from the command line

Specifying resource types as Java types works well within a build
configuration. On the command line, however, this approach would be
rather cumbersome.

For this reason, the root project's constructor can define
aliases for resources that may be used as command arguments
when invoking the JDrupes Builder. This is illustrated by the following sample project
code:
```java
        // Command arguments
        commandAlias("build", of(AppJarFile.class).usingAll());
```
 
Note that the command argument "`clean`" is predefined and can always
be used to request
"[Cleanliness](javadoc/org/jdrupes/builder/api/Cleanliness.html)".
