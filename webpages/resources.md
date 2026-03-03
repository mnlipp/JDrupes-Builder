---
title: "Adding resource types"
description: >-
  Explains how to define and instantiate new resource types.
layout: jdbld
---

# Adding Resource types

While the JDrupes Builder provides a set of standard resource types, it is
sometimes useful to define additional resources in build projects to model
specific build artifacts or processes.

## Defining new resource types

Defining a new resource type is straightforward if it extends an existing
resource interface and does not need additional methods. For example,
if you want to define a new type of file resource, you can extend the
[FileResource](javadoc/org/jdrupes/builder/api/FileResource.html) interface:

```java
public interface AudioFile extends FileResource {

    static AudioFile from(Path path) {
        return ResourceFactory.create(new ResourceType<>() {}, path);
    }
}
```

By extending `FileResource`, your new resource type inherits all its methods,
such as `path()` and `cleanup()`.

## Instantiating resources with ResourceFactory

As shown in the example above, new resource instances are typically created
using the
[ResourceFactory](javadoc/org/jdrupes/builder/api/ResourceFactory.html).
The `ResourceFactory.create` method takes a
[ResourceType](javadoc/org/jdrupes/builder/api/ResourceType.html) and any
additional arguments required by the resource's constructor.

The implementation of `ResourceFactory` uses a Java
[Proxy](https://docs.oracle.com/en/java/javase/25/docs/api//java.base/java/lang/reflect/Proxy.html)
to provide the implementation of the interface. This means you don't need
to write a concrete class that implements your new interface; the builder
handles it dynamically as long as the interface does not define additional
methods (`static` and `default` methods can be added).

### Convention for the "from" method

Following the style of existing resource interfaces in the JDrupes Builder
API, it is recommended to provide a static `from` method in your interface.
This method serves as a convenient factory for creating instances of the
resource and encapsulates the call to `ResourceFactory.create`.

```java
    static AudioFile from(Path path) {
        return ResourceFactory.create(new ResourceType<>() {}, path);
    }
```

This makes the usage of your new resource type consistent with the rest of
the API:

```java
AudioFile audio = AudioFile.from(project.buildDirectory()
  .resolve("background.mp3"));
```

## Adding methods to resource types

If your new resource type needs to provide additional methods that are not
part of the base interface, the dynamic proxy mechanism is no longer
sufficient. In this case, you must provide a custom implementation of the
resource and a corresponding `ResourceFactory` to instantiate it.

### Define the resource interface

First, define your interface with the additional methods:

```java
public interface ExtendedResource extends Resource {
    String extraInfo();
}
```

### Provide an implementation

Next, create a class that implements your interface. It is recommended to
extend `ResourceObject` (from the `core` package) to inherit standard
resource behavior:

```java
public class DefaultExtendedResource extends ResourceObject 
        implements ExtendedResource {
    
    private final String info;

    public DefaultExtendedResource(ResourceType<? extends ExtendedResource> type,
            String info) {
        super(type);
        this.info = info;
    }

    @Override
    public String extraInfo() {
        return info;
    }
}
```

### Implement ResourceFactory

Create a factory class that knows how to instantiate your resource. You can
use `CoreResourceFactory.createNarrowed` to handle the proxying if your
factory should also support the proxy mechanism to create specialized
versions of your new resource type:

```java
public class MyResourceFactory implements ResourceFactory {
    @Override
    public <T extends Resource> Optional<T> newResource(ResourceType<T> type,
            Project project, Object... args) {
        return CoreResourceFactory.createNarrowed(type, ExtendedResource.class,
            () -> new DefaultExtendedResource(
                (ResourceType<? extends ExtendedResource>) type,
                (String) args[0]));
    }
}
```

### Register the factory as a service

Finally, register your factory as a service by creating a file named
`META-INF/services/org.jdrupes.builder.api.ResourceFactory` in your
resources directory, containing the fully qualified name of your factory class:

```text
com.myproject.MyResourceFactory
```

You can find examples for `ResourceFactory` implementions in the jdbld
sub-projects for supporting 
[Java](javadoc/org/jdrupes/builder/java/JavaResourceFactory.html) specific
resource types of for handling
[Maven repository](javadoc/org/jdrupes/builder/mvnrepo/MvnRepoResourceFactory.html)
related types.
