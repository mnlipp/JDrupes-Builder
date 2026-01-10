---
title: "Requesting Resources"
description: >-
  Describes a consistent way to define queries for resource types.
layout: jdbld
---

# Requesting Resources

## Resource providers

Resources are never directly requested from a
[ResourceProvider](javadoc/org/jdrupes/builder/api/ResourceProvider.html).
Instead, requests must be passed through the 
[BuildContext](javadoc/org/jdrupes/builder/api/BuildContext.html)'s
[get](javadoc/org/jdrupes/builder/api/BuildContext.html#get(org.jdrupes.builder.api.ResourceProvider,org.jdrupes.builder.api.ResourceRequest))
method. This allows the `BuildContext` to synchronize the requests and 
to cache the results.

Resources are returned as a lazily evaluated Java `Stream` of resources of
the requested type. This delays the evaluation of the request until the
`Stream` is terminated. The `Stream`s must never be terminated in the
constructor of a [Project](javadoc/org/jdrupes/builder/api/Project.html)
as the evaluation may require resources from other projects that are not
available yet.

There are two methods that simplify requesting resources in the project
configuration. The first is to use the
[Project](javadoc/org/jdrupes/builder/api/Project.html)'s
[from](javadoc/org/jdrupes/builder/api/Project.html#from(org.jdrupes.builder.api.ResourceProvider))
method. This allows you to write `project.from(provider).get(resourceType)`
instead of `project.context().get(provider, resourceType)`. The second method
is the [Project](javadoc/org/jdrupes/builder/api/Project.html)'s
[get](javadoc/org/jdrupes/builder/api/Project.html#get(org.jdrupes.builder.api.ResourceRequest))
method that allows you to easily get resources provided by the
[Project](javadoc/org/jdrupes/builder/api/Project.html) itself.

## Resource requests

Resources are usually requested by type rather than by name.
Therefore we need a consistent way to define resource types for
queries. Let's use the Java classpath as an example. A Java classpath
consists of elements. These elements can be jar files or trees
of class files (which are denoted in the classpath by their root
directory).

Using [ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html)
as common supertype for jar files and class trees, we could ask for 
resource type
[ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html).
However, we cannot expect to receive a single classpath element.
Instead, we should expect to get a collection of classpath elements.
Therefore it makes more sense to ask for a collection of classpath
elements, which results in the requested type being
[Resources](javadoc/org/jdrupes/builder/api/Resources.html)&lt;[ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html)&gt;.

![Java base types](javadoc/java-base-types.svg)

What we expect from a provider when requesting the
[ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html)s
are resources required for a build that uses the provider as dependency.
These classes are commonly referred to as the "compile classpath".

For executing Java code (e.g. when running unit tests) we additionally
need the classes that were used during the build, but are not provided
by the project because they are not part of the provided artifacts'
API. These classes (together with the classes from the compile classpath)
are commonly referred to as the "runtime classpath".

The runtime classpath is unusual because it is not a build result. It's
a collection of resources that is generated for a specific purpose.
Obtaining *all* classpath elements requires a project (in its role
as provider) to change its behavior and to also return the results
from providers with intend `Consume`. Such a change of behavior can be
controlled by using a specialized container type, which indicates the
desired change. In the case of a
[Project](javadoc/org/jdrupes/builder/api/Project.html)
the behavior changes if the container implements
[AllResources](javadoc/org/jdrupes/builder/api/AllResources.html).

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
        commandAlias("build", requestFor(AppJarFile.class));
```
 
Note that the command argument "`clean`" is predefined and can always
be used to request
"[Cleanliness](javadoc/org/jdrupes/builder/api/Cleanliness.html)".
