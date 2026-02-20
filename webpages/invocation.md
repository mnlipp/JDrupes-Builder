---
title: "JDrupes Builder invocation"
description: >-
  How to invoke JDrupes Builder and the available command line and
  configuration options.
layout: jdbld
---

# Invocation

## `jdbld` executable

The only file that you need to get started with JDrupes Builder is the
`jdbld` script. You can download it from
[GitHub](https://github.com/mnlipp/jdbld/) with the following command:

```bash
curl -L https://raw.githubusercontent.com/mnlipp/JDrupes-Builder/refs/heads/main/jdbld -o jdbld
chmod +x jdbld
```

The script can be located anywhere on your computer, although it is
usually placed in the root directory of your project. No matter where
you place it, it must be invoked from the root directory of your
project. The script checks for the presence of a `.jdbld.properties`
file in the current directory. This file serves as a marker indicating
that the current directory is the root directory of a JDrupes Builder
managed project. But of course, it also allows you to configure some
options for running the JDrupes Builder.

  * "`jdbldDirectory = <name>`" sets the name of the directory that
     contains the sources of the JDrupes Builder project. The default
     value is `_jdrupes`. The sources of the JDrupes Builder project
     consist of all files matching the pattern
     `**/<jdbldDirectory>/src/**/*.java`.

  * "`jdbldVersion = <version>`" sets the version of the JDrupes Builder 
    to use.

  * "`buildExtensions = ...`" is a comma-separated list of maven
    coordinates of libraries to be added to the classpath when
    compiling the JDrupes Builder project.
    
  * "`extensionsSnapshotRepository = <url>`" sets the snapshot repository
    to use if one of the maven coordinates in `buildExtensions` is a
    snapshot.
    
  * "`javaHome = <directory>`" sets the directory that contains the JDK
    to use. Note that the environment variable `JAVA_HOME` takes
    precedence. 

## `jdbld` arguments

The `jdbld` command accepts the following options:

  * `-B-x <glob pattern>` excludes matching directories from the
    search for JDrupes Builder project source files. This option
    is only required for the development of the JDrupes Builder
    project itself. 
    
  * `-P <property name>=<property value>` sets the value of a
    property. The properties can be accessed from the build 
    project via the `BuildContext`'s method
    [property](javadoc/org/jdrupes/builder/api/BuildContext.html#property(java.lang.String,java.lang.String)).
    