---
title: "Requesting Resources"
description: >-
  Describes a consistent way to define resource types for queries.
layout: jdbld
---

# Requesting Resources

Resources are usually requested by type rather than by name.
Therefore we need a consistent way to define resource types for
queries. Let's use the Java classpath as an example. A Java classpath
consists of elements. These elements can be jar files or trees
of class files (which are denoted in the classpath by their root
directory).

Using [ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html)
as common supertype for jar files and class trees, we could ask for the
resource type
[ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html).
However, we cannot expect to receive a single classpath element.
Instead, we should expect to get a collection of classpath elements.
Therefore it makes more sense to ask for a collection of classpath
elements, which results in the requested type being
`Resources<ClasspathElement>`.

![Java base types](javadoc/java-base-types.svg)

Another point to consider is that there are different kinds of
classpaths such as compile-time and a runtime classpaths. Both are 
collections of
[ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html)s.
However, depending on the kind of classpath, a
[ResourceProvider](javadoc/org/jdrupes/builder/api/ResourceProvider.html)
may deliver different subsets of these elements. We can include this
information in the requested resource type by using a specialized
container whose type indicates the desired subset of instances. For Java
classpaths, the specialized container types are
[CompilationResources](javadoc/org/jdrupes/builder/java/CompilationResources.html)
and [RuntimeResources](javadoc/org/jdrupes/builder/java/RuntimeResources.html).

From this example, we derive the common pattern for resource requests.

 1. The requested type is always a collection of resources, i.e.
    [Resources](javadoc/org/jdrupes/builder/api/Resources.html) or a type
    derived from it.
 2. The type contained in
    [Resources](javadoc/org/jdrupes/builder/api/Resources.html) (its type
    paramter) must match the type parameter of
    [ResourceRequest](javadoc/org/jdrupes/builder/api/ResourceRequest.html)
    and is the type of the elements of the Java `Stream` that is returned
    by the
    [ResourceProvider](javadoc/org/jdrupes/builder/api/ResourceProvider.html)'s
    [provide](javadoc/org/jdrupes/builder/api/ResourceProvider.html#provide)
    when handling the request.
 3. The type of the collection of resources may be used by providers
    to select the instances they want to provide.

The available resource types and the effect that the type of the
container has on the provided resources can be found in the
documentation of the respective providers.

A commonly available resource that must be supported by all providers
that generate resources is 
"[Cleanliness](javadoc/org/jdrupes/builder/api/Cleanliness.html)".
Admittedly, this pushes the concept of everything being a resource
to its limits. It solves the problem of cleaning up after a build.
Depending on your point of view, "cleanliness" may be the absence of
something, but you could also argue that "cleanliness" is something
that can be provided.

## Requesting resources from the command line

Specifying resource types as Java types works well within the build
configuration. However, specifying them in this way on the command
line when invoking the JDrupes builder would be rather tedious.

The root project's constructor can therefore be used to define
aliases for the resources that can be used as command arguments
when invoking the builder. This is shown in the sample project:

```java
        // Command arguments
        commandAlias("build",
            new ResourceRequest<AppJarFile>(new ResourceType<>() {}));
```
 
Note that the command argument "`clean`" is predefined and can always
be used to request
"[Cleanliness](javadoc/org/jdrupes/builder/api/Cleanliness.html)".
