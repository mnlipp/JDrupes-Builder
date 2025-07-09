---
title: "Requesting Resources"
description: >-
  Describes a consistent way to define resource types for queries.
layout: jdbld
---

# Requesting Resources

Resources are usually requested by type rather than by name.
Therefore we need a consistent way to define resource types for
queries. Let's use the Java class path as an example. A Java class
path consists of elements. These elements can be jar files or trees
of class files (which are denoted in the class path by their root
directory).

Using [ClasspathElement](javadoc/org/jdrupes/builder/api/ClasspathElement)
as common supertype for jar files and class trees, we could ask for the
resource type
[ClasspathElement](javadoc/org/jdrupes/builder/api/ClasspathElement).
But actually, we cannot expect to get a single classpath element back.
Instead, we should expect to get a collection of classpath elements. So
it makes more sense to ask for a collection of classpath elements, which
makes the requested type `Resources<ClasspathElement>`.

![Java base types](javadoc/java-base-types.svg)

There is one more point to consider. There can be different kinds of
classpaths such as a compile time and a runtime classpath. Both are 
collections of
[ClasspathElement](javadoc/org/jdrupes/builder/api/ClasspathElement)s.
However, depending on the kind of classpath, a
[ResourceProvider](javadoc/org/jdrupes/builder/api/) may deliver different
subsets of classpath elements. We can include this information in the
resource type by using a specialized container whose type indicates
the desired subset of instances. For Java classpaths, the specialized
container types are
[CompilationResources](javadoc/org/jdrupes/builder/api/CompilationResources)
and [RuntimeResources](javadoc/org/jdrupes/builder/api/RuntimeResources).

From this example, we derive the common pattern for resource requests.

 1. The requested type is always a collection of resources, i.e.
    [Resources](javadoc/org/jdrupes/builder/api/Resources) or a type
    derived from it.
 2. The type of the elements in the container is the type of the
    resource instances returned as Java `Stream` by the 
    [ResourceProvider](javadoc/org/jdrupes/builder/api/ResourceProvider)'s
    [provide](javadoc/org/jdrupes/builder/api/ResourceProvider#provide)
    method.
 3. The type of the container may be used by providers to select
    the instances they want to provide.

The available resource types and the effect that the type of the
container has on the provided resources can be found in the
documentation of the respective providers.

A commonly available resource that must be supported by all providers
that generate resources is 
"[Cleanliness](javadoc/org/jdrupes/builder/api/Cleanliness)".
Admittedly, this is pushing the concept of everything being a resource
to its limits. It solves the problem of cleaning up after a build.
Depending on your point of view, "cleanliness" may be the absence of
something, but you could also argue that "cleanliness" is something
that can be provided.

## Requesting resources from the command line

Specifying resource types as Java types works fine within the build
configuration. Specifying them in this way on the command line when
invoking the JDrupes builder would be rather tedious.

The root project's constructor can therefore be used to define
aliases for the resources that can be used as arguments when invoking
the builder. This is shown in the sample project:

```java
        // Commands
        defineCommand("build",
            new ResourceRequest<AppJarFile>(new ResourceType<>() {}));
```
 
Note that the alias "`clean`" is predefined and can always be used to
request "[Cleanliness](javadoc/org/jdrupes/builder/api/Cleanliness)".
