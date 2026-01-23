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
to a provider. The request usually specifies a type rather than a name.
Therefore we need a consistent way to define resource types for
queries. Let's use the Java classpath as an example. A Java classpath
consists of elements. These elements can be jar files or trees
of class files (which are denoted in the classpath by their root
directory).

Using [ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html)
as common supertype for jar files and class trees, we can ask for 
resource type
[ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html).
Requests for this type are handled by providers for jar files
as well as by providers for class trees.

![Java base types](javadoc/java-base-types.svg)

A commonly available resource that must be supported by all providers
that generate resources is 
"[Cleanliness](javadoc/org/jdrupes/builder/api/Cleanliness.html)".
Admittedly, this pushes the concept of everything being a resource
to its limits. It solves the problem of cleaning up after a build.
Depending on your point of view, "cleanliness" may be the absence of
something, but you could also argue that "cleanliness" is something
that can be provided.

## Resource providers

Resources may never be directly requested from a
[ResourceProvider](javadoc/org/jdrupes/builder/api/ResourceProvider.html).
Instead, requests must be passed through the 
[BuildContext](javadoc/org/jdrupes/builder/api/BuildContext.html)'s
[resources](javadoc/org/jdrupes/builder/api/BuildContext.html#resources(org.jdrupes.builder.api.ResourceProvider,org.jdrupes.builder.api.ResourceRequest))
method. This allows the `BuildContext` to synchronize the requests and 
to cache the results. To simplify the invocation,
[ResourceProvider](javadoc/org/jdrupes/builder/api/ResourceProvider.html)
has a method 
[resources](javadoc/org/jdrupes/builder/api/BuildContext.html#resources(org.jdrupes.builder.api.ResourceProvider,org.jdrupes.builder.api.ResourceRequest))
that implements the invocation via the build context.

Resources are returned as a lazily evaluated Java `Stream` of resources of
the requested type. This delays the evaluation of the request until the
`Stream` is terminated. The `Stream`s must never be terminated in the
constructor of a [Project](javadoc/org/jdrupes/builder/api/Project.html)
as the evaluation may require resources from other projects that are not
available yet.

## Projects as resource providers

The resources provided by a
[Project](javadoc/org/jdrupes/builder/api/Project.html) do not only
depend on the type of resource requested, but also on the 
providers that the project selects when forwarding the request. This
is controlled by the intents associated with the request through
[using](javadoc/org/jdrupes/builder/api/ResourceRequest.html#using(java.util.Set)).

Certain combinations of requested type and intents map directly to
common concepts in other build tools. Take as an example a request for
classpath elements in combination with using `Supply` and `Expose`.
The result of such a request includes the classes created by the project
together with the resources used to create them. For a project serving
as a build dependency this result constitutes the project's API.

More about using intents together with requests can be found in the class
documentation of [Project](javadoc/org/jdrupes/builder/api/Project.html).

## Requesting resources from the command line

Specifying resource types as Java types works well within the build
configuration. However, specifying them in this way on the command
line when invoking the JDrupes builder would be rather tedious.

The root project's constructor can therefore be used to define
aliases for the resources that can be used as command arguments
when invoking the builder. This is shown in the sample project:

```java
        // Command arguments
        commandAlias("build", requestFor(AppJarFile.class));
```
 
Note that the command argument "`clean`" is predefined and can always
be used to request
"[Cleanliness](javadoc/org/jdrupes/builder/api/Cleanliness.html)".
