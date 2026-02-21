---
title: "JDrupes Builder invocation"
description: >-
  How to invoke JDrupes Builder and the available command line and
  configuration options.
layout: jdbld
---

# Invocation

## `jdbld` executable

To get started with JDrupes Builder, you only need the `jdbld` script.
It can be downloaded from [GitHub](https://github.com/mnlipp/jdbld/) using:

```bash
curl -L https://raw.githubusercontent.com/mnlipp/JDrupes-Builder/refs/heads/main/jdbld -o jdbld
chmod +x jdbld
```

The script can be placed anywhere on your system, but it is typically
copied into the root directory of your project. Regardless of its
location, it must always be invoked from the project's root directory.
The script checks for the presence of a `.jdbld.properties`
file in the current directory. This file serves as a marker indicating
that the current directory is the root directory of a JDrupes
Builder-managed project. It also allows you to configure some
options for running the JDrupes Builder.

`jdbldDirectory = <name>`
: sets the name of the directory that
contains the sources of the builder project. The default value is
`_jdbld`. The builder project sources consist of all files matching
the pattern "`**/<jdbldDirectory>/src/**/*.java`".

`jdbldVersion = <version>`
: sets the version of the JDrupes Builder to use.

`buildExtensions = ...`
: is a comma-separated list of Maven coordinates of libraries that are
added to the classpath when compiling the builder project.
    
`extensionsSnapshotRepository = <url>`
: sets the snapshot repository to use if one of the Maven coordinates
in `buildExtensions` refers to a snapshot.
    
`javaHome = <directory>`
: sets the directory that contains the JDK to use. Note that the
environment variable `JAVA_HOME` takes precedence. 

## `jdbld` arguments

The `jdbld` command accepts the following options:

  * `-B-x <glob pattern>` excludes matching directories from the
    search for source files for the builder project. This option
    is only required for the development of the JDrupes Builder
    itself, where sample build projects for tests are located
    in subdirectories of the builder's source tree.
    
  * `-P <property name>=<property value>` sets the value of a
    property. Properties can be accessed within the build project via 
    [`BuildContext.property(...)`](javadoc/org/jdrupes/builder/api/BuildContext.html#property(java.lang.String,java.lang.String)).
    